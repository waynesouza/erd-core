package com.erd.core.config;

import com.erd.core.service.CollaborationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final CollaborationService collaborationService;

    public WebSocketEventListener(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user != null) {
            String userEmail = user.getName();
            logger.info("WebSocket session disconnected for user: {}", userEmail);
            collaborationService.clearUserLocks(userEmail);
            logger.info("All entity locks released for disconnected user: {}", userEmail);
        } else {
            logger.info("WebSocket session disconnected (unauthenticated session: {})", accessor.getSessionId());
        }
    }

}
