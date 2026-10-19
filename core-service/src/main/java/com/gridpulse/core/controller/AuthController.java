package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.request.LoginRequest;
import com.gridpulse.core.dto.request.SignupRequest;
import com.gridpulse.core.dto.response.LoginResponse;
import com.gridpulse.core.dto.response.OtpChallengeResponse;
import com.gridpulse.core.dto.response.UserResponse;
import com.gridpulse.core.service.AuthService;
import com.gridpulse.core.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// sign up (two steps) + the two logins, all public (SecurityConfig); the code is asked at sign up, not at login
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    // step 1: emails the code, no account is made yet
    @PostMapping("/api/register/send-code")
    public ResponseEntity<OtpChallengeResponse> sendSignupCode(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userService.sendSignupCode(request));
    }

    // step 2: same body + otp -> 201, the account is always a User
    @PostMapping("/api/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(request));
    }

    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // admin + super admin; 401 also for a driver or a disabled admin
    @PostMapping("/api/admin/login")
    public ResponseEntity<LoginResponse> loginAdmin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }
}
