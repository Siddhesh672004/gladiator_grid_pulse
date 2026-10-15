package com.gridpulse.notification.service;

import java.util.List;

import com.gridpulse.notification.email.Attachment;
import com.gridpulse.notification.email.EmailMessage;

// SmtpEmailService sends through gmail, FileEmailService is only for test runs (MAIL_TO_FILE=true)
public interface EmailService {

    void send(String to, EmailMessage message, List<Attachment> attachments);
}
