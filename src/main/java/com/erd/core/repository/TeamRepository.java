package com.erd.core.repository;

import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("SELECT new com.erd.core.dto.response.UserProjectDetailsResponseDTO(t.user.id, t.user.email, t.role) FROM Team t WHERE t.project.id = :projectId")
    List<UserProjectDetailsResponseDTO> findByProjectId(UUID projectId);

}
