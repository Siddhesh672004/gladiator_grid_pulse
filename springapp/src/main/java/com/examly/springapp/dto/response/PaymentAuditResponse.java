package com.examly.springapp.dto.response;

import java.time.Instant;

public record PaymentAuditResponse(String event, String detail, Instant createdAt) {
}
