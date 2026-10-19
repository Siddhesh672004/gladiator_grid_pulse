package com.gridpulse.core.model;

// plain text in the db and in the json, these constants only stop typos in the services
public final class BookingStatus {

    public static final String PENDING = "Pending";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";
    // paid, the session is full: moves up to Pending by itself when a seat opens, refunded if none does
    public static final String WAITLISTED = "Waitlisted";

    private BookingStatus() {
    }
}
