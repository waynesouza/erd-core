package com.erd.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * This method is used to register STOMP endpoints.
     * STOMP is a simple text-orientated messaging protocol that defines a protocol for client/server communication.
     * It provides an interoperable wire format that allows STOMP clients to talk with any message broker supporting the protocol.
     *
     * @param registry the StompEndpointRegistry that keeps track of all STOMP endpoints.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    /**
     * This method is used to configure the message broker.
     * It sets the application destination prefixes and enables a simple broker.
     * The application destination prefixes are used to define the prefixes for destinations that are mapped to @MessageMapping methods in @Controller classes.
     * The simple broker is used to handle subscriptions and send messages back to the client.
     *
     * @param registry the MessageBrokerRegistry that keeps track of all message brokers.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

}