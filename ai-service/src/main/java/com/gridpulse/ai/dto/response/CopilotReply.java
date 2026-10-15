package com.gridpulse.ai.dto.response;

import java.util.List;

// the Copilot's answer; actions = the changes it prepared, each waits for its Confirm
public record CopilotReply(String reply, String chatId, List<ActionCard> actions) {
}
