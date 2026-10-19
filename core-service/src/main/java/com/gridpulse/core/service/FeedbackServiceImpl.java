package com.gridpulse.core.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gridpulse.common.event.FeedbackSubmitted;
import com.gridpulse.common.event.UrgentFeedbackReported;
import com.gridpulse.common.paging.PageResponse;
import com.gridpulse.common.paging.Paging;
import com.gridpulse.core.dto.request.FeedbackRequest;
import com.gridpulse.core.dto.response.FeedbackCountsResponse;
import com.gridpulse.core.dto.response.FeedbackLabelResponse;
import com.gridpulse.core.dto.response.FeedbackResponse;
import com.gridpulse.core.dto.response.RecentFeedbackResponse;
import com.gridpulse.core.exceptions.ConflictException;
import com.gridpulse.core.exceptions.ResourceNotFoundException;
import com.gridpulse.core.mapper.FeedbackMapper;
import com.gridpulse.core.model.BookingStatus;
import com.gridpulse.core.model.Feedback;
import com.gridpulse.core.model.RaceSession;
import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.Sentiment;
import com.gridpulse.core.model.SessionStatus;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.FeedbackRepo;
import com.gridpulse.core.repository.RaceBookingRepo;
import com.gridpulse.core.repository.RaceSessionRepo;
import com.gridpulse.core.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// feedback is about one session: the driver drove in it and it is over
// the ai never stops a feedback from being saved: it is labelled afterwards by ai-service (kafka)
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepo feedbackRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingRepo raceBookingRepo;
    private final UserService userService;
    private final FeedbackMapper feedbackMapper;
    private final UserRepo userRepo;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    private static final int EXCERPT_LENGTH = 300;
    private static final int MAX_RECENT = 50;

    @Override
    @Transactional
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
        feedback.setSentiment(Sentiment.NOT_ANALYSED);
        Feedback saved = feedbackRepo.save(feedback);
        // outbox with this transaction: ai-service labels it a moment later
        eventPublisher.publishEvent(new FeedbackSubmitted(saved.getFeedbackId(), saved.getFeedbackText()));
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

    // the admin asks again at once (ai-service down -> NOT_ANALYSED, the row stays as it was)
    @Override
    @Transactional
    public FeedbackResponse analyseFeedback(Long id) {
        Feedback feedback = findFeedback(id);
        FeedbackLabelResponse label = aiClient.label(feedback.getFeedbackText());
        Optional.of(label)
                .filter(answer -> !Sentiment.NOT_ANALYSED.name().equals(answer.sentiment()))
                .ifPresent(answer -> label(feedback, answer.sentiment(), answer.urgent()));
        log.info("Feedback {} analysed again -> {}", id, feedback.getSentiment());
        return feedbackMapper.toAdminResponse(feedbackRepo.save(feedback));
    }

    // a feedback deleted in the meantime: nothing to label
    @Override
    @Transactional
    public void applyLabel(Long feedbackId, String sentiment, boolean urgent) {
        feedbackRepo.findById(feedbackId)
                .ifPresentOrElse(feedback -> label(feedback, sentiment, urgent),
                        () -> log.debug("Feedback {} is gone, its label is skipped", feedbackId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentFeedbackResponse> getRecentFeedback(int limit) {
        return feedbackRepo.findTop50ByOrderByFeedbackIdDesc().stream()
                .limit(Math.max(1, Math.min(limit, MAX_RECENT)))
                .map(feedback -> new RecentFeedbackResponse(feedback.getRating(), feedback.getFeedbackText()))
                .toList();
    }

    // an unknown word counts as NOT_ANALYSED; a feedback that turns urgent tells every admin once
    private void label(Feedback feedback, String sentiment, boolean urgent) {
        boolean wasUrgent = feedback.isUrgent();
        feedback.setSentiment(EnumSet.allOf(Sentiment.class).stream()
                .filter(known -> known.name().equals(sentiment))
                .findFirst()
                .orElse(Sentiment.NOT_ANALYSED));
        feedback.setUrgent(urgent);
        feedbackRepo.save(feedback);
        Optional.of(feedback)
                .filter(labelled -> urgent && !wasUrgent)
                .ifPresent(this::reportUrgent);
    }

    private void reportUrgent(Feedback feedback) {
        String text = feedback.getFeedbackText();
        List<String> adminEmails = userRepo.findByUserRoleInAndEnabledTrue(List.of(Role.ADMIN, Role.SUPER_ADMIN))
                .stream()
                .map(User::getEmail)
                .toList();
        eventPublisher.publishEvent(new UrgentFeedbackReported(feedback.getFeedbackId(),
                feedback.getUser().getUsername(), feedback.getSession().getTrack().getTrackName(),
                feedback.getSession().getSessionDate(), feedback.getRating(),
                text.substring(0, Math.min(text.length(), EXCERPT_LENGTH)), adminEmails));
        log.warn("Feedback {} reports a safety problem, {} admin(s) are emailed", feedback.getFeedbackId(),
                adminEmails.size());
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
