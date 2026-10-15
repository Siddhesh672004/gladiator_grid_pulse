package com.gridpulse.payment.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.payment.dto.request.RaceBookingRequest;

// the core's /internal/bookings (InternalBookingController); only CoreCalls uses it (timeouts, breaker, retry)
// the booking stays the core's json: it goes back to angular unchanged
@HttpExchange("/internal/bookings")
public interface CoreClient {

    @PostExchange("/check")
    BookingQuote check(@RequestBody RaceBookingRequest request);

    // 201 new booking, 200 = this order already has one
    @PostExchange
    JsonNode createBooking(@RequestBody InternalBookingRequest request);

    @GetExchange("/by-order/{orderId}")
    JsonNode findByOrder(@PathVariable String orderId);
}
