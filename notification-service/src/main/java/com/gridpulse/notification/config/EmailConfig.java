package com.gridpulse.notification.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.gridpulse.notification.config.properties.EmailProperties;
import com.gridpulse.notification.service.EmailService;
import com.gridpulse.notification.service.FileEmailService;
import com.gridpulse.notification.service.SmtpEmailService;

import lombok.extern.slf4j.Slf4j;

// every email goes through gmail (SmtpEmailService); at startup it logs in once and the result is in the log ("EMAIL:")
// only test runs use MAIL_TO_FILE=true: nothing is sent, the texts go to a build folder
@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailService emailService(JavaMailSender mailSender, MailProperties mail, EmailProperties email) {
        return email.toFile() ? fileEmailService(email) : gmailEmailService(mailSender, mail);
    }

    private EmailService fileEmailService(EmailProperties email) {
        log.warn("EMAIL: TEST MODE (MAIL_TO_FILE=true) - no email is sent, the test runs read them from {}",
                email.fileDir());
        return new FileEmailService(email);
    }

    private EmailService gmailEmailService(JavaMailSender mailSender, MailProperties mail) {
        String gmail = clean(mail.getUsername());
        // google shows the app password with spaces, they are not part of it
        String appPassword = clean(mail.getPassword()).replace(" ", "");
        // address + app password both there -> log in, else the log says what is missing
        Optional.of(gmail)
                .filter(address -> !address.isEmpty() && !appPassword.isEmpty())
                .ifPresentOrElse(address -> logIn(mailSender, address, appPassword), () -> log.error(
                        "EMAIL: MAIL_USERNAME / MAIL_PASSWORD are EMPTY in secrets.properties - no email can be sent. "
                                + "Fill both in and restart"));
        return new SmtpEmailService(mailSender, gmail);
    }

    private void logIn(JavaMailSender mailSender, String gmail, String appPassword) {
        // only the real gmail sender takes a login
        Optional.ofNullable(mailSender)
                .filter(JavaMailSenderImpl.class::isInstance)
                .map(JavaMailSenderImpl.class::cast)
                .ifPresent(gmailSender -> {
                    gmailSender.setUsername(gmail);
                    gmailSender.setPassword(appPassword);
                    checkGmailInBackground(gmailSender);
                });
    }

    // login test only (sends nothing), in its own thread so startup is not held up
    private void checkGmailInBackground(JavaMailSenderImpl gmailSender) {
        Thread check = new Thread(() -> {
            try {
                gmailSender.testConnection();
                log.info("EMAIL: Gmail login OK - emails are sent through Gmail");
            } catch (Exception e) {
                log.error("EMAIL: Gmail is NOT working - {}", SmtpEmailService.explain(e));
            }
        }, "gmail-check");
        check.setDaemon(true);
        check.start();
    }

    private String clean(String value) {
        // trimmed, a value written in "quotes" loses them, nothing set = empty
        return Optional.ofNullable(value)
                .map(String::trim)
                .map(text -> isInQuotes(text) ? text.substring(1, text.length() - 1).trim() : text)
                .orElse("");
    }

    private boolean isInQuotes(String text) {
        return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"");
    }
}
