package com.gridpulse.core.service;

import java.util.List;

import com.gridpulse.core.dto.request.AddAdminRequest;
import com.gridpulse.core.dto.response.AdminResponse;
import com.gridpulse.core.dto.response.MessageResponse;

public interface SuperAdminService {

    // the invite email (set-password link) goes out after the save
    AdminResponse addAdmin(AddAdminRequest request);

    // a new link, the older one stops working. too soon after the last one -> 400
    MessageResponse resendInvite(int userId);

    List<AdminResponse> getAdmins();

    // disabled: can't log in and the tokens stop working. not an admin -> 404
    AdminResponse setAdminEnabled(int userId, boolean enabled);

    void deleteAdmin(int userId);
}
