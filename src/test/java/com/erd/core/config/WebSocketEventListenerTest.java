package com.erd.core.config;

import com.erd.core.service.CollaborationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private CollaborationService collaborationService;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    private SessionDisconnectEvent disconnectEvent(Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("session-1");
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, "session-1", CloseStatus.NORMAL);
    }

    @Test
    void testHandleWebSocketDisconnect_releasesTheLocksOfTheDisconnectedUser() {
        // Given
        Principal user = new UsernamePasswordAuthenticationToken("ada@erd.com", null, List.of());

        // When
        webSocketEventListener.handleWebSocketDisconnect(disconnectEvent(user));

        // Then
        verify(collaborationService).clearUserLocks("ada@erd.com");
    }

    @Test
    void testHandleWebSocketDisconnect_ignoresAnUnauthenticatedSession() {
        // When
        webSocketEventListener.handleWebSocketDisconnect(disconnectEvent(null));

        // Then
        verify(collaborationService, never()).clearUserLocks(anyString());
    }

}
