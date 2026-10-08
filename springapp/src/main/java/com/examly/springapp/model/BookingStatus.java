package com.examly.springapp.model;

// plain text in the db and in the json, these constants only stop typos in the services
public final class BookingStatus {

    public static final String PENDING = "Pending";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";

    private BookingStatus() {
    }
}
