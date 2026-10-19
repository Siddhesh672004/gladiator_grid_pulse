package com.gridpulse.core.service;

import com.gridpulse.core.dto.response.AdminUserResponse;
import com.gridpulse.core.dto.response.PageResponse;

// Manage Users page: admins look after driver accounts only (admins belong to the super admin)
public interface AdminUserService {

    PageResponse<AdminUserResponse> getUsers(String search, String status, int page, int size);

    // not found -> 404, not a driver -> 403, no change -> 409
    AdminUserResponse setUserEnabled(int userId, boolean enabled);
}
