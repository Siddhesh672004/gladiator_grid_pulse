package com.gridpulse.ai.client;

// one feedback of the core's /internal/feedback/recent (newest first), for the admin's summary
public record RecentFeedback(Integer rating, String text) {
}
