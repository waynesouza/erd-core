package com.erd.core.controller;

import com.erd.core.dto.error.ErrorMessageDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.exception.DdlImportException;
import com.erd.core.service.DdlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@RequestMapping("/api/ddl")
public class DdlController {

    private final DdlService ddlService;

    public DdlController(DdlService ddlService) {
        this.ddlService = ddlService;
    }

    @PostMapping(value = "/import", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurityService.canUserEditProject(#requestDto.projectId, authentication.name)")
    public ResponseEntity<Object> importDdl(@RequestBody ImportDdlRequestDTO requestDto) {
        try {
            // The body lists what the script could not represent, so the user knows what was left out.
            return ResponseEntity.ok(ddlService.importDdl(requestDto));
        } catch (DdlImportException e) {
            // The reason is meant for the user, so it goes back in the body.
            return ResponseEntity.badRequest().body(new ErrorMessageDTO(
                    HttpStatus.BAD_REQUEST.value(), LocalDateTime.now(), e.getMessage(), "uri=/api/ddl/import"));
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
