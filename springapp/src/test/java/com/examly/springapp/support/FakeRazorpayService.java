package com.examly.springapp.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;

import com.examly.springapp.service.RazorpayService;

// razorpay for the tests: no internet, no money. predictable ids, "valid" is the only good signature
@Primary
@TestConfiguration(proxyBeanMethods = false)
public class FakeRazorpayService implements RazorpayService {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String keyId() {
        return "rzp_test_dummy";
    }

    @Override
    public String createOrder(long amountPaise, String receipt) {
        return "order_test_" + counter.incrementAndGet();
    }

    @Override
    public boolean isValidSignature(String orderId, String paymentId, String signature) {
        return "valid".equals(signature);
    }

    @Override
    public String refund(String paymentId, long amountPaise) {
        return "rfnd_test_" + counter.incrementAndGet();
    }

    @Override
    public String findCapturedPayment(String orderId) {
        return null;
    }
}
