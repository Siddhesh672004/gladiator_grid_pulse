package com.gridpulse.core.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gridpulse.common.event.AdminInvited;
import com.gridpulse.core.config.properties.FrontendProperties;
import com.gridpulse.core.config.properties.OtpProperties;
import com.gridpulse.core.dto.request.AddAdminRequest;
import com.gridpulse.core.dto.response.AdminResponse;
import com.gridpulse.core.exceptions.OtpException;
import com.gridpulse.core.exceptions.ResourceNotFoundException;
import com.gridpulse.core.mapper.UserMapper;
import com.gridpulse.core.model.OtpPurpose;
import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.OtpCodeRepo;
import com.gridpulse.core.repository.UserRepo;
import com.gridpulse.web.error.MessageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// a new admin chooses the password through the emailed link (invite = OtpCode ADMIN_INVITE)
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final OtpService otpService;
    private final OtpProperties otpProperties;
    private final FrontendProperties frontendProperties;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OtpCodeRepo otpCodeRepo;

    // until the link is used the account has a random password nobody knows, so nobody can log in
    @Override
    @Transactional
    public AdminResponse addAdmin(AddAdminRequest request) {
        User admin = userMapper.toAdminEntity(request);
        userService.checkNotRegistered(admin);
        admin.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        User saved = userRepo.save(admin);

        invite(saved);
        log.info("Admin {} (id {}) added and invited", saved.getEmail(), saved.getUserId());
        return userMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public MessageResponse resendInvite(int userId) {
        User admin = findAdmin(userId);
        // same wait as the emailed codes, so a double click sends one email
        Optional.of(otpService.secondsUntilNextCode(admin.getEmail(), OtpPurpose.ADMIN_INVITE))
                .filter(wait -> wait > 0)
                .ifPresent(wait -> {
                    throw new OtpException("An invite was just sent. Please wait " + wait
                            + " seconds before sending a new one.");
                });
        invite(admin);
        log.info("Invite sent again to admin {}", admin.getEmail());
        return new MessageResponse("A new invite link was sent to " + admin.getEmail());
    }

    // the invite goes to kafka with this transaction (messaging/DomainEventOutboxListener), notification-service emails it
    private void invite(User admin) {
        String token = otpService.createInvite(admin);
        String link = frontendProperties.link("/set-password?email="
                + URLEncoder.encode(admin.getEmail(), StandardCharsets.UTF_8) + "&token=" + token);
        eventPublisher.publishEvent(new AdminInvited(admin.getEmail(), admin.getUsername(), link,
                otpProperties.inviteHours()));
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
