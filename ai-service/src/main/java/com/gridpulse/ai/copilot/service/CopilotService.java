package com.gridpulse.ai.copilot.service;

import com.gridpulse.ai.copilot.dto.ActionResult;
import com.gridpulse.ai.copilot.dto.CopilotReply;
import com.gridpulse.ai.copilot.dto.CopilotRequest;

// the logged-in chat: reads with tools, changes only as Confirm cards (ActionExecutor runs them)
public interface CopilotService {

    CopilotReply ask(CopilotRequest request);

    ActionResult confirm(String token);

    void cancel(String token);

    void clearMemory(String chatId);
}
