package com.gridpulse.core.booking.service;

import com.gridpulse.core.booking.dto.TicketCheckResponse;

// e-tickets: the signed code of an approved booking, its pdf, the check at the gate
public interface TicketService {

    String codeFor(Long bookingId);

    // the driver's own or any for an admin; not Approved -> 400
    byte[] ticketPdf(Long bookingId);

    TicketCheckResponse check(String code);
}
