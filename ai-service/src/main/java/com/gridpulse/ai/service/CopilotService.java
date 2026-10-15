package com.gridpulse.ai.service;

import com.gridpulse.ai.dto.request.CopilotRequest;
import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.dto.response.CopilotReply;

// the logged-in chat: reads with tools, changes only as Confirm cards (ActionExecutor runs them)
public interface CopilotService {

    CopilotReply ask(CopilotRequest request);

    ActionResult confirm(String token);

    void cancel(String token);

    void clearMemory(String chatId);
}
