package com.gridpulse.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gridpulse.core.dto.request.AdminStatusRequest;
import com.gridpulse.core.dto.response.AdminUserResponse;
import com.gridpulse.core.dto.response.PageResponse;
import com.gridpulse.core.service.AdminUserService;
import com.gridpulse.web.security.AdminOnly;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Manage Users page: driver accounts only
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @AdminOnly
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUserService.getUsers(search, status, page, size));
    }

    // 403 for an admin account, 409 when it is already in that state
    @AdminOnly
    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse> setStatus(@PathVariable int userId,
            @Valid @RequestBody AdminStatusRequest request) {
        return ResponseEntity.ok(adminUserService.setUserEnabled(userId, request.enabled()));
    }
}
