package com.gridpulse.notification.email;

import java.text.DecimalFormat;
import java.time.LocalDate;

// sent when payment-service finished a refund (PaymentStatusChanged REFUNDED)
public record RefundDoneEmail(String username, Double amount, String trackName, LocalDate sessionDate)
        implements EmailMessage {

    @Override
    public String subject() {
        return "Your GridPulse refund is done";
    }

    @Override
    public String body() {
        return """
                Hi %s,

                Rs.%s has been refunded for your booking on %s, %s.
                It goes back the way you paid; your bank may take a few days to show it.

                - GridPulse
                """.formatted(username, new DecimalFormat("0.##").format(amount), trackName, sessionDate);
    }
}
