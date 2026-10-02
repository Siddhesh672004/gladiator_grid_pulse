package com.examly.springapp.service;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.JwtUtils;
import com.examly.springapp.dto.request.EmailRequest;
import com.examly.springapp.dto.request.LoginRequest;
import com.examly.springapp.dto.request.ResetPasswordRequest;
import com.examly.springapp.dto.response.LoginResponse;
import com.examly.springapp.dto.response.MessageResponse;
import com.examly.springapp.exceptions.EmailSendException;
import com.examly.springapp.exceptions.OtpException;
import com.examly.springapp.mapper.UserMapper;
import com.examly.springapp.model.OtpPurpose;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;

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

    // a gmail problem only goes to the log, the answer stays the same
    private void sendResetCode(User user) {
        try {
            otpService.createAndSend(user, OtpPurpose.RESET_PASSWORD);
        } catch (EmailSendException e) {
            log.error("Reset code for {} could not be emailed", user.getEmail());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
