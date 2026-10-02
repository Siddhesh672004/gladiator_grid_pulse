package com.examly.springapp.service;

import com.examly.springapp.dto.request.EmailRequest;
import com.examly.springapp.dto.request.LoginRequest;
import com.examly.springapp.dto.request.ResetPasswordRequest;
import com.examly.springapp.dto.response.LoginResponse;
import com.examly.springapp.dto.response.MessageResponse;

// login for drivers and admins (two login pages) + forgot / reset password
public interface AuthService {

    // drivers only, wrong email / password / not a driver -> 401
    LoginResponse login(LoginRequest request);

    LoginResponse adminLogin(LoginRequest request);

    // same answer for every email
    MessageResponse forgotPassword(EmailRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
