package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.service.WebSocketService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WebSocketController {

    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @MessageMapping("/send")
    public void handle(DiagramDataRequestDTO diagramDataRequestDto) {
        webSocketService.save(diagramDataRequestDto);
    }

}
