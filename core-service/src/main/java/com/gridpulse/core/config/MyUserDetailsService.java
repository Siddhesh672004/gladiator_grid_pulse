package com.gridpulse.core.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gridpulse.core.repository.UserRepo;
import com.gridpulse.core.service.AuthServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// spring security loads the user from our db through this (login + every request with a token)
@Slf4j
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.trace("loading user details for {}", email);
        return userRepo.findByEmailIgnoreCase(email)
                .map(UserPrinciple::new)
                .orElseThrow(() -> new UsernameNotFoundException(AuthServiceImpl.WRONG_LOGIN));
    }
}
