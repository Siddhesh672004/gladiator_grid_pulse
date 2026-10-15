package com.examly.springapp.service;

import com.examly.springapp.model.OtpPurpose;
import com.examly.springapp.model.User;

// the 6-digit codes for sign up and reset password, and the set-password token of an admin invite
public interface OtpService {

    // returns the seconds the code is valid. at sign up the user is not saved yet, only email + name are used
    int createAndSend(User user, OtpPurpose purpose);

    // a new token closes the older ones, only its BCrypt hash is stored. returns the token for the link
    String createInvite(User admin);

    void verify(String email, OtpPurpose purpose, String code);

    // 0 = a new code may be sent now
    long secondsUntilNextCode(String email, OtpPurpose purpose);

    boolean hasUsableCode(String email, OtpPurpose purpose);
}
