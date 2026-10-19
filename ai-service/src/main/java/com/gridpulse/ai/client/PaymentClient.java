package com.gridpulse.ai.client;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.fasterxml.jackson.databind.JsonNode;

// payment-service's api with the caller's own token (ClientConfig); a 4xx -> ToolCallException
@HttpExchange("/api/payments")
public interface PaymentClient {

    // the same order as "Pay & Register": angular opens razorpay with it and confirms like the booking page
    @PostExchange("/order")
    JsonNode order(@RequestBody Map<String, Object> booking);

    @GetExchange("/summary")
    JsonNode summary();

    @GetExchange("/audit")
    JsonNode trail(@RequestParam String orderId);

    @GetExchange("/page")
    JsonNode page(@RequestParam(required = false) String status, @RequestParam int page, @RequestParam int size);

    @PostExchange("/{paymentId}/retry-refund")
    JsonNode retryRefund(@PathVariable Long paymentId);
}
