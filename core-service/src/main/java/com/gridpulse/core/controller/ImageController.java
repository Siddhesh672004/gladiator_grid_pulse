package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gridpulse.core.dto.response.ImageResponse;
import com.gridpulse.core.security.annotation.AdminOnly;
import com.gridpulse.core.security.annotation.LoggedIn;
import com.gridpulse.core.service.CloudinaryService;

import lombok.RequiredArgsConstructor;

// picture upload: one file goes to cloudinary, the page gets the link and saves it with its form
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final CloudinaryService cloudinaryService;

    @AdminOnly
    @PostMapping("/track")
    public ResponseEntity<ImageResponse> uploadTrackPicture(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(new ImageResponse(cloudinaryService.upload(file, "tracks")));
    }

    @LoggedIn
    @PostMapping("/profile")
    public ResponseEntity<ImageResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(new ImageResponse(cloudinaryService.upload(file, "profiles")));
    }
}
