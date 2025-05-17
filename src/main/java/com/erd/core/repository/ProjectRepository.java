package com.erd.core.repository;

import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT p FROM Project p JOIN p.teams t WHERE t.user.email = :email")
    List<Project> findByUserEmail(String email);

    @Query("SELECT new com.erd.core.dto.response.ProjectDetailsResponseDTO(p.id, p.name, p.description, p.createdAt) FROM Project p WHERE p.id = :id")
    ProjectDetailsResponseDTO findProjectDetailsById(UUID id);

}
