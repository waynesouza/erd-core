package com.erd.core.controller;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(@RequestBody ProjectCreateRequestDTO projectCreateRequestDto) {
        return new ResponseEntity<>(projectService.create(projectCreateRequestDto), HttpStatus.CREATED);
    }

    @GetMapping("/user-email/{email}")
    public ResponseEntity<List<ProjectResponseDTO>> getProjectsByUserEmail(@PathVariable(name = "email") String email) {
        return new ResponseEntity<>(projectService.getProjectsByUserEmail(email), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailsResponseDTO> getProjectDetailsById(@PathVariable(name = "id") UUID id) {
        return new ResponseEntity<>(projectService.getProjectDetailsById(id), HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<ProjectResponseDTO> update(@RequestBody ProjectUpdateRequestDTO projectUpdateRequestDto, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return new ResponseEntity<>(projectService.update(projectUpdateRequestDto, userId), HttpStatus.OK);
    }

    @PostMapping("/team-member")
    public ResponseEntity<Void> addTeamMember(@RequestBody TeamMemberRequestDTO teamMemberRequestDto, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        projectService.addTeamMember(teamMemberRequestDto, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/team-member")
    public ResponseEntity<Void> updateTeamMember(@RequestBody UpdateTeamMemberRequestDTO requestDTO, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        projectService.updateTeamMember(requestDTO, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/team-member/{memberId}/project/{projectId}")
    public ResponseEntity<Void> removeTeamMember(@PathVariable UUID memberId, @PathVariable UUID projectId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        projectService.removeTeamMember(memberId, projectId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        projectService.deleteById(id, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
