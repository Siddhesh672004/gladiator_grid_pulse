package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.model.Feedback;

// what the ai does for feedback. never fails when the ai is down: NOT_ANALYSED / empty list
public interface FeedbackAiService {

    // sets sentiment + urgent on the feedback, the caller saves it
    void label(Feedback feedback);

    List<String> suggest(String feedbackText);

    List<String> summarise(List<String> feedbackLines);
}
