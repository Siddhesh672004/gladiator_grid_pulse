package com.examly.springapp.service;

import com.examly.springapp.dto.request.PaymentConfirmRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.PaymentOrderResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;

// checkout: createOrder -> the driver pays in the Razorpay window -> confirm
public interface PaymentService {

    PaymentOrderResponse createOrder(RaceBookingRequest request);

    // wrong signature -> 400. booking refused after paying -> the error + an automatic refund
    RaceBookingResponse confirm(PaymentConfirmRequest request);
}
