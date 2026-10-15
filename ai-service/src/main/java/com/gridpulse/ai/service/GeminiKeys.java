package com.gridpulse.ai.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.gridpulse.ai.config.properties.GeminiProperties;

import lombok.extern.slf4j.Slf4j;

// the gemini keys and which one is in use right now. a key that gives no answer (daily limit reached,
// gemini busy ...) is left for the next one: 1 -> 2 -> 3 -> 1. used by GeminiServiceImpl and the Copilot
@Slf4j
@Component
public class GeminiKeys {

    // why a key gave no answer, the chat tells the driver when every key failed
    public enum Problem { INVALID, NO_REQUESTS, BUSY, NO_ANSWER }

    // google's 429 ends with "Please retry in 5h11m20s" or "Please retry in 44.2s"
    private static final Pattern RETRY_IN = Pattern.compile(
            "(?i)retry in (?:(\\d+)h)?(?:(\\d+)m(?!s))?(?:(\\d+(?:\\.\\d+)?)s)?");

    // the per-minute limit is back soon: that is "busy", not "used up for today"
    private static final Duration SHORT_WAIT = Duration.ofMinutes(2);

    private record Rest(Instant until, Problem problem) {
    }

    private final List<String> keys;
    private final AtomicInteger current = new AtomicInteger();
    private final Map<Integer, Rest> resting = new ConcurrentHashMap<>();

    public GeminiKeys(GeminiProperties gemini) {
        this.keys = gemini.keys();
    }

    public int count() {
        return keys.size();
    }

    // 0 = GEMINI_API_KEY, 1 = GEMINI_API_KEY_2, 2 = GEMINI_API_KEY_3
    public int current() {
        return current.get();
    }

    public String key(int position) {
        return keys.get(position);
    }

    // the key that answered is asked first next time
    public void use(int position) {
        current.set(position);
    }

    // google told this key to wait ("retry in ..."): skip it without a call. null = the key may be asked
    public Problem restingFor(int position) {
        // a rest that is over counts as no rest
        return Optional.ofNullable(resting.get(position))
                .filter(rest -> !Instant.now().isAfter(rest.until()))
                .map(Rest::problem)
                .orElse(null);
    }

    // status = gemini's http status, 0 = no answer at all (timeout, network, unreadable answer)
    public Problem useNext(int failed, int status, String reason) {
        String text = Optional.ofNullable(reason).orElse("");
        Duration wait = retryIn(text);
        Problem problem = problemOf(status, text, wait);
        // google said when the key works again: skip it until then
        Optional.ofNullable(wait)
                .ifPresent(pause -> resting.put(failed, new Rest(Instant.now().plus(pause), problem)));
        // two requests can fail at the same moment: compareAndSet lets only the first one move on
        current.compareAndSet(failed, (failed + 1) % keys.size());
        log.warn("Gemini key {} of {} gave no answer ({}), trying the next key. Reason: {}",
                failed + 1, keys.size(), problem, text);
        return problem;
    }

    private Problem problemOf(int status, String text, Duration wait) {
        boolean refused = status == 401 || status == 403 || (status == 400 && text.contains("API key"));
        boolean limitReached = status == 429 || text.contains("RESOURCE_EXHAUSTED");
        boolean backSoon = wait != null && wait.compareTo(SHORT_WAIT) < 0;
        // the first case that fits names the problem, nothing fits = no answer at all
        return Stream.of(Map.entry(refused, Problem.INVALID),
                        Map.entry(limitReached, backSoon ? Problem.BUSY : Problem.NO_REQUESTS),
                        Map.entry(status >= 500, Problem.BUSY))
                .filter(Map.Entry::getKey)
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Problem.NO_ANSWER);
    }

    // null = google did not say when the key works again
    private Duration retryIn(String text) {
        // hours, minutes and seconds of "retry in 5h11m20s", a wait of 0 counts as none
        return Optional.of(RETRY_IN.matcher(text))
                .filter(Matcher::find)
                .map(match -> number(match.group(1)) * 3600 + number(match.group(2)) * 60 + number(match.group(3)))
                .filter(seconds -> seconds > 0)
                .map(seconds -> Duration.ofMillis((long) (seconds * 1000)))
                .orElse(null);
    }

    // a part google left out is 0
    private double number(String digits) {
        return Optional.ofNullable(digits)
                .map(Double::parseDouble)
                .orElse(0.0);
    }
}
