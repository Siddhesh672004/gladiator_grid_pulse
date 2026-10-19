package com.gridpulse.core.account.service;

import com.gridpulse.core.account.dto.EmailRequest;
import com.gridpulse.core.account.dto.LoginRequest;
import com.gridpulse.core.account.dto.LoginResponse;
import com.gridpulse.core.account.dto.ResetPasswordRequest;
import com.gridpulse.core.account.dto.SetPasswordRequest;
import com.gridpulse.web.error.MessageResponse;

// login for drivers and admins (two login pages) + forgot / reset password + the first password of an invited admin
public interface AuthService {

    // drivers only, wrong email / password / not a driver -> 401
    LoginResponse login(LoginRequest request);

    LoginResponse adminLogin(LoginRequest request);

    // same answer for every email
    MessageResponse forgotPassword(EmailRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    // invite link: works once, only for an admin account
    MessageResponse setInitialPassword(SetPasswordRequest request);
}
