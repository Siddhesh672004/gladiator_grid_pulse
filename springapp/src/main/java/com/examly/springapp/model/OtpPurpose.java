package com.examly.springapp.model;

// a sign up code can never reset a password and the other way round
public enum OtpPurpose {
    SIGNUP,
    RESET_PASSWORD
}
