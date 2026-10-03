package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.request.AddAdminRequest;
import com.examly.springapp.dto.request.AdminStatusRequest;
import com.examly.springapp.dto.response.AdminResponse;
import com.examly.springapp.security.annotation.SuperAdminOnly;
import com.examly.springapp.service.SuperAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// the super admin manages the admin accounts (an admin gets 403)
@RestController
@RequestMapping("/api/super-admin/admins")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // sends the invite email; 503 when it could not be sent
    @SuperAdminOnly
    @PostMapping
    public ResponseEntity<AdminResponse> addAdmin(@Valid @RequestBody AddAdminRequest request) {
        return ResponseEntity.status(201).body(superAdminService.addAdmin(request));
    }

    @SuperAdminOnly
    @GetMapping
    public ResponseEntity<List<AdminResponse>> getAdmins() {
        return ResponseEntity.ok(superAdminService.getAdmins());
    }

    @SuperAdminOnly
    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminResponse> setStatus(@PathVariable int userId,
            @Valid @RequestBody AdminStatusRequest request) {
        return ResponseEntity.ok(superAdminService.setAdminEnabled(userId, request.enabled()));
    }

    // 404 for anything that is not an admin (also the super admin itself and drivers)
    @SuperAdminOnly
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable int userId) {
        superAdminService.deleteAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}
