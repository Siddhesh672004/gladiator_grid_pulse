package com.gridpulse.notification.listener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.gridpulse.common.event.AdminInvited;
import com.gridpulse.common.event.BookingDecided;
import com.gridpulse.common.event.BookingPromoted;
import com.gridpulse.common.event.BookingWaitlisted;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.OtpRequested;
import com.gridpulse.common.event.PaymentStatusChanged;
import com.gridpulse.common.event.Topics;
import com.gridpulse.common.inbox.Inbox;
import com.gridpulse.common.ticket.TicketData;
import com.gridpulse.common.ticket.TicketPdf;
import com.gridpulse.notification.email.AdminInviteEmail;
import com.gridpulse.notification.email.Attachment;
import com.gridpulse.notification.email.BookingStatusEmail;
import com.gridpulse.notification.email.EmailMessage;
import com.gridpulse.notification.email.OtpEmail;
import com.gridpulse.notification.email.PasswordResetEmail;
import com.gridpulse.notification.email.PromotedEmail;
import com.gridpulse.notification.email.RefundDoneEmail;
import com.gridpulse.notification.email.WaitlistEmail;
import com.gridpulse.notification.ics.IcsBuilder;
import com.gridpulse.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// account, booking and payment events -> emails. kafka may deliver an event twice: the inbox makes it one email
// a failed email (gmail down) is tried again after 2 s, 6 s and 18 s, then the event lands on the dead letter topic
// the retry topics carry this service's name, so another service's retries never mix with ours
// whoever starts first makes the topics: 3 partitions here too, the same as the core's (KafkaTopicConfig)
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    static final String CONSUMER = "notification-service";

    private final EventReader eventReader;
    private final Inbox inbox;
    private final EmailService emailService;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-notification-retry", dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = Topics.ACCOUNT, groupId = CONSUMER)
    public void onAccountEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        switch (event.type()) {
            case "OtpRequested" -> once(event, () -> sendCode(eventReader.payload(event, OtpRequested.class)));
            case "AdminInvited" -> once(event, () -> sendInvite(eventReader.payload(event, AdminInvited.class)));
            default -> log.debug("Event type {} is not for notification-service, skipped", event.type());
        }
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-notification-retry", dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = Topics.BOOKING, groupId = CONSUMER)
    public void onBookingEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        switch (event.type()) {
            case "BookingDecided" -> once(event, () -> sendDecision(eventReader.payload(event, BookingDecided.class)));
            case "BookingWaitlisted" -> once(event, () -> sendWaitlisted(eventReader.payload(event, BookingWaitlisted.class)));
            case "BookingPromoted" -> once(event, () -> sendPromoted(eventReader.payload(event, BookingPromoted.class)));
            default -> log.debug("Event type {} is not for notification-service, skipped", event.type());
        }
    }

    // only a finished refund is emailed, the other payment statuses are for the core
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 3), numPartitions = "3",
            retryTopicSuffix = "-notification-retry", dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = Topics.PAYMENT, groupId = CONSUMER)
    public void onPaymentEvent(String message) {
        EventEnvelope event = eventReader.read(message);
        switch (event.type()) {
            case "PaymentStatusChanged" -> {
                PaymentStatusChanged change = eventReader.payload(event, PaymentStatusChanged.class);
                if ("REFUNDED".equals(change.status())) {
                    once(event, () -> sendRefundDone(change));
                }
            }
            default -> log.debug("Event type {} is not for notification-service, skipped", event.type());
        }
    }

    // every try failed: only the event id and type go to the log, never the payload (codes, links)
    @DltHandler
    public void onDeadLetter(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            EventEnvelope event = eventReader.read(message);
            log.error("Event {} ({}) gave up after 4 tries, it is on {}", event.eventId(), event.type(), topic);
        } catch (IllegalArgumentException e) {
            log.error("A message that is not an event landed on {}", topic);
        }
    }

    // already done -> nothing; else the email, then the event is marked done (a crash in between = one more email)
    private void once(EventEnvelope event, Runnable work) {
        if (!inbox.isNew(event.eventId(), CONSUMER)) {
            log.debug("Event {} was already handled", event.eventId());
            return;
        }
        work.run();
        inbox.markProcessed(event.eventId(), CONSUMER);
    }

    private void sendCode(OtpRequested otp) {
        EmailMessage email = "SIGNUP".equals(otp.purpose())
                ? new OtpEmail(otp.username(), otp.code(), otp.minutes())
                : new PasswordResetEmail(otp.username(), otp.code(), otp.minutes());
        emailService.send(otp.email(), email, List.of());
    }

    private void sendInvite(AdminInvited invite) {
        emailService.send(invite.email(), new AdminInviteEmail(invite.username(), invite.link(), invite.hours()),
                List.of());
    }

    private void sendDecision(BookingDecided booking) {
        emailService.send(booking.driverEmail(), new BookingStatusEmail(booking.driverName(), booking.trackName(),
                booking.sessionDate(), booking.startTime(), booking.endTime(), booking.carNumber(), booking.status(),
                booking.note(), booking.refundAmount()), ticketFiles(booking));
    }

    private void sendWaitlisted(BookingWaitlisted booking) {
        emailService.send(booking.driverEmail(), new WaitlistEmail(booking.driverName(), booking.trackName(),
                booking.sessionDate(), booking.startTime(), booking.endTime(), booking.carNumber(), booking.note()),
                List.of());
    }

    private void sendPromoted(BookingPromoted booking) {
        emailService.send(booking.driverEmail(), new PromotedEmail(booking.driverName(), booking.trackName(),
                booking.sessionDate(), booking.startTime(), booking.endTime(), booking.carNumber()), List.of());
    }

    private void sendRefundDone(PaymentStatusChanged refund) {
        emailService.send(refund.driverEmail(), new RefundDoneEmail(refund.driverName(), refund.amount(),
                refund.trackName(), refund.sessionDate()), List.of());
    }

    // an approval with a ticket code -> the e-ticket pdf + the calendar file, anything else -> no files
    private List<Attachment> ticketFiles(BookingDecided booking) {
        return Optional.ofNullable(booking.ticketCode())
                .filter(code -> "Approved".equals(booking.status()))
                .map(code -> List.of(
                        new Attachment("gridpulse-ticket-" + booking.bookingId() + ".pdf", "application/pdf",
                                TicketPdf.render(new TicketData(booking.bookingId(), code, booking.driverName(),
                                        booking.trackName(), booking.trackLocation(), booking.sessionDate(),
                                        booking.startTime(), booking.endTime(), booking.carNumber(),
                                        booking.carCategory()))),
                        new Attachment("gridpulse-booking-" + booking.bookingId() + ".ics",
                                "text/calendar; charset=UTF-8; method=PUBLISH",
                                IcsBuilder.build(booking, Instant.now()).getBytes(StandardCharsets.UTF_8))))
                .orElse(List.of());
    }
}
