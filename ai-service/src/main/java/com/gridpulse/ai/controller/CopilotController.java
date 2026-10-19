package com.gridpulse.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.ai.dto.request.CopilotRequest;
import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.dto.response.CopilotReply;
import com.gridpulse.ai.service.CopilotService;
import com.gridpulse.web.security.LoggedIn;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// the logged-in chat: ask, then Confirm / Cancel the cards. a card is the only way anything changes
@RestController
@RequestMapping("/api/ai/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;

    @LoggedIn
    @PostMapping
    public ResponseEntity<CopilotReply> ask(@Valid @RequestBody CopilotRequest request) {
        return ResponseEntity.ok(copilotService.ask(request));
    }

    // only the user who got the card, once, within 5 minutes (404 / 403 / 409 / 410 otherwise)
    @LoggedIn
    @PostMapping("/actions/{token}/confirm")
    public ResponseEntity<ActionResult> confirm(@PathVariable String token) {
        return ResponseEntity.ok(copilotService.confirm(token));
    }

    @LoggedIn
    @PostMapping("/actions/{token}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String token) {
        copilotService.cancel(token);
        return ResponseEntity.noContent().build();
    }

    @LoggedIn
    @DeleteMapping("/memory/{chatId}")
    public ResponseEntity<Void> clearMemory(@PathVariable String chatId) {
        copilotService.clearMemory(chatId);
        return ResponseEntity.noContent().build();
    }
}
