package com.erd.core.controller;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.service.ProjectService;
import com.erd.core.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;
    private final TeamService teamService;

    public ProjectController(ProjectService projectService, TeamService teamService) {
        this.projectService = projectService;
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(@RequestBody ProjectCreateRequestDTO projectCreateRequestDto) {
        return new ResponseEntity<>(projectService.create(projectCreateRequestDto), HttpStatus.CREATED);
    }

    @GetMapping("/user-email/{email}")
    public ResponseEntity<List<ProjectDetailsResponseDTO>> getProjectsByUserEmail(@PathVariable(name = "email") String email) {
        return new ResponseEntity<>(projectService.getProjectsByUserEmail(email), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurityService.isUserOwnerOrMember(#id.toString(), authentication.name)")
    public ResponseEntity<ProjectDetailsResponseDTO> getProjectDetailsById(@PathVariable(name = "id") UUID id) {
        return new ResponseEntity<>(projectService.getProjectDetailsById(id), HttpStatus.OK);
    }

    @PutMapping
    @PreAuthorize("@projectSecurityService.isProjectOwner(#projectUpdateRequestDto.id.toString(), authentication.name)")
    public ResponseEntity<ProjectResponseDTO> update(@RequestBody ProjectUpdateRequestDTO projectUpdateRequestDto) {
        return new ResponseEntity<>(projectService.update(projectUpdateRequestDto), HttpStatus.OK);
    }

    @PostMapping("/team-member")
    @PreAuthorize("@projectSecurityService.isProjectOwner(#teamMemberRequestDto.projectId.toString(), authentication.name)")
    public ResponseEntity<UserProjectDetailsResponseDTO> addTeamMember(@RequestBody TeamMemberRequestDTO teamMemberRequestDto) {
        UserProjectDetailsResponseDTO newMember = projectService.addTeamMember(teamMemberRequestDto);
        return new ResponseEntity<>(newMember, HttpStatus.CREATED);
    }

    @PutMapping("/team-member")
    @PreAuthorize("@projectSecurityService.isProjectOwner(#requestDTO.projectId.toString(), authentication.name)")
    public ResponseEntity<UserProjectDetailsResponseDTO> updateTeamMember(@RequestBody UpdateTeamMemberRequestDTO requestDTO) {
        UserProjectDetailsResponseDTO updatedMember = projectService.updateTeamMember(requestDTO);
        return new ResponseEntity<>(updatedMember, HttpStatus.OK);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@projectSecurityService.isUserOwnerOrMember(#id.toString(), authentication.name)")
    public ResponseEntity<List<UserProjectDetailsResponseDTO>> getProjectMembers(@PathVariable(name = "id") UUID id) {
        List<UserProjectDetailsResponseDTO> members = teamService.findByProjectId(id);
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @DeleteMapping("/team-member/{memberId}/project/{projectId}")
    @PreAuthorize("@projectSecurityService.isProjectOwner(#projectId.toString(), authentication.name)")
    public ResponseEntity<Void> removeTeamMember(@PathVariable UUID memberId, @PathVariable UUID projectId) {
        projectService.removeTeamMember(memberId, projectId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurityService.isProjectOwner(#id.toString(), authentication.name)")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") UUID id) {
        projectService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
