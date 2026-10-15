package com.gridpulse.payment.service;

import com.gridpulse.payment.dto.request.PaymentConfirmRequest;
import com.gridpulse.payment.dto.request.RaceBookingRequest;
import com.gridpulse.payment.dto.response.ConfirmResult;
import com.gridpulse.payment.dto.response.PaymentOrderResponse;

// checkout: createOrder -> the driver pays in the Razorpay window -> confirm
public interface PaymentService {

    PaymentOrderResponse createOrder(RaceBookingRequest request);

    // wrong signature -> 400. booking refused after paying -> the core's error + an automatic refund
    // core not answering -> "being confirmed" (RefundJob finishes it)
    ConfirmResult confirm(PaymentConfirmRequest request);
}
