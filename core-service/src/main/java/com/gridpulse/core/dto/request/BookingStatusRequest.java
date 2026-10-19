package com.gridpulse.core.dto.request;

import com.gridpulse.core.validation.ValidationRules;

import jakarta.validation.constraints.Size;

// admin approves or rejects a booking, a rejection needs an adminNote (the reason)
// the driver's own comments are never touched
public record BookingStatusRequest(

        String status,

        @Size(min = 5, message = "Reason must be at least 5 characters")
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String adminNote) {

    // an empty note becomes null: approving needs none
    public BookingStatusRequest {
        adminNote = ValidationRules.clean(adminNote);
    }
}
