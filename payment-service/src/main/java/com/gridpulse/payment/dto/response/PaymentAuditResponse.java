package com.gridpulse.payment.dto.response;

import java.time.Instant;

public record PaymentAuditResponse(String event, String detail, Instant createdAt) {
}
