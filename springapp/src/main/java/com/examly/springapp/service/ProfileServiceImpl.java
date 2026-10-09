package com.examly.springapp.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.examly.springapp.dto.request.ProfileRequest;
import com.examly.springapp.dto.response.ProfileResponse;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.mapper.UserMapper;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import com.examly.springapp.validation.ValidationRules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// the profile page, always the logged-in user (from the token)
// date of birth + license are typed once and then locked, every booking copies the license from here
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final String LOCKED = "Date of birth and driving license number cannot be changed once they are saved";

    private final UserService userService;
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public ProfileResponse getProfile() {
        return userMapper.toProfileResponse(userService.getLoggedInUser());
    }

    @Override
    public ProfileResponse updateProfile(ProfileRequest request) {
        User user = userService.getLoggedInUser();
        // only a driver has a date of birth + license to save
        Optional.of(user)
                .filter(account -> account.getUserRole() == Role.USER)
                .ifPresent(driver -> saveDriverFields(driver, request));
        user.setProfilePicture(checkedPicture(request.profilePicture()));
        return userMapper.toProfileResponse(userRepo.save(user));
    }

    // no picture is fine (null), a picture must be one we uploaded
    private String checkedPicture(String url) {
        return Optional.ofNullable(url)
                .filter(link -> !link.isBlank())
                .map(this::ourPicture)
                .orElse(null);
    }

    private String ourPicture(String url) {
        return Optional.of(url)
                .filter(cloudinaryService::isOurImage)
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.PICTURE_NOT_OURS));
    }

    // saved once = locked: the old values are only compared, an empty profile is filled in
    private void saveDriverFields(User driver, ProfileRequest request) {
        Optional.of(driver)
                .filter(User::isProfileComplete)
                .ifPresentOrElse(locked -> checkNothingChanged(locked, request),
                        () -> fillDriverFields(driver, request));
    }

    // only a driver must fill these two in, so "required" is checked here and not in ProfileRequest
    private void fillDriverFields(User driver, ProfileRequest request) {
        LocalDate dateOfBirth = Optional.ofNullable(request.dateOfBirth())
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.DOB_REQUIRED));
        String license = Optional.ofNullable(request.driverLicenseNumber())
                .filter(typed -> !typed.isBlank())
                .map(UserMapper::cleanLicense)
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.LICENSE_REQUIRED));
        // one license belongs to one driver
        Optional.of(license)
                .filter(userRepo::existsByDriverLicenseNumber)
                .ifPresent(taken -> {
                    throw new ConflictException(ValidationRules.LICENSE_TAKEN);
                });
        driver.setDateOfBirth(dateOfBirth);
        driver.setDriverLicenseNumber(license);
        log.info("Driver {} completed the profile", driver.getUserId());
    }

    // the page sends the whole form again: same values are fine
    private void checkNothingChanged(User driver, ProfileRequest request) {
        boolean newDate = Optional.ofNullable(request.dateOfBirth())
                .filter(date -> !date.equals(driver.getDateOfBirth()))
                .isPresent();
        boolean newLicense = Optional.ofNullable(request.driverLicenseNumber())
                .filter(typed -> !typed.isBlank())
                .map(UserMapper::cleanLicense)
                .filter(license -> !license.equals(driver.getDriverLicenseNumber()))
                .isPresent();
        // a different date or license is refused
        Optional.of(driver)
                .filter(locked -> !newDate && !newLicense)
                .orElseThrow(() -> new ConflictException(LOCKED));
    }
}
