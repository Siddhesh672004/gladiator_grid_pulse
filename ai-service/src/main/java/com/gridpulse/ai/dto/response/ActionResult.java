package com.gridpulse.ai.dto.response;

// the result of a confirmed action. paymentOrder + booking only for a booking: angular opens razorpay with them
// and confirms the payment exactly like the booking page
public record ActionResult(boolean ok, String message, Object paymentOrder, Object booking) {

    public static ActionResult done(String message) {
        return new ActionResult(true, message, null, null);
    }

    public static ActionResult payment(String message, Object paymentOrder, Object booking) {
        return new ActionResult(true, message, paymentOrder, booking);
    }

    public static ActionResult failed(String message) {
        return new ActionResult(false, message, null, null);
    }
}
