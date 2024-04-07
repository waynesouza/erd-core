package com.erd.core.repository;

import com.erd.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    @Query("select u from User u where u.passwordReset.token = :token and u.passwordReset.expiration > :now")
    Optional<User> findByPasswordResetToken(@Param("token") String token, @Param("now") LocalDateTime now);

}
