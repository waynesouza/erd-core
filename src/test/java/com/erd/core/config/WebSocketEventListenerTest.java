package com.erd.core.config;

import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.service.CollaborationNotifier;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private CollaborationNotifier collaborationNotifier;

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
        EntityLockDTO users = new EntityLockDTO("users", "ada@erd.com", "ada@erd.com", "Ada", LocalDateTime.now(), "p1");
        EntityLockDTO orders = new EntityLockDTO("orders", "ada@erd.com", "ada@erd.com", "Ada", LocalDateTime.now(), "p1");
        when(collaborationService.clearUserLocks("ada@erd.com")).thenReturn(List.of(users, orders));

        // When
        webSocketEventListener.handleWebSocketDisconnect(disconnectEvent(user));

        // Then - each released lock is announced to the other members of its project
        verify(collaborationNotifier).lockReleased(users);
        verify(collaborationNotifier).lockReleased(orders);
    }

    @Test
    void testHandleWebSocketDisconnect_ignoresAnUnauthenticatedSession() {
        // When
        webSocketEventListener.handleWebSocketDisconnect(disconnectEvent(null));

        // Then
        verify(collaborationService, never()).clearUserLocks(anyString());
    }

}
