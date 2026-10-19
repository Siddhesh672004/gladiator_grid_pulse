package com.gridpulse.core.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gridpulse.core.model.Role;
import com.gridpulse.core.model.User;

public interface UserRepo extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByDriverLicenseNumber(String driverLicenseNumber);

    List<User> findByUserRole(Role userRole);

    // who gets the urgent feedback emails
    List<User> findByUserRoleInAndEnabledTrue(Collection<Role> userRoles);

    // newest account first (the Admins page)
    List<User> findByUserRoleOrderByUserIdDesc(Role userRole);

    long countByUserRole(Role userRole);

    long countByUserRoleAndEnabled(Role userRole, boolean enabled);

    // a driver who can book: switched on AND date of birth + license on file
    long countByUserRoleAndEnabledTrueAndDateOfBirthIsNotNullAndDriverLicenseNumberIsNotNull(Role userRole);

    // Manage Users: ?2 = true / false for active / inactive, null = both
    // ?3 = the search text as "%text%", matched on name or email in lower case
    @Query("select u from User u where u.userRole = ?1 and (?2 is null or u.enabled = ?2)"
            + " and (lower(u.username) like ?3 or lower(u.email) like ?3)")
    Page<User> search(Role userRole, Boolean enabled, String pattern, Pageable pageable);
}
