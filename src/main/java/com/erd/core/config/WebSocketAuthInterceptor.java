package com.erd.core.config;

import com.erd.core.service.JwtService;
import com.erd.core.service.ProjectSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    static final String JWT_TOKEN_ATTR = "jwtToken";

    // The only destinations clients may subscribe to; both are scoped to a single project.
    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/(?:diagram|collaboration)/([^/]+)$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ProjectSecurityService projectSecurityService;

    public WebSocketAuthInterceptor(JwtService jwtService, UserDetailsService userDetailsService,
                                    ProjectSecurityService projectSecurityService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.projectSecurityService = projectSecurityService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateFromHandshake(accessor);
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    // Throwing here makes Spring answer the CONNECT with a STOMP ERROR frame and close the session.
    private void authenticateFromHandshake(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String token = sessionAttributes == null ? null : (String) sessionAttributes.get(JWT_TOKEN_ATTR);

        if (token == null || !jwtService.isTokenValid(token)) {
            logger.warn("WebSocket STOMP CONNECT rejected: no valid JWT token in session");
            throw new AccessDeniedException("A valid JWT token is required to open a WebSocket session");
        }

        String email = jwtService.getEmailFromToken(token);
        var userDetails = userDetailsService.loadUserByUsername(email);
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
        logger.info("WebSocket STOMP CONNECT authenticated for user: {}", email);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        Matcher matcher = PROJECT_TOPIC.matcher(destination == null ? "" : destination);

        if (user == null || !matcher.matches()
                || !projectSecurityService.isUserOwnerOrMember(matcher.group(1), user.getName())) {
            logger.warn("WebSocket SUBSCRIBE to {} rejected", destination);
            throw new AccessDeniedException("Subscription to " + destination + " is not allowed");
        }
    }

}
