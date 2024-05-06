package com.erd.core.repository;

import com.erd.core.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> { }
