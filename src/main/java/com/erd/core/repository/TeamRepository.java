package com.erd.core.repository;

import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("SELECT new com.erd.core.dto.response.UserProjectDetailsResponseDTO(t.user.id, t.user.email, t.user.firstName, t.user.lastName, t.role) FROM Team t WHERE t.project.id = :projectId")
    List<UserProjectDetailsResponseDTO> findByProjectId(UUID projectId);

    @Query("SELECT new com.erd.core.dto.response.UserProjectDetailsResponseDTO(t.user.id, t.user.email, t.user.firstName, t.user.lastName, t.role) FROM Team t WHERE t.project.id = :projectId AND t.role = :role")
    List<UserProjectDetailsResponseDTO> findByProjectIdAndRole(@Param("projectId") UUID projectId, @Param("role") RoleProjectEnum role);

    @Query("SELECT new com.erd.core.dto.response.UserProjectDetailsResponseDTO(t.user.id, t.user.email, t.user.firstName, t.user.lastName, t.role) FROM Team t WHERE t.project.id = :projectId AND (LOWER(t.user.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(t.user.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<UserProjectDetailsResponseDTO> findByProjectIdAndNameContaining(@Param("projectId") UUID projectId, @Param("name") String name);

    @Query("SELECT t FROM Team t WHERE t.user.id = :userId AND t.project.id = :projectId")
    Optional<Team> findByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    boolean existsByProjectIdAndRole(UUID projectId, RoleProjectEnum role);

    boolean existsByUserIdAndProjectIdAndRole(UUID userId, UUID projectId, RoleProjectEnum role);

}
