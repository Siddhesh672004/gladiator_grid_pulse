package com.gridpulse.core.service;

import com.gridpulse.core.dto.request.ProfileRequest;
import com.gridpulse.core.dto.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile();

    // date of birth + license are filled once and then locked, the picture can change any time
    // taken license / changed locked value -> ConflictException (409)
    ProfileResponse updateProfile(ProfileRequest request);
}
