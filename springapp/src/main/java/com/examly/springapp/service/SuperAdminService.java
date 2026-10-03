package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.request.AddAdminRequest;
import com.examly.springapp.dto.response.AdminResponse;

public interface SuperAdminService {

    // sends the invite email, mail failed -> 503 and nothing saved
    AdminResponse addAdmin(AddAdminRequest request);

    List<AdminResponse> getAdmins();

    // disabled: can't log in and the tokens stop working. not an admin -> 404
    AdminResponse setAdminEnabled(int userId, boolean enabled);

    void deleteAdmin(int userId);
}
