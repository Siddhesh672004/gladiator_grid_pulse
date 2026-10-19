package com.gridpulse.core.account.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gridpulse.core.account.data.OtpPurpose;
import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.data.UserRepo;
import com.gridpulse.core.account.dto.OtpChallengeResponse;
import com.gridpulse.core.account.dto.SignupRequest;
import com.gridpulse.core.account.dto.UserResponse;
import com.gridpulse.core.account.mapper.UserMapper;
import com.gridpulse.core.account.security.UserPrinciple;
import com.gridpulse.core.platform.exception.ConflictException;
import com.gridpulse.core.platform.exception.OtpException;
import com.gridpulse.core.platform.exception.RegistrationException;
import com.gridpulse.core.platform.validation.ValidationRules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final OtpService otpService;

    // step 1: nothing is saved, the user object is only used for its email and name
    @Override
    public OtpChallengeResponse sendSignupCode(SignupRequest request) {
        User user = userMapper.toEntity(request);
        checkNotRegistered(user);
        String email = user.getEmail();

        long wait = otpService.secondsUntilNextCode(email, OtpPurpose.SIGNUP);
        return wait > 0 ? alreadySent(email, wait) : newCode(user);
    }

    // button pressed twice: the code from a moment ago is still valid, without one the driver has to wait
    private OtpChallengeResponse alreadySent(String email, long wait) {
        return Optional.of(email)
                .filter(address -> otpService.hasUsableCode(address, OtpPurpose.SIGNUP))
                .map(address -> new OtpChallengeResponse(address,
                        "A code was just sent to your email. You can ask for a new one in " + seconds(wait) + ".", 0))
                .orElseThrow(() -> new OtpException("Please wait " + seconds(wait) + " before asking for a new code."));
    }

    private OtpChallengeResponse newCode(User user) {
        int seconds = otpService.createAndSend(user, OtpPurpose.SIGNUP);
        return new OtpChallengeResponse(user.getEmail(), "We sent a 6-digit code to your email.", seconds);
    }

    @Override
    public UserResponse createUser(SignupRequest request) {
        User user = userMapper.toEntity(request);
        checkNotRegistered(user);
        checkCodeIsTyped(request.otp());
        otpService.verify(user.getEmail(), OtpPurpose.SIGNUP, request.otp());

        user.setPassword(passwordEncoder.encode(request.password()));
        try {
            User saved = userRepo.save(user);
            log.info("New {} registered: {} (id {})", saved.getUserRole().getLabel(), saved.getUsername(),
                    saved.getUserId());
            return userMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // two sign ups at once: a taken mobile / license gets its own message, else it was the email
            checkNotRegistered(user);
            throw new RegistrationException(ValidationRules.EMAIL_TAKEN, e);
        }
    }

    @Override
    public void checkNotRegistered(User user) {
        // email first, then mobile, then license: the first one that is taken is the message
        Optional.ofNullable(user.getEmail())
                .filter(userRepo::existsByEmailIgnoreCase)
                .ifPresent(taken -> {
                    throw new RegistrationException(ValidationRules.EMAIL_TAKEN);
                });
        // admins may have no mobile number or license (null)
        Optional.ofNullable(user.getMobileNumber())
                .filter(userRepo::existsByMobileNumber)
                .ifPresent(taken -> {
                    throw new ConflictException(ValidationRules.MOBILE_TAKEN);
                });
        Optional.ofNullable(user.getDriverLicenseNumber())
                .filter(userRepo::existsByDriverLicenseNumber)
                .ifPresent(taken -> {
                    throw new ConflictException(ValidationRules.LICENSE_TAKEN);
                });
    }

    private String seconds(long wait) {
        return wait == 1 ? "1 second" : wait + " seconds";
    }

    // a missing or misspelled code must not count as one of the 5 wrong tries
    private void checkCodeIsTyped(String code) {
        String typed = Optional.ofNullable(code)
                .filter(text -> !text.isBlank())
                .orElseThrow(() -> new OtpException(ValidationRules.CODE_REQUIRED));
        // digits only, then the length: the first broken rule is the message
        Stream.of(Map.entry(ValidationRules.CODE_DIGITS_PATTERN, ValidationRules.CODE_DIGITS_ONLY),
                        Map.entry(ValidationRules.CODE_LENGTH_PATTERN, ValidationRules.CODE_INVALID))
                .filter(rule -> !typed.matches(rule.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(message -> {
                    throw new OtpException(message);
                });
    }

    @Override
    public User getLoggedInUser() {
        UserPrinciple principle = currentPrinciple();
        return userRepo.findById(principle.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Please log in to continue"));
    }

    @Override
    public void checkOwnerOrAdmin(int ownerUserId) {
        UserPrinciple principle = currentPrinciple();
        // an admin sees everything, a driver only the own records
        Optional.of(principle)
                .filter(caller -> caller.isAdmin() || caller.getUserId() == ownerUserId)
                .orElseThrow(() -> {
                    log.warn("User {} tried to access data of user {}", principle.getUserId(), ownerUserId);
                    return new AccessDeniedException("You can only access your own records");
                });
    }

    // no login at all, or one that is not ours (anonymous) -> 403
    private UserPrinciple currentPrinciple() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(UserPrinciple.class::isInstance)
                .map(UserPrinciple.class::cast)
                .orElseThrow(() -> new AccessDeniedException("Please log in to continue"));
    }
}
