package com.gridpulse.ai.client;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

// the core's api, called with the caller's own token (config/ClientConfig); a 4xx -> ToolCallException
@HttpExchange
public interface CoreClient {

    // admin token + internal key
    @GetExchange("/internal/feedback/recent")
    List<RecentFeedback> recentFeedback(@RequestParam int limit);
}
