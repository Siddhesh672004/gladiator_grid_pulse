package com.gridpulse.core.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.gridpulse.common.paging.PageResponse;
import com.gridpulse.core.dto.response.AdminUserResponse;
import com.gridpulse.core.exceptions.ConflictException;
import com.gridpulse.core.exceptions.ResourceNotFoundException;
import com.gridpulse.core.mapper.UserMapper;
import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.User;
import com.gridpulse.core.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// a deactivated driver can't log in (AuthServiceImpl) and old tokens stop working (JwtAuthenticationFilter)
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Map<String, Boolean> STATUS_FILTERS = Map.of("Active", true, "Inactive", false);

    private final UserRepo userRepo;
    private final UserMapper userMapper;

    @Override
    public PageResponse<AdminUserResponse> getUsers(String search, String status, int page, int size) {
        // pages start at 0, a page has 1 to 50 rows
        Optional.of(size)
                .filter(rows -> page >= 0 && rows >= 1 && rows <= MAX_PAGE_SIZE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "userId"));
        // no search text = every driver
        String pattern = "%" + Optional.ofNullable(search).map(String::trim).map(String::toLowerCase).orElse("") + "%";
        Page<User> users = userRepo.search(Role.USER, enabledFilter(status), pattern, pageable);
        return new PageResponse<>(users.map(userMapper::toAdminUserResponse).getContent(), users.getTotalElements(),
                page, size);
    }

    @Override
    public AdminUserResponse setUserEnabled(int userId, boolean enabled) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID: " + userId + " not found."));
        // drivers only, and only a real change
        Optional.of(user)
                .filter(account -> account.getUserRole() == Role.USER)
                .orElseThrow(() -> new AccessDeniedException("Admin accounts are managed by the Super Admin"));
        Optional.of(user)
                .filter(account -> account.isEnabled() != enabled)
                .orElseThrow(() -> new ConflictException(
                        "This account is already " + (enabled ? "active" : "inactive")));
        user.setEnabled(enabled);
        User saved = userRepo.save(user);
        log.info("Driver {} is now {}", saved.getEmail(), enabled ? "active" : "inactive");
        return userMapper.toAdminUserResponse(saved);
    }

    // nothing or "All" = no filter (null), "Active" = true, "Inactive" = false
    private Boolean enabledFilter(String status) {
        return Optional.ofNullable(status)
                .filter(text -> !text.isBlank() && !text.equalsIgnoreCase("All"))
                .map(this::enabledValue)
                .orElse(null);
    }

    private Boolean enabledValue(String status) {
        return STATUS_FILTERS.entrySet().stream()
                .filter(choice -> choice.getKey().equalsIgnoreCase(status))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Status must be Active, Inactive or All"));
    }
}
