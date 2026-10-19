package com.gridpulse.core.config;

import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.gridpulse.core.config.properties.SuperAdminProperties;
import com.gridpulse.core.mapper.UserMapper;
import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every start: creates / updates the ONE super admin from secrets.properties (new email -> the old row is switched off)
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final SuperAdminProperties superAdmin;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // nothing in secrets.properties -> only the warning
        Optional.of(superAdmin)
                .filter(SuperAdminProperties::isConfigured)
                .ifPresentOrElse(settings -> checkPassword(), () -> log.warn(
                        "No super admin configured - set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD in "
                                + "secrets.properties"));
    }

    private void checkPassword() {
        // a short password stops here, the account stays as it is
        Optional.of(superAdmin.password())
                .filter(password -> password.length() >= MIN_PASSWORD_LENGTH)
                .ifPresentOrElse(password -> checkEmail(UserMapper.cleanEmail(superAdmin.email())), () -> log.error(
                        "SUPER_ADMIN_PASSWORD must be at least {} characters - super admin NOT created / updated",
                        MIN_PASSWORD_LENGTH));
    }

    private void checkEmail(String email) {
        Optional<User> existing = userRepo.findByEmailIgnoreCase(email);
        User account = existing.orElseGet(() -> new User(email, null, "Super Admin", null, Role.SUPER_ADMIN));
        String done = existing.isPresent() ? "updated" : "created";
        // the email of a driver / admin account cannot become the super admin
        existing.filter(user -> user.getUserRole() != Role.SUPER_ADMIN)
                .ifPresentOrElse(
                        user -> log.error("SUPER_ADMIN_EMAIL {} already belongs to a {} account - choose another email",
                                email, user.getUserRole().getLabel()),
                        () -> createOrUpdate(account, email, done));
    }

    private void createOrUpdate(User account, String email, String done) {
        // saved only when the password is new or the account was switched off
        Optional.of(account)
                .filter(user -> passwordChanged(user) || !user.isEnabled())
                .ifPresent(user -> {
                    user.setPassword(passwordEncoder.encode(superAdmin.password()));
                    user.setEnabled(true);
                    userRepo.save(user);
                    log.info("Super admin {} {}", email, done);
                });
        switchOffOldSuperAdmins(email);
    }

    private boolean passwordChanged(User user) {
        // a new account has no password yet
        return Optional.ofNullable(user.getPassword())
                .map(hash -> !passwordEncoder.matches(superAdmin.password(), hash))
                .orElse(true);
    }

    private void switchOffOldSuperAdmins(String currentEmail) {
        // a super admin with another email that is still on gets switched off
        userRepo.findByUserRole(Role.SUPER_ADMIN).stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase(currentEmail))
                .filter(User::isEnabled)
                .forEachOrdered(user -> {
                    user.setEnabled(false);
                    userRepo.save(user);
                    log.warn("Old super admin {} switched off (SUPER_ADMIN_EMAIL changed)", user.getEmail());
                });
    }
}
