package com.gridpulse.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.payment-url (PAYMENT_URL): where payment-service runs, for service/PaymentClient
@ConfigurationProperties(prefix = "gridpulse")
public record PaymentServiceProperties(String paymentUrl) {
}
