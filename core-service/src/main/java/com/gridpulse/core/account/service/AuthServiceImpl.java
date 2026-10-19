package com.gridpulse.core.account.service;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gridpulse.core.account.data.OtpPurpose;
import com.gridpulse.core.account.data.Role;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.data.UserRepo;
import com.gridpulse.core.account.dto.EmailRequest;
import com.gridpulse.core.account.dto.LoginRequest;
import com.gridpulse.core.account.dto.LoginResponse;
import com.gridpulse.core.account.dto.ResetPasswordRequest;
import com.gridpulse.core.account.dto.SetPasswordRequest;
import com.gridpulse.core.account.mapper.UserMapper;
import com.gridpulse.core.account.security.JwtUtils;
import com.gridpulse.core.platform.exception.OtpException;
import com.gridpulse.web.error.MessageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// every wrong login is the same 401, so nobody can find out which accounts exist
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // the one wrong-login text, also used by MyUserDetailsService
    public static final String WRONG_LOGIN = "Invalid Email or Password";
    private static final String FORGOT_ANSWER =
            "If this email belongs to an account, a reset code has been sent to it.";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;
    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = checkPassword(request);
        // an admin on the driver page gets the wrong-login answer
        Optional.of(user)
                .filter(driver -> driver.getUserRole() == Role.USER)
                .orElseThrow(() -> {
                    log.warn("{} {} tried the driver login page", user.getUserRole().getLabel(), user.getEmail());
                    return new BadCredentialsException(WRONG_LOGIN);
                });
        // a switched off account stays out
        Optional.of(user)
                .filter(User::isEnabled)
                .orElseThrow(() -> {
                    log.warn("Disabled driver {} tried to log in", user.getEmail());
                    return new DisabledException("This account is disabled. Please contact GridPulse.");
                });
        return loggedIn(user);
    }

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        User user = checkPassword(request);
        // a driver on the admin page gets the wrong-login answer
        Optional.of(user)
                .filter(admin -> admin.getUserRole().isAdmin())
                .orElseThrow(() -> {
                    log.warn("Driver {} tried the admin login page", user.getEmail());
                    return new BadCredentialsException(WRONG_LOGIN);
                });
        // a switched off admin stays out
        Optional.of(user)
                .filter(User::isEnabled)
                .orElseThrow(() -> {
                    log.warn("Disabled admin {} tried to log in", user.getEmail());
                    return new DisabledException("This admin account is disabled. Please contact the Super Admin.");
                });
        return loggedIn(user);
    }

    @Override
    public MessageResponse forgotPassword(EmailRequest request) {
        String email = UserMapper.cleanEmail(request.email());
        // super admin's password is in secrets.properties. a code goes out only when the last one is old enough
        userRepo.findByEmailIgnoreCase(email)
                .filter(user -> user.getUserRole() != Role.SUPER_ADMIN && user.isEnabled())
                .filter(user -> otpService.secondsUntilNextCode(email, OtpPurpose.RESET_PASSWORD) == 0)
                .ifPresent(this::sendResetCode);
        return new MessageResponse(FORGOT_ANSWER);
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = UserMapper.cleanEmail(request.email());
        otpService.verify(email, OtpPurpose.RESET_PASSWORD, request.code());
        // no account behind the email, or the super admin -> same answer as a wrong code
        User user = userRepo.findByEmailIgnoreCase(email)
                .filter(found -> found.getUserRole() != Role.SUPER_ADMIN)
                .orElseThrow(() -> new OtpException("This code is not valid for this email."));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
        log.info("Password reset for {}", email);
        return new MessageResponse("Your password was changed. You can log in now.");
    }

    @Override
    public MessageResponse setInitialPassword(SetPasswordRequest request) {
        String email = UserMapper.cleanEmail(request.email());
        otpService.verify(email, OtpPurpose.ADMIN_INVITE, request.token());
        // only an admin gets an invite: a driver or the super admin with the same email -> same answer as a bad link
        User admin = userRepo.findByEmailIgnoreCase(email)
                .filter(found -> found.getUserRole() == Role.ADMIN)
                .orElseThrow(() -> new OtpException("This invite link is not valid for this email."));
        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(admin);
        log.info("Admin {} chose a password", email);
        return new MessageResponse("Your password is set. You can log in now.");
    }

    private User checkPassword(LoginRequest request) {
        // an empty box is a wrong login too
        Optional.of(request)
                .filter(typed -> !isBlank(typed.email()) && !isBlank(typed.password()))
                .orElseThrow(() -> new BadCredentialsException(WRONG_LOGIN));
        String email = UserMapper.cleanEmail(request.email());
        // unknown email and wrong password look the same from outside
        return userRepo.findByEmailIgnoreCase(email)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for {}", email);
                    return new BadCredentialsException(WRONG_LOGIN);
                });
    }

    private LoginResponse loggedIn(User user) {
        log.info("{} {} logged in", user.getUserRole().getLabel(), user.getUsername());
        return userMapper.toLoginResponse(user, jwtUtils.generateToken(user));
    }

    // the email goes out in the background, the answer stays the same
    private void sendResetCode(User user) {
        otpService.createAndSend(user, OtpPurpose.RESET_PASSWORD);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
