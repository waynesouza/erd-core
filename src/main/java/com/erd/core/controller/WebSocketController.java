package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final WebSocketService webSocketService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(WebSocketService webSocketService, SimpMessagingTemplate messagingTemplate) {
        this.webSocketService = webSocketService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/send")
    public void handle(DiagramDataRequestDTO diagramDataRequestDto) {
        logger.info("Received message: {}", diagramDataRequestDto);
        DiagramDataResponseDTO savedDto = webSocketService.save(diagramDataRequestDto);
        messagingTemplate.convertAndSend("/topic/receive", savedDto);
    }

}
