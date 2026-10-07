package com.examly.springapp.model;

// SCHEDULED -> LIVE -> COMPLETED, or SCHEDULED -> CANCELLED
// bookings are made / approved / rejected only while SCHEDULED, feedback only when COMPLETED
public enum SessionStatus {
    SCHEDULED,
    LIVE,
    COMPLETED,
    CANCELLED
}
