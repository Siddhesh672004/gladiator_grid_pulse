package com.gridpulse.core.service;

import com.gridpulse.core.dto.request.SignupRequest;
import com.gridpulse.core.dto.response.OtpChallengeResponse;
import com.gridpulse.core.dto.response.UserResponse;
import com.gridpulse.core.model.User;

// sign up in two steps (code by email, then the account) and "who is calling?". login is in AuthService
public interface UserService {

    OtpChallengeResponse sendSignupCode(SignupRequest request);

    UserResponse createUser(SignupRequest request);

    void checkNotRegistered(User user);

    User getLoggedInUser();

    void checkOwnerOrAdmin(int ownerUserId);
}
