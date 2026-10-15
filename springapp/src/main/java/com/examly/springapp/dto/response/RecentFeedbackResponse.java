package com.examly.springapp.dto.response;

// one feedback for ai-service's admin summary (/internal/feedback/recent): only the stars and the text
public record RecentFeedbackResponse(Integer rating, String text) {
}
