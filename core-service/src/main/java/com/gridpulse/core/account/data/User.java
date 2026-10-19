package com.gridpulse.core.account.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// one row per account. the api never sends this class out, UserResponse does, so the password hash cannot leak
@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(unique = true, nullable = false)
    private String email;

    // BCrypt hash, never the real password
    private String password;

    private String username;

    // one mobile number = one account, admins may have none
    @Column(unique = true)
    private String mobileNumber;

    private Role userRole;

    // false = switched off (an admin by the super admin, a driver by an admin): no login, old tokens stop working
    @Column(columnDefinition = "boolean default true")
    private boolean enabled = true;

    private LocalDateTime createdAt;

    // driver profile: typed once at sign up (or on the profile page for older accounts), then locked
    private LocalDate dateOfBirth;

    // one license = one driver (unique), stored trimmed + upper case
    @Column(unique = true, length = 30)
    private String driverLicenseNumber;

    @Column(length = 500)
    private String profilePicture;

    public User(String email, String password, String username, String mobileNumber, Role userRole) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.userRole = userRole;
    }

    public User(int userId, String email, String password, String username, String mobileNumber, Role userRole) {
        this(email, password, username, mobileNumber, userRole);
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // a driver may only book once the date of birth and the license are on file (the picture is optional)
    public boolean isProfileComplete() {
        return dateOfBirth != null && driverLicenseNumber != null;
    }
}
