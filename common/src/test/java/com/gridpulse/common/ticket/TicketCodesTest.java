package com.gridpulse.common.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TicketCodesTest {

    private static final String SECRET = "ticket-secret-for-tests";

    @Test
    void signedCodeVerifies() {
        String code = TicketCodes.sign(42, SECRET);

        assertThat(code).matches("GP-42-[0-9a-f]{16}");
        assertThat(TicketCodes.verify(code, SECRET)).contains(42L);
        assertThat(TicketCodes.verify("  " + code + " ", SECRET)).contains(42L);
    }

    @Test
    void tamperedCodesFail() {
        String code = TicketCodes.sign(42, SECRET);
        String otherId = code.replace("GP-42-", "GP-43-");
        char last = code.charAt(code.length() - 1);
        String otherHex = code.substring(0, code.length() - 1) + (last == '0' ? '1' : '0');

        assertThat(TicketCodes.verify(otherId, SECRET)).isEmpty();
        assertThat(TicketCodes.verify(otherHex, SECRET)).isEmpty();
        assertThat(TicketCodes.verify(code, "another-secret")).isEmpty();
        assertThat(TicketCodes.verify("GP-42", SECRET)).isEmpty();
        assertThat(TicketCodes.verify(null, SECRET)).isEmpty();
    }

    @Test
    void pdfIsMade() {
        TicketData ticket = new TicketData(42, TicketCodes.sign(42, SECRET), "Aarav Rao", "Buddh Circuit", "Greater Noida",
                LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), LocalTime.of(11, 0), 27, "GT3");

        byte[] pdf = TicketPdf.render(ticket);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
