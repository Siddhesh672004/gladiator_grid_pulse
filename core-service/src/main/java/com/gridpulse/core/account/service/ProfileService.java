package com.gridpulse.core.account.service;

import com.gridpulse.core.account.dto.ProfileRequest;
import com.gridpulse.core.account.dto.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile();

    // date of birth + license are filled once and then locked, the picture can change any time
    // taken license / changed locked value -> ConflictException (409)
    ProfileResponse updateProfile(ProfileRequest request);
}
