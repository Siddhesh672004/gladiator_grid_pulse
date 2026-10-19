package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.request.EmailRequest;
import com.gridpulse.core.dto.request.ResetPasswordRequest;
import com.gridpulse.core.dto.request.SetPasswordRequest;
import com.gridpulse.core.service.AuthService;
import com.gridpulse.web.error.MessageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// forgot password for drivers and admins + the first password of an invited admin, public
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {

    private final AuthService authService;

    // same 200 answer whether the account exists or not
    @PostMapping("/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // the page behind the admin invite link; used, expired or wrong link -> 400
    @PostMapping("/set-initial")
    public ResponseEntity<MessageResponse> setInitialPassword(@Valid @RequestBody SetPasswordRequest request) {
        return ResponseEntity.ok(authService.setInitialPassword(request));
    }
}
