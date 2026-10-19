package com.gridpulse.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gridpulse.core.model.OtpCode;
import com.gridpulse.core.model.OtpPurpose;

public interface OtpCodeRepo extends JpaRepository<OtpCode, Long> {

    // the newest code is the only one that can still work
    Optional<OtpCode> findTopByEmailAndPurposeOrderByCreatedAtDescIdDesc(String email, OtpPurpose purpose);

    List<OtpCode> findByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);

    // an old reset code can never be used for a new account with the same email
    void deleteByEmail(String email);
}
