package com.gridpulse.ai.copilot.tools;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.gridpulse.ai.platform.exception.ServiceDownException;
import com.gridpulse.ai.platform.exception.ToolCallException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;

// a failed tool call becomes one sentence for the model (and for the Confirm results):
// the service's own 4xx message (or a tool's own 400 for a wrong argument), "not allowed" for 401 / 403,
// "unavailable" when a service did not answer
@Slf4j
@Component
public class ToolErrorProcessor implements ToolExecutionExceptionProcessor {

    public static final String NOT_ALLOWED = "You are not allowed to do this.";
    public static final String UNAVAILABLE = "That part of GridPulse is unavailable right now.";

    @Override
    public String process(ToolExecutionException e) {
        return messageFor(e);
    }

    // the first cause we know names the answer
    public static String messageFor(Throwable e) {
        return Stream.iterate(e, Objects::nonNull, Throwable::getCause)
                .map(ToolErrorProcessor::known)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Tool call failed: {}", e.getClass().getSimpleName());
                    return UNAVAILABLE;
                });
    }

    private static Optional<String> known(Throwable cause) {
        if (cause instanceof ToolCallException refused) {
            return Optional.of(refused.getStatus() == 401 || refused.getStatus() == 403 ? NOT_ALLOWED
                    : refused.getMessage());
        }
        if (cause instanceof AccessDeniedException) {
            return Optional.of(NOT_ALLOWED);
        }
        boolean down = cause instanceof ServiceDownException || cause instanceof CallNotPermittedException
                || cause instanceof ResourceAccessException || cause instanceof HttpServerErrorException;
        return down ? Optional.of(UNAVAILABLE) : Optional.empty();
    }
}
