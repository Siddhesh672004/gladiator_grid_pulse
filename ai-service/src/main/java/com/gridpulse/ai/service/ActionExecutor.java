package com.gridpulse.ai.service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.exceptions.ActionRefusedException;
import com.gridpulse.ai.security.TokenUser;
import com.gridpulse.ai.tools.ConfirmableTools;
import com.gridpulse.ai.tools.ToolErrorProcessor;

import lombok.extern.slf4j.Slf4j;

// Confirm: the action is claimed (and committed) first, so it runs at most once; then it runs with the caller's
// current token (the services check the user again) and the outcome goes into ai_action_log
@Slf4j
@Service
public class ActionExecutor {

    private final PendingActions pendingActions;
    private final Map<String, Function<String, ActionResult>> handlers;

    public ActionExecutor(PendingActions pendingActions, ObjectProvider<ConfirmableTools> tools) {
        this.pendingActions = pendingActions;
        this.handlers = tools.orderedStream()
                .flatMap(set -> set.handlers().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public ActionResult confirm(String token) {
        PendingActions.Claim claim = pendingActions.claim(token, TokenUser.current());
        if (claim.expired()) {
            throw new ActionRefusedException(410, "This action expired - ask the Copilot again");
        }
        String toolName = claim.action().getToolName();
        ActionResult result = Optional.ofNullable(handlers.get(toolName))
                .map(handler -> run(handler, claim.action().getArgsJson()))
                .orElseGet(() -> ActionResult.failed("This action is not available any more"));
        pendingActions.record(claim.action(), result.ok() ? "OK" : "ERROR", result.message());
        log.info("Action {} confirmed by user {}: {}", toolName, claim.action().getUserId(),
                result.ok() ? "OK" : "ERROR");
        return result;
    }

    public void cancel(String token) {
        pendingActions.cancel(token, TokenUser.current());
    }

    // a refusal of the core / payment-service is an answer for the user, not an error of ours
    private ActionResult run(Function<String, ActionResult> handler, String argsJson) {
        try {
            return handler.apply(argsJson);
        } catch (RuntimeException e) {
            return ActionResult.failed(ToolErrorProcessor.messageFor(e));
        }
    }
}
