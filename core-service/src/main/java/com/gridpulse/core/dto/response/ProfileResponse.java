package com.gridpulse.core.dto.response;

import java.time.LocalDate;

import com.gridpulse.core.model.Role;

public record ProfileResponse(
        int userId,
        String email,
        String username,
        String mobileNumber,
        Role userRole,
        LocalDate dateOfBirth,
        String driverLicenseNumber,
        String profilePicture,
        boolean profileComplete) {      // false = a driver who cannot book yet (no date of birth / license)
}
