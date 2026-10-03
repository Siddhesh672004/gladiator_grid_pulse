package com.examly.springapp.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.request.AddAdminRequest;
import com.examly.springapp.dto.response.AdminResponse;
import com.examly.springapp.email.AdminInviteEmail;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.UserMapper;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.OtpCodeRepo;
import com.examly.springapp.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OtpCodeRepo otpCodeRepo;

    // invite email fails -> the new admin is not saved either
    @Override
    @Transactional
    public AdminResponse addAdmin(AddAdminRequest request) {
        User admin = userMapper.toAdminEntity(request);
        userService.checkNotRegistered(admin);
        admin.setPassword(passwordEncoder.encode(request.initialPassword()));
        User saved = userRepo.save(admin);

        emailService.send(saved.getEmail(),
                new AdminInviteEmail(saved.getUsername(), saved.getEmail(), request.initialPassword()));
        log.info("Admin {} (id {}) added and invited", saved.getEmail(), saved.getUserId());
        return userMapper.toAdminResponse(saved);
    }

    @Override
    public List<AdminResponse> getAdmins() {
        return userRepo.findByUserRoleOrderByUserIdDesc(Role.ADMIN).stream()
                .map(userMapper::toAdminResponse)
                .toList();
    }

    @Override
    public AdminResponse setAdminEnabled(int userId, boolean enabled) {
        User admin = findAdmin(userId);
        admin.setEnabled(enabled);
        User saved = userRepo.save(admin);
        log.info("Admin {} is now {}", saved.getEmail(), enabled ? "enabled" : "disabled");
        return userMapper.toAdminResponse(saved);
    }

    // only admins are found here: the super admin can't delete itself, drivers are never deleted
    // old tokens stop working (JwtUtils checks the user id, even for a new account with the same email)
    @Override
    @Transactional
    public void deleteAdmin(int userId) {
        User admin = findAdmin(userId);
        otpCodeRepo.deleteByEmail(admin.getEmail());
        userRepo.delete(admin);
        log.info("Admin {} (id {}) deleted", admin.getEmail(), userId);
    }

    private User findAdmin(int userId) {
        return userRepo.findById(userId)
                .filter(user -> user.getUserRole() == Role.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Admin with ID: " + userId + " not found."));
    }
}
