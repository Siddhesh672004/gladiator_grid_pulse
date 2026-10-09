package com.examly.springapp.service;

// the one place that talks to Razorpay. amounts are in paise (650 rupees = 65000)
// not reachable / refused -> GatewayException (503) with Razorpay's reason
public interface RazorpayService {

    boolean isConfigured();

    String keyId();

    String createOrder(long amountPaise, String receipt);

    boolean isValidSignature(String orderId, String paymentId, String signature);

    String refund(String paymentId, long amountPaise);

    // payment id of this order that went through, null when nobody paid
    String findCapturedPayment(String orderId);
}
