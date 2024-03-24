package com.erd.core.controller;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PutMapping
    public ResponseEntity<ProjectResponseDTO> update(ProjectUpdateRequestDTO projectUpdateRequestDto) {
        return new ResponseEntity<>(projectService.update(projectUpdateRequestDto), HttpStatus.OK);
    }

    @PostMapping("/team-member")
    public ResponseEntity<Void> addTeamMember(TeamMemberRequestDTO teamMemberRequestDto) {
        projectService.addTeamMember(teamMemberRequestDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") UUID id) {
        projectService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
