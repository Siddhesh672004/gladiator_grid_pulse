package com.gridpulse.core.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.User;

import lombok.Getter;

// our User wrapped in the format spring security understands
@Getter
public class UserPrinciple implements UserDetails {

    private final int userId;
    private final String email;
    private final String password;
    private final Role userRole;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public UserPrinciple(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.userRole = user.getUserRole();
        this.enabled = user.isEnabled();
        // hasRole('Admin') looks for "ROLE_Admin"
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + userRole.getLabel()));
    }

    public boolean isAdmin() {
        return userRole.isAdmin();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // false = account disabled -> JwtAuthenticationFilter ignores the token
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
