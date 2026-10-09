package com.examly.springapp.dto.response;

// everything the razorpay checkout window needs, keyId is the PUBLIC key (the secret stays on the server)
// amount is in paise: 650 rupees = 65000
public record PaymentOrderResponse(
        String keyId,
        String orderId,
        long amount,
        String currency,
        String description,
        String name,
        String email,
        String contact) {
}
