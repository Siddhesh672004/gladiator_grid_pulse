package com.gridpulse.core.account.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.account.dto.ProfileRequest;
import com.gridpulse.core.account.dto.ProfileResponse;
import com.gridpulse.core.account.service.ProfileService;
import com.gridpulse.web.security.LoggedIn;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// profile of whoever is logged in; no id in the url, the user comes from the token
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @LoggedIn
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    // date of birth + license are filled once and locked; 409 if the license belongs to another driver
    @LoggedIn
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }
}
