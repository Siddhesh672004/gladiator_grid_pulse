package com.gridpulse.notification.email;

// every email GridPulse sends; the sending itself is service/EmailService (config/EmailConfig)
public sealed interface EmailMessage permits OtpEmail, AdminInviteEmail, PasswordResetEmail, BookingStatusEmail,
        RefundDoneEmail {

    String subject();

    String body();
}
