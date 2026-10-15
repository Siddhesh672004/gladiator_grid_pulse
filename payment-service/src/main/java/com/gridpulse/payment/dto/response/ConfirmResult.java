package com.gridpulse.payment.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

// what confirm ends with: the core's booking json (201), or the core did not answer and the booking is
// still being confirmed (202 + message, RefundJob finishes it)
public record ConfirmResult(JsonNode booking, String message) {

    public static ConfirmResult booked(JsonNode booking) {
        return new ConfirmResult(booking, null);
    }

    public static ConfirmResult pending(String message) {
        return new ConfirmResult(null, message);
    }

    public boolean confirmed() {
        return booking != null;
    }
}
