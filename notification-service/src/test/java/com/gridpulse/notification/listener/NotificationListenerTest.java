package com.gridpulse.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.gridpulse.common.event.BookingDecided;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.OtpRequested;
import com.gridpulse.common.event.PaymentStatusChanged;
import com.gridpulse.common.inbox.Inbox;
import com.gridpulse.notification.email.Attachment;
import com.gridpulse.notification.email.EmailMessage;
import com.gridpulse.notification.email.OtpEmail;
import com.gridpulse.notification.email.RefundDoneEmail;
import com.gridpulse.notification.service.EmailService;

// the listener without kafka: event text in, email out (mocked), once per event
class NotificationListenerTest {

    private final Inbox inbox = mock(Inbox.class);
    private final EmailService emailService = mock(EmailService.class);
    private final NotificationListener listener = new NotificationListener(new EventReader(), inbox, emailService);

    @Test
    void approvalGetsTicketAndCalendar() throws Exception {
        when(inbox.isNew("e1", NotificationListener.CONSUMER)).thenReturn(true);

        listener.onBookingEvent(envelope("e1", new BookingDecided(7L, "driver@gridpulse.test", "Aarav Rao", "Buddh",
                "Noida", LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), LocalTime.of(11, 0), 27, "GT3", "Approved",
                null, null, "GP-7-0123456789abcdef")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Attachment>> files = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(eq("driver@gridpulse.test"), any(EmailMessage.class), files.capture());
        assertThat(files.getValue()).extracting(Attachment::filename)
                .containsExactly("gridpulse-ticket-7.pdf", "gridpulse-booking-7.ics");
        verify(inbox).markProcessed("e1", NotificationListener.CONSUMER);
    }

    @Test
    void anEventIsEmailedOnce() throws Exception {
        when(inbox.isNew("e2", NotificationListener.CONSUMER)).thenReturn(false);

        listener.onAccountEvent(envelope("e2", new OtpRequested("a@b.co", "A", "123456", 10, "SIGNUP")));

        verify(emailService, never()).send(any(), any(), anyList());
    }

    @Test
    void signUpCodeAndUnknownTypes() throws Exception {
        when(inbox.isNew("e3", NotificationListener.CONSUMER)).thenReturn(true);

        listener.onAccountEvent(envelope("e3", new OtpRequested("a@b.co", "A", "123456", 10, "SIGNUP")));
        listener.onAccountEvent(EventJson.MAPPER.writeValueAsString(new EventEnvelope("e4", "ProfileChanged",
                Instant.now(), "springapp", EventJson.MAPPER.createObjectNode())));

        verify(emailService).send(eq("a@b.co"), any(OtpEmail.class), eq(List.of()));
        verify(inbox, never()).isNew("e4", NotificationListener.CONSUMER);
    }

    @Test
    void onlyAFinishedRefundIsEmailed() throws Exception {
        when(inbox.isNew("e5", NotificationListener.CONSUMER)).thenReturn(true);

        listener.onPaymentEvent(envelope("e5", new PaymentStatusChanged("order_1", 7L, "REFUNDED", 650.0,
                "driver@gridpulse.test", "Aarav Rao", "Buddh", LocalDate.of(2026, 10, 20), "Booking rejected")));
        listener.onPaymentEvent(envelope("e6", new PaymentStatusChanged("order_2", 8L, "REFUND_PENDING", 650.0,
                "driver@gridpulse.test", "Aarav Rao", "Buddh", LocalDate.of(2026, 10, 20), null)));

        ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(eq("driver@gridpulse.test"), email.capture(), eq(List.of()));
        assertThat(email.getValue()).isInstanceOf(RefundDoneEmail.class);
        assertThat(email.getValue().body()).contains("Rs.650 has been refunded");
        verify(inbox, never()).isNew("e6", NotificationListener.CONSUMER);
    }

    private static String envelope(String eventId, Object payload) throws Exception {
        return EventJson.MAPPER.writeValueAsString(new EventEnvelope(eventId, payload.getClass().getSimpleName(),
                Instant.now(), "springapp", EventJson.MAPPER.valueToTree(payload)));
    }
}
