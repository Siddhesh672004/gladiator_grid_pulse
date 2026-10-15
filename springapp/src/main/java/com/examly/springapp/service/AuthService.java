package com.examly.springapp.service;

import com.examly.springapp.dto.request.EmailRequest;
import com.examly.springapp.dto.request.LoginRequest;
import com.examly.springapp.dto.request.ResetPasswordRequest;
import com.examly.springapp.dto.request.SetPasswordRequest;
import com.examly.springapp.dto.response.LoginResponse;
import com.examly.springapp.dto.response.MessageResponse;

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
