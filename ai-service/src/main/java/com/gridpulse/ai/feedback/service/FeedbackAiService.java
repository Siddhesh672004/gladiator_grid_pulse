package com.gridpulse.ai.feedback.service;

import java.util.List;

import com.gridpulse.ai.feedback.dto.FeedbackLabel;

// what the ai does for feedback. never fails when the ai is down: NOT_ANALYSED / empty list
public interface FeedbackAiService {

    FeedbackLabel label(String feedbackText);

    List<String> suggest(String feedbackText);

    // the admin's summary of the newest feedback (read from the core)
    List<String> summariseRecent();
}
