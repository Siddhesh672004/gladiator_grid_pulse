package com.gridpulse.core.aspect;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gridpulse.web.error.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

// one short log line per failed api call: CLIENT_ERROR (4xx, WARN) or SERVER_ERROR (5xx, ERROR)
// never logs passwords, codes, tokens or request bodies
@Slf4j
@Aspect
@Component
public class GlobalLoggingAspect {

    private static final String OUR_PACKAGE = "com.gridpulse.core";
    private static final int CAUSE_MAX = 300;

    // runs after GlobalExceptionHandler picked the answer (also bad json / @Valid errors)
    @AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ExceptionHandler)",
            returning = "response")
    public void logError(JoinPoint joinPoint, ResponseEntity<?> response) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Throwable error = findError(joinPoint.getArgs());
        int status = response.getStatusCode().value();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 5xx goes out as ERROR with the real cause, the rest as WARN with the client's message
        Optional.of(status)
                .filter(code -> code >= 500)
                .ifPresentOrElse(
                        code -> log.error("SERVER_ERROR {} {} {} \"{}\" at {} user={}", code, method, uri,
                                cause(error), thrownAt(error), currentUser()),
                        () -> log.warn("CLIENT_ERROR {} {} {} \"{}\" at {} user={}", status, method, uri,
                                clientMessage(response, error), thrownAt(error), currentUser()));
    }

    private Throwable findError(Object[] args) {
        // the handler gets the error as one of its arguments
        return Arrays.stream(args)
                .filter(Throwable.class::isInstance)
                .map(Throwable.class::cast)
                .findFirst()
                .orElse(null);
    }

    // the same message the client got
    private String clientMessage(ResponseEntity<?> response, Throwable error) {
        // an answer without our error body only has the name of the error
        return Optional.ofNullable(response.getBody())
                .filter(ErrorResponse.class::isInstance)
                .map(ErrorResponse.class::cast)
                .map(body -> oneLine(body.getMessage()))
                .orElseGet(() -> errorName(error));
    }

    private String errorName(Throwable error) {
        // no error to name = a dash
        return Optional.ofNullable(error)
                .map(thrown -> thrown.getClass().getSimpleName())
                .orElse("-");
    }

    // a 5xx client only gets a friendly text, the log line says what really broke
    private String cause(Throwable error) {
        // a long cause is cut, the log line stays short
        return Optional.ofNullable(error)
                .map(thrown -> thrown.getClass().getSimpleName() + ": " + oneLine(thrown.getMessage()))
                .map(text -> text.substring(0, Math.min(text.length(), CAUSE_MAX)))
                .orElse("-");
    }

    // first line of our own code in the stack trace ("-" when the error came from spring itself)
    private String thrownAt(Throwable error) {
        // the stack starts at the newest call, so the first line of ours is where it broke
        return Stream.ofNullable(error)
                .flatMap(thrown -> Arrays.stream(thrown.getStackTrace()))
                .filter(line -> line.getClassName().startsWith(OUR_PACKAGE) && !passesOn(line))
                .map(line -> line.getFileName() + ":" + line.getLineNumber())
                .findFirst()
                .orElse("-");
    }

    // proxies, the jwt filter and this aspect only pass the call on, never the cause
    private boolean passesOn(StackTraceElement line) {
        String name = line.getClassName();
        return name.contains("$$") || name.equals(GlobalLoggingAspect.class.getName())
                || line.getMethodName().equals("doFilterInternal");
    }

    private String currentUser() {
        // nobody logged in (or only the anonymous visitor) = a dash
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !"anonymousUser".equals(auth.getName()))
                .map(Authentication::getName)
                .orElse("-");
    }

    // one error = one log line
    private String oneLine(String text) {
        // an error without a message = a dash
        return Optional.ofNullable(text)
                .map(message -> message.replace('\r', ' ').replace('\n', ' '))
                .orElse("-");
    }
}
