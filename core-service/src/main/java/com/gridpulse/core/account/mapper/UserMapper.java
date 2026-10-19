package com.gridpulse.core.account.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.gridpulse.core.account.data.Role;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.dto.AddAdminRequest;
import com.gridpulse.core.account.dto.AdminResponse;
import com.gridpulse.core.account.dto.AdminUserResponse;
import com.gridpulse.core.account.dto.LoginResponse;
import com.gridpulse.core.account.dto.ProfileResponse;
import com.gridpulse.core.account.dto.SignupRequest;
import com.gridpulse.core.account.dto.UserResponse;
import com.gridpulse.core.account.dto.UserSummaryResponse;

// passwords are still plain text here, the services hash them before saving
@Component
public class UserMapper {

    // the role is always User, whatever the browser sends
    public User toEntity(SignupRequest request) {
        User driver = new User(cleanEmail(request.email()), request.password(), request.username().trim(),
                request.mobileNumber(), Role.USER);
        driver.setDateOfBirth(request.dateOfBirth());
        driver.setDriverLicenseNumber(cleanLicense(request.driverLicenseNumber()));
        return driver;
    }

    public User toAdminEntity(AddAdminRequest request) {
        // admins may leave the mobile number empty, then none is saved
        String mobile = Optional.ofNullable(request.mobileNumber())
                .filter(number -> !number.isBlank())
                .orElse(null);
        // no password yet, SuperAdminServiceImpl sets one nobody knows until the invite link is used
        return new User(cleanEmail(request.email()), null, request.username().trim(), mobile, Role.ADMIN);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getEmail(), user.getUsername(), user.getMobileNumber(),
                user.getUserRole());
    }

    public UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getUserId(), user.getUsername());
    }

    public LoginResponse toLoginResponse(User user, String token) {
        return new LoginResponse(token, user.getUsername(), user.getUserRole(), user.getUserId());
    }

    public AdminResponse toAdminResponse(User user) {
        return new AdminResponse(user.getUserId(), user.getEmail(), user.getUsername(), user.getMobileNumber(),
                user.isEnabled(), user.getCreatedAt());
    }

    public AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(user.getUserId(), user.getUsername(), user.getEmail(), user.getMobileNumber(),
                user.isEnabled(), user.isProfileComplete(), user.getCreatedAt());
    }

    // the profile page. admins have nothing to fill in, so their profile always counts as complete
    public ProfileResponse toProfileResponse(User user) {
        boolean complete = user.getUserRole() != Role.USER || user.isProfileComplete();
        return new ProfileResponse(user.getUserId(), user.getEmail(), user.getUsername(), user.getMobileNumber(),
                user.getUserRole(), user.getDateOfBirth(), user.getDriverLicenseNumber(), user.getProfilePicture(),
                complete);
    }

    // lower case + trimmed, so one address can't be used in two spellings
    public static String cleanEmail(String email) {
        // no email at all counts as an empty one
        return Optional.ofNullable(email)
                .map(typed -> typed.trim().toLowerCase())
                .orElse("");
    }

    // trimmed + upper case, same reason
    public static String cleanLicense(String license) {
        return license.trim().toUpperCase();
    }
}
