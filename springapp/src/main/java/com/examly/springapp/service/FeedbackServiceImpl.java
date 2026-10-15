package com.examly.springapp.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.examly.springapp.dto.request.FeedbackRequest;
import com.examly.springapp.dto.request.SuggestionRequest;
import com.examly.springapp.dto.response.AiLinesResponse;
import com.examly.springapp.dto.response.FeedbackCountsResponse;
import com.examly.springapp.dto.response.FeedbackResponse;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.FeedbackMapper;
import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.Sentiment;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.FeedbackRepo;
import com.examly.springapp.repository.RaceBookingRepo;
import com.examly.springapp.repository.RaceSessionRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// feedback is about one session: the driver drove in it and it is over
// the ai never stops a feedback from being saved
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepo feedbackRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingRepo raceBookingRepo;
    private final UserService userService;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackAiService feedbackAiService;

    @Override
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        User author = userService.getLoggedInUser();
        checkDate(request.date());
        RaceSession session = raceSessionRepo.findById(request.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session with ID: " + request.sessionId()
                        + " not found."));

        checkSessionCompleted(session);
        checkTookPart(session, author);
        checkFirstFeedback(session, author);

        Feedback feedback = feedbackMapper.toEntity(request, author, session);
        // ai down -> NOT_ANALYSED, saved anyway
        feedbackAiService.label(feedback);
        Feedback saved = feedbackRepo.save(feedback);
        log.info("Feedback {} added by user {} for session {}", saved.getFeedbackId(), author.getUserId(),
                session.getSessionId());
        return feedbackMapper.toResponse(saved);
    }

    @Override
    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = findFeedback(id);
        userService.checkOwnerOrAdmin(feedback.getUser().getUserId());
        return feedbackMapper.toResponse(feedback);
    }

    @Override
    public List<FeedbackResponse> getAllFeedbacks() {
        return toAdminResponses(feedbackRepo.findAll());
    }

    @Override
    public PageResponse<FeedbackResponse> getFeedbackPage(String search, String sentiment, String rating,
            boolean urgentOnly, int page, int size) {
        Sentiment wanted = Paging.enumFilter(Sentiment.class, sentiment, "Sentiment");
        Integer stars = Optional.ofNullable(Paging.filter(rating))
                .map(this::starsFilter)
                .orElse(null);
        // NOT_ANALYSED also finds the old rows that never got a label
        return Paging.toResponse(feedbackRepo.searchPage(wanted, wanted == Sentiment.NOT_ANALYSED, stars, urgentOnly,
                Paging.likePattern(search), Paging.pageable(page, size, Sort.by(Sort.Direction.DESC, "feedbackId"))),
                feedbackMapper::toAdminResponse);
    }

    @Override
    public FeedbackCountsResponse getCounts() {
        // every label starts at 0, a row without a label counts as NOT_ANALYSED
        Map<Sentiment, Long> counts = new EnumMap<>(Sentiment.class);
        Arrays.stream(Sentiment.values()).forEach(label -> counts.put(label, 0L));
        feedbackRepo.countPerSentiment().forEach(row -> counts.merge(
                Objects.requireNonNullElse(row.getSentiment(), Sentiment.NOT_ANALYSED), row.getAmount(), Long::sum));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new FeedbackCountsResponse(total, counts, feedbackRepo.countByUrgentTrue());
    }

    private Integer starsFilter(String rating) {
        return Optional.of(rating)
                .filter(text -> text.matches("[1-5]"))
                .map(Integer::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("Rating must be 1 to 5 or All"));
    }

    @Override
    public FeedbackResponse deleteFeedback(Long id) {
        Feedback feedback = findFeedback(id);
        userService.checkOwnerOrAdmin(feedback.getUser().getUserId());
        feedbackRepo.delete(feedback);
        log.info("Feedback {} deleted", id);
        return feedbackMapper.toResponse(feedback);
    }

    @Override
    public List<FeedbackResponse> getFeedbacksByUserId(int userId) {
        userService.checkOwnerOrAdmin(userId);
        return toResponses(feedbackRepo.findByUserUserId(userId));
    }

    @Override
    public List<FeedbackResponse> getFeedbacksBySessionId(Long sessionId) {
        return toAdminResponses(feedbackRepo.findBySessionSessionId(sessionId));
    }

    @Override
    public FeedbackResponse analyseFeedback(Long id) {
        Feedback feedback = findFeedback(id);
        feedbackAiService.label(feedback);
        Feedback saved = feedbackRepo.save(feedback);
        log.info("Feedback {} analysed again -> {}", id, saved.getSentiment());
        return feedbackMapper.toAdminResponse(saved);
    }

    @Override
    public AiLinesResponse suggest(SuggestionRequest request) {
        return new AiLinesResponse(feedbackAiService.suggest(request.feedbackText().trim()));
    }

    @Override
    public AiLinesResponse summarise() {
        List<String> lines = feedbackRepo.findTop50ByOrderByFeedbackIdDesc().stream()
                .map(this::summaryLine)
                .toList();
        // no feedback yet -> no ai call, an empty summary
        return new AiLinesResponse(lines.isEmpty() ? List.of() : feedbackAiService.summarise(lines));
    }

    private String summaryLine(Feedback feedback) {
        String text = feedback.getFeedbackText().replace("\r", " ").replace("\n", " ");
        String cut = text.substring(0, Math.min(text.length(), 200));
        // the stars first, a feedback without stars says so
        return Optional.ofNullable(feedback.getRating())
                .map(rating -> rating + " stars: " + cut)
                .orElse("no rating: " + cut);
    }

    // "today" is India's date, also on a server that runs in UTC. no date = today (FeedbackMapper)
    private void checkDate(LocalDate date) {
        // a day after today is refused
        Optional.ofNullable(date)
                .filter(day -> day.isAfter(RaceSession.today()))
                .ifPresent(day -> {
                    throw new IllegalArgumentException("Date cannot be in the future");
                });
    }

    private void checkSessionCompleted(RaceSession session) {
        // only a finished session gets feedback
        Optional.of(session)
                .filter(over -> over.getStatus() == SessionStatus.COMPLETED)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You can give feedback once the session is completed"));
    }

    private void checkTookPart(RaceSession session, User author) {
        // the driver needs an approved booking in this session
        Optional.of(session)
                .filter(drove -> raceBookingRepo.existsBySessionSessionIdAndUserUserIdAndStatus(drove.getSessionId(),
                        author.getUserId(), BookingStatus.APPROVED))
                .orElseThrow(() -> new IllegalArgumentException(
                        "You can only give feedback on a session you took part in"));
    }

    private void checkFirstFeedback(RaceSession session, User author) {
        // one feedback per driver per session
        Optional.of(session)
                .filter(given -> !feedbackRepo.existsBySessionSessionIdAndUserUserId(given.getSessionId(),
                        author.getUserId()))
                .orElseThrow(() -> new ConflictException("You have already given feedback for this session"));
    }

    private Feedback findFeedback(Long id) {
        return feedbackRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback with ID: " + id + " not found."));
    }

    // driver lists have no ai label
    private List<FeedbackResponse> toResponses(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .map(feedbackMapper::toResponse)
                .toList();
    }

    private List<FeedbackResponse> toAdminResponses(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .map(feedbackMapper::toAdminResponse)
                .toList();
    }
}
