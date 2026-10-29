package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.request.DiagramPatchRequestDTO;
import com.erd.core.service.ProjectSecurityService;
import com.erd.core.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final WebSocketService webSocketService;
    private final ProjectSecurityService projectSecurityService;

    public WebSocketController(WebSocketService webSocketService, ProjectSecurityService projectSecurityService) {
        this.webSocketService = webSocketService;
        this.projectSecurityService = projectSecurityService;
    }

    @MessageMapping("/send")
    public void handle(DiagramDataRequestDTO diagramDataRequestDto, Principal principal) {
        if (canEdit(diagramDataRequestDto.getProjectId(), principal)) {
            webSocketService.save(diagramDataRequestDto);
        }
    }

    @MessageMapping("/patch")
    public void handlePatch(DiagramPatchRequestDTO diagramPatchRequestDto, Principal principal) {
        if (canEdit(diagramPatchRequestDto.getProjectId(), principal)) {
            webSocketService.applyPatch(diagramPatchRequestDto);
        }
    }

    // STOMP messages bypass @PreAuthorize (no SecurityContext is bound to the message thread), so the
    // same edit rule the REST diagram endpoint uses is checked here against the session principal.
    private boolean canEdit(UUID projectId, Principal principal) {
        if (principal == null || projectId == null
                || !projectSecurityService.canUserEditProject(projectId.toString(), principal.getName())) {
            logger.warn("Diagram update rejected: user is not allowed to edit project {}", projectId);
            return false;
        }
        return true;
    }

}
