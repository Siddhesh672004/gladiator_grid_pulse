package com.examly.springapp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// gridpulse.ticket-secret (TICKET_SECRET): signs the e-ticket codes, empty only on a dev laptop
@ConfigurationProperties(prefix = "gridpulse")
public record TicketProperties(String ticketSecret) {

    // the secret never goes into a log line
    @Override
    public String toString() {
        return "TicketProperties[ticketSecret=***]";
    }
}
