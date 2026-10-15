package com.gridpulse.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

// service-to-service calls (/internal/**) carry the shared INTERNAL_API_KEY in the X-Internal-Key header
public final class InternalApi {

    public static final String HEADER = "X-Internal-Key";
    // a laptop without INTERNAL_API_KEY: every service falls back to this one (prod refuses to start without a key)
    public static final String DEV_KEY = "gridpulse-dev-internal-key-not-for-a-real-server";

    private InternalApi() {
    }

    public static String keyOrDevDefault(String configured) {
        return Optional.ofNullable(configured)
                .filter(key -> !key.isBlank())
                .orElse(DEV_KEY);
    }

    // constant time: the answer time tells nothing about how much of the key was right
    public static boolean matches(String expected, String given) {
        return given != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                given.getBytes(StandardCharsets.UTF_8));
    }
}
