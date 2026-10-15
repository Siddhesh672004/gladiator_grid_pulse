package com.gridpulse.payment.dto.response;

// the driver as the core's UserResponse shows it, from the payment's copy (userRole is not known here: null)
public record UserResponse(
        Integer userId,
        String email,
        String username,
        String mobileNumber,
        String userRole) {
}
