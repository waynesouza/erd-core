package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.service.WebSocketService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSocketController {

    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @MessageMapping("/send")
    public void handle(DiagramDataRequestDTO diagramDataRequestDto) {
        webSocketService.save(diagramDataRequestDto);
    }

    @GetMapping("/diagram/{projectId}")
    public ResponseEntity<DiagramDataResponseDTO> getDiagramByProjectId(@PathVariable String projectId) {
        return ResponseEntity.ok(webSocketService.getDiagramByProjectId(projectId));
    }

}
