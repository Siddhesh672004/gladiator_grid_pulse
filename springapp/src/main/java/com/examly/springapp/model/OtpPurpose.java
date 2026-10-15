package com.examly.springapp.model;

// a sign up code can never reset a password and the other way round
public enum OtpPurpose {
    SIGNUP,
    RESET_PASSWORD,
    // the set-password link of a new admin (a long token, not 6 digits)
    ADMIN_INVITE
}
