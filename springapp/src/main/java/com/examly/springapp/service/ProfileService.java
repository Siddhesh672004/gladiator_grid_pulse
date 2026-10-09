package com.examly.springapp.service;

import com.examly.springapp.dto.request.ProfileRequest;
import com.examly.springapp.dto.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile();

    // date of birth + license are filled once and then locked, the picture can change any time
    // taken license / changed locked value -> ConflictException (409)
    ProfileResponse updateProfile(ProfileRequest request);
}
