package com.gridpulse.common.ticket;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// e-ticket code "GP-{bookingId}-{16 hex}": the hex is an HMAC of the id, so a code can't be made up or changed
public final class TicketCodes {

    private static final Pattern CODE = Pattern.compile("GP-(\\d{1,18})-([0-9a-f]{16})");

    private TicketCodes() {
    }

    public static String sign(long bookingId, String secret) {
        return "GP-" + bookingId + "-" + mac(bookingId, secret);
    }

    // a code we made -> its booking id, anything else -> empty
    public static Optional<Long> verify(String code, String secret) {
        return Optional.ofNullable(code)
                .map(String::trim)
                .map(CODE::matcher)
                .filter(Matcher::matches)
                .filter(parts -> MessageDigest.isEqual(
                        mac(Long.parseLong(parts.group(1)), secret).getBytes(StandardCharsets.UTF_8),
                        parts.group(2).getBytes(StandardCharsets.UTF_8)))
                .map(parts -> Long.parseLong(parts.group(1)));
    }

    // HMAC-SHA256 of the id, the first 16 hex characters
    private static String mac(long bookingId, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(String.valueOf(bookingId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
