package com.gridpulse.core.feedback.mapper;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.mapper.UserMapper;
import com.gridpulse.core.feedback.data.Feedback;
import com.gridpulse.core.feedback.data.Sentiment;
import com.gridpulse.core.feedback.dto.FeedbackRequest;
import com.gridpulse.core.feedback.dto.FeedbackResponse;
import com.gridpulse.core.session.data.RaceSession;
import com.gridpulse.core.session.mapper.SessionMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedbackMapper {

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;

    public Feedback toEntity(FeedbackRequest request, User author, RaceSession session) {
        LocalDate date = Objects.requireNonNullElse(request.date(), LocalDate.now());
        return new Feedback(request.feedbackText().trim(), request.rating(), date, author, session);
    }

    // a driver gets no ai label (null, so it is left out of the json)
    public FeedbackResponse toResponse(Feedback feedback) {
        return build(feedback, null, null);
    }

    // an admin gets the label, a row from before the ai counts as NOT_ANALYSED
    public FeedbackResponse toAdminResponse(Feedback feedback) {
        Sentiment sentiment = Objects.requireNonNullElse(feedback.getSentiment(), Sentiment.NOT_ANALYSED);
        return build(feedback, sentiment, feedback.isUrgent());
    }

    private FeedbackResponse build(Feedback feedback, Sentiment sentiment, Boolean urgent) {
        return new FeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getFeedbackText(),
                feedback.getRating(),
                feedback.getDate(),
                feedback.getUser().getUserId(),
                userMapper.toResponse(feedback.getUser()),
                sessionMapper.toResponse(feedback.getSession()),
                sentiment,
                urgent);
    }
}
