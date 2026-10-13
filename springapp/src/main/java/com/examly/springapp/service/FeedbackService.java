package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.request.FeedbackRequest;
import com.examly.springapp.dto.request.SuggestionRequest;
import com.examly.springapp.dto.response.AiLinesResponse;
import com.examly.springapp.dto.response.FeedbackResponse;

// feedback of drivers, the ai part is in FeedbackAiService
public interface FeedbackService {

    // COMPLETED session, driver took part, once per session (409). the ai label is set here too
    FeedbackResponse createFeedback(FeedbackRequest request);

    // own feedback only, admins see everyone's
    FeedbackResponse getFeedbackById(Long id);

    // admin lists include the ai label
    List<FeedbackResponse> getAllFeedbacks();

    FeedbackResponse deleteFeedback(Long id);

    List<FeedbackResponse> getFeedbacksByUserId(int userId);

    List<FeedbackResponse> getFeedbacksBySessionId(Long sessionId);

    FeedbackResponse analyseFeedback(Long id);

    // nothing is saved
    AiLinesResponse suggest(SuggestionRequest request);

    AiLinesResponse summarise();
}
