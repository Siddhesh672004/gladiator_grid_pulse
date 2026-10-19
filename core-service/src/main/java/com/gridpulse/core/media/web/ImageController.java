package com.gridpulse.core.media.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gridpulse.core.media.dto.ImageResponse;
import com.gridpulse.core.media.service.CloudinaryService;
import com.gridpulse.web.security.AdminOnly;
import com.gridpulse.web.security.LoggedIn;

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
