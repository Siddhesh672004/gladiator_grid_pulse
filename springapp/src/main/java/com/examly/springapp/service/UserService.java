package com.examly.springapp.service;

import com.examly.springapp.dto.request.SignupRequest;
import com.examly.springapp.dto.response.OtpChallengeResponse;
import com.examly.springapp.dto.response.UserResponse;
import com.examly.springapp.model.User;

// sign up in two steps (code by email, then the account) and "who is calling?". login is in AuthService
public interface UserService {

    OtpChallengeResponse sendSignupCode(SignupRequest request);

    UserResponse createUser(SignupRequest request);

    void checkNotRegistered(User user);

    User getLoggedInUser();

    void checkOwnerOrAdmin(int ownerUserId);
}
