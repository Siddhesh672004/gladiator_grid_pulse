package com.examly.springapp.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.examly.springapp.config.properties.EmailProperties;
import com.examly.springapp.email.EmailMessage;
import com.examly.springapp.exceptions.EmailSendException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// test runs only (MAIL_TO_FILE=true): each email becomes a text file in target/test-mails, nothing goes to gmail
// not a @Service, config/EmailConfig creates it in that test mode
@Slf4j
@RequiredArgsConstructor
public class FileEmailService implements EmailService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final EmailProperties emailProperties;

    @Override
    public void send(String to, EmailMessage message) {
        Path file = Path.of(emailProperties.fileDir(), LocalDateTime.now().format(FILE_TIME) + "_" + to + ".txt");
        String text = "To: " + to + "\nSubject: " + message.subject() + "\n\n" + message.body();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            // subject only, the body holds a code
            log.info("TEST MODE: email '{}' for {} written to {}", message.subject(), to, file);
        } catch (IOException e) {
            throw new EmailSendException("Could not send the email, please try again", e);
        }
    }
}
