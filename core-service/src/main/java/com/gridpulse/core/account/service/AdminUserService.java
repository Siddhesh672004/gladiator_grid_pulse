package com.gridpulse.core.account.service;

import com.gridpulse.common.paging.PageResponse;
import com.gridpulse.core.account.dto.AdminUserResponse;

// Manage Users page: admins look after driver accounts only (admins belong to the super admin)
public interface AdminUserService {

    PageResponse<AdminUserResponse> getUsers(String search, String status, int page, int size);

    // not found -> 404, not a driver -> 403, no change -> 409
    AdminUserResponse setUserEnabled(int userId, boolean enabled);
}
