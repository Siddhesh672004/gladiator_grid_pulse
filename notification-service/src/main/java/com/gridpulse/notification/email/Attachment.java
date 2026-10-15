package com.gridpulse.notification.email;

// a file that goes with an email (e-ticket pdf, calendar file)
public record Attachment(String filename, String contentType, byte[] data) {

    // the bytes are not printed
    @Override
    public String toString() {
        return "Attachment[" + filename + ", " + data.length + " bytes]";
    }
}
