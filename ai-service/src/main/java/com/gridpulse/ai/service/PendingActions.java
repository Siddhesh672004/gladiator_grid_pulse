package com.gridpulse.ai.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridpulse.ai.entity.ActionLog;
import com.gridpulse.ai.entity.PendingAction;
import com.gridpulse.ai.exceptions.ActionRefusedException;
import com.gridpulse.ai.repository.ActionLogRepo;
import com.gridpulse.ai.repository.PendingActionRepo;
import com.gridpulse.ai.security.TokenUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the Copilot's prepared changes: saved with their owner, shown as Confirm cards, used at most once
// the tokens prepared while the current request is answered are collected for its reply (same thread)
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingActions {

    private static final Duration LIFETIME = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    // what a pending action looks like when Confirm is pressed: claimed, or found expired just now
    public record Claim(PendingAction action, boolean expired) {
    }

    private final PendingActionRepo pendingActionRepo;
    private final ActionLogRepo actionLogRepo;
    private final ObjectMapper objectMapper;

    private final ThreadLocal<List<PendingAction>> prepared = ThreadLocal.withInitial(ArrayList::new);

    // called by a prepare* tool: the owner comes from the caller's token, never from the model
    public String prepare(String toolName, Object args, String summary) {
        TokenUser user = TokenUser.current();
        Instant now = Instant.now();
        PendingAction action = pendingActionRepo.save(new PendingAction(newToken(), user.userId(), user.role(),
                toolName, toJson(args), summary, now, now.plus(LIFETIME)));
        prepared.get().add(action);
        log.info("Action {} prepared for user {}", toolName, user.userId());
        return "Prepared. The user must press Confirm: " + summary;
    }

    // the cards of this request; the list is emptied
    public List<PendingAction> takePrepared() {
        List<PendingAction> taken = List.copyOf(prepared.get());
        prepared.remove();
        return taken;
    }

    // the request is over: nothing of it stays on this pooled thread
    public void forget() {
        prepared.remove();
    }

    // a gemini try that failed half way: its actions were never shown, they go
    public void discardPrepared() {
        pendingActionRepo.deleteAll(prepared.get());
        prepared.remove();
    }

    public <T> T args(String argsJson, Class<T> type) {
        try {
            return objectMapper.readValue(argsJson, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Saved action arguments are not readable", e);
        }
    }

    // locked: a double click waits here, then finds it used. only its owner, with the same role, may use it
    @Transactional
    public Claim claim(String token, TokenUser user) {
        PendingAction action = owned(token, user);
        if (action.getExpiresAt().isBefore(Instant.now())) {
            action.setUsedAt(Instant.now());
            actionLogRepo.save(new ActionLog(action, "EXPIRED", "Confirmed too late"));
            return new Claim(action, true);
        }
        action.setUsedAt(Instant.now());
        return new Claim(action, false);
    }

    @Transactional
    public void cancel(String token, TokenUser user) {
        PendingAction action = owned(token, user);
        action.setUsedAt(Instant.now());
        actionLogRepo.save(new ActionLog(action, "CANCELLED", "Cancelled by the user"));
    }

    public void record(PendingAction action, String outcome, String message) {
        actionLogRepo.save(new ActionLog(action, outcome, message));
    }

    private PendingAction owned(String token, TokenUser user) {
        PendingAction action = pendingActionRepo.findWithLockByToken(token)
                .orElseThrow(() -> new ActionRefusedException(404, "This action does not exist"));
        if (action.getUserId() != user.userId() || !Objects.equals(action.getRole(), user.role())) {
            throw new AccessDeniedException("This action belongs to another user");
        }
        if (action.getUsedAt() != null) {
            throw new ActionRefusedException(409, "This action was already confirmed or cancelled");
        }
        return action;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String toJson(Object args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Action arguments could not be saved", e);
        }
    }
}
