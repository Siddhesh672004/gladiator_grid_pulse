package com.gridpulse.core.feedback.service;

import java.util.List;

import com.gridpulse.common.paging.PageResponse;
import com.gridpulse.core.feedback.dto.FeedbackCountsResponse;
import com.gridpulse.core.feedback.dto.FeedbackRequest;
import com.gridpulse.core.feedback.dto.FeedbackResponse;
import com.gridpulse.core.feedback.dto.RecentFeedbackResponse;

// feedback of drivers, the ai part is in FeedbackAiService
public interface FeedbackService {

    // COMPLETED session, driver took part, once per session (409). saved NOT_ANALYSED, ai-service labels it
    // a moment later (FeedbackSubmitted -> FeedbackLabelled)
    FeedbackResponse createFeedback(FeedbackRequest request);

    // own feedback only, admins see everyone's
    FeedbackResponse getFeedbackById(Long id);

    // admin lists include the ai label
    List<FeedbackResponse> getAllFeedbacks();

    // admin list: newest first, search = driver, track or text. rating "1" - "5" or All
    PageResponse<FeedbackResponse> getFeedbackPage(String search, String sentiment, String rating, boolean urgentOnly,
            int page, int size);

    FeedbackCountsResponse getCounts();

    FeedbackResponse deleteFeedback(Long id);

    List<FeedbackResponse> getFeedbacksByUserId(int userId);

    List<FeedbackResponse> getFeedbacksBySessionId(Long sessionId);

    FeedbackResponse analyseFeedback(Long id);

    // ai-service's label (FeedbackLabelled). turning urgent -> UrgentFeedbackReported (the admins get an email)
    void applyLabel(Long feedbackId, String sentiment, boolean urgent);

    // newest first, for ai-service's admin summary
    List<RecentFeedbackResponse> getRecentFeedback(int limit);
}
