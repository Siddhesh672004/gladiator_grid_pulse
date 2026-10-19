package com.gridpulse.core.account.service;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.account.dto.OtpChallengeResponse;
import com.gridpulse.core.account.dto.SignupRequest;
import com.gridpulse.core.account.dto.UserResponse;

// sign up in two steps (code by email, then the account) and "who is calling?". login is in AuthService
public interface UserService {

    OtpChallengeResponse sendSignupCode(SignupRequest request);

    UserResponse createUser(SignupRequest request);

    void checkNotRegistered(User user);

    User getLoggedInUser();

    void checkOwnerOrAdmin(int ownerUserId);
}
