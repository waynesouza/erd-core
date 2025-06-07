package com.erd.core.controller;

import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.service.DiagramService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/api/diagram")
public class DiagramController {

    private final DiagramService diagramService;

    public DiagramController(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurityService.canUserEditProject(#requestDto.projectId, authentication.name)")
    public ResponseEntity<Void> create(@RequestBody CreateDiagramRequestDTO requestDto) {
        diagramService.createDiagram(requestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{projectId}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurityService.isUserOwnerOrMember(#projectId, authentication.name)")
    public ResponseEntity<DiagramDataResponseDTO> getDiagramByProjectId(@PathVariable String projectId) {
        try {
            return ResponseEntity.ok(diagramService.getDiagramByProjectId(projectId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
