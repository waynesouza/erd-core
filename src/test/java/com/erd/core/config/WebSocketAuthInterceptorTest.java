package com.erd.core.config;

import com.erd.core.enumeration.RoleEnum;
import com.erd.core.model.User;
import com.erd.core.service.JwtService;
import com.erd.core.service.ProjectSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ProjectSecurityService projectSecurityService;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Ada", "Lovelace", "ada@erd.com", "encoded", RoleEnum.USER);
    }

    private Message<byte[]> stompMessage(StompCommand command, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (authenticated) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor accessorOf(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }

    @Test
    void testPreSend_authenticatesAConnectCarryingAValidToken() {
        // Given
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(WebSocketAuthInterceptor.JWT_TOKEN_ATTR, "valid-token");
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, sessionAttributes);
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn("ada@erd.com");
        when(userDetailsService.loadUserByUsername("ada@erd.com")).thenReturn(user);

        // When
        Message<?> result = webSocketAuthInterceptor.preSend(message, messageChannel);

        // Then
        assertSame(message, result);
        assertNotNull(accessorOf(result).getUser());
        assertEquals(user, ((org.springframework.security.core.Authentication) accessorOf(result).getUser())
                .getPrincipal());
    }

    @Test
    void testPreSend_rejectsAConnectWithAnInvalidToken() {
        // Given
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(WebSocketAuthInterceptor.JWT_TOKEN_ATTR, "bogus-token");
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, sessionAttributes);
        when(jwtService.isTokenValid("bogus-token")).thenReturn(false);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel));
        assertNull(accessorOf(message).getUser());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_rejectsAConnectWithoutAToken() {
        // Given
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, new HashMap<>());

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel));
        assertNull(accessorOf(message).getUser());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_rejectsAConnectWithoutSessionAttributes() {
        // Given
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel));
        assertNull(accessorOf(message).getUser());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_ignoresFramesOtherThanConnect() {
        // Given
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(WebSocketAuthInterceptor.JWT_TOKEN_ATTR, "valid-token");
        Message<byte[]> message = stompMessage(StompCommand.SEND, sessionAttributes);

        // When
        Message<?> result = webSocketAuthInterceptor.preSend(message, messageChannel);

        // Then
        assertSame(message, result);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_allowsAMemberToSubscribeToTheProjectTopics() {
        // Given
        when(projectSecurityService.isUserOwnerOrMember("project-1", "ada@erd.com")).thenReturn(true);
        Message<byte[]> diagram = subscribeMessage("/topic/diagram/project-1", true);
        Message<byte[]> collaboration = subscribeMessage("/topic/collaboration/project-1", true);

        // When & Then
        assertSame(diagram, webSocketAuthInterceptor.preSend(diagram, messageChannel));
        assertSame(collaboration, webSocketAuthInterceptor.preSend(collaboration, messageChannel));
    }

    @Test
    void testPreSend_rejectsASubscriptionFromANonMember() {
        // Given
        when(projectSecurityService.isUserOwnerOrMember("project-1", "ada@erd.com")).thenReturn(false);
        Message<byte[]> message = subscribeMessage("/topic/diagram/project-1", true);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel));
    }

    @Test
    void testPreSend_rejectsASubscriptionWithoutAnAuthenticatedUser() {
        // Given
        Message<byte[]> message = subscribeMessage("/topic/diagram/project-1", false);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel));
        verify(projectSecurityService, never()).isUserOwnerOrMember(anyString(), anyString());
    }

    @Test
    void testPreSend_rejectsASubscriptionToAnUnknownDestination() {
        // Given
        Message<byte[]> unknown = subscribeMessage("/topic/everything", true);
        Message<byte[]> missing = subscribeMessage(null, true);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(unknown, messageChannel));
        assertThrows(AccessDeniedException.class, () -> webSocketAuthInterceptor.preSend(missing, messageChannel));
        verify(projectSecurityService, never()).isUserOwnerOrMember(anyString(), anyString());
    }

    @Test
    void testPreSend_passesThroughAMessageWithoutStompHeaders() {
        // Given
        Message<String> message = MessageBuilder.withPayload("plain").build();

        // When
        Message<?> result = webSocketAuthInterceptor.preSend(message, messageChannel);

        // Then
        assertSame(message, result);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

}
