package com.erd.core.controller;

import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.service.DdlService;
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
@RequestMapping("/api/ddl")
public class DdlController {

    private final DdlService ddlService;

    public DdlController(DdlService ddlService) {
        this.ddlService = ddlService;
    }

    @PostMapping(value = "/import", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurityService.canUserEditProject(#requestDto.projectId, authentication.name)")
    public ResponseEntity<Void> importDdl(@RequestBody ImportDdlRequestDTO requestDto) {
        try {
            ddlService.importDdl(requestDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/export/{projectId}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurityService.isUserOwnerOrMember(#projectId, authentication.name)")
    public ResponseEntity<ExportDdlResponseDTO> exportDdl(@PathVariable String projectId) {
        try {
            ExportDdlResponseDTO response = ddlService.exportDdl(projectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
