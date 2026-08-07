package com.erd.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the directly invokable parts of the {@code @Configuration} classes. The callbacks that
 * Spring drives while building the context ({@code securityFilterChain}, the STOMP registrations)
 * are additionally exercised by the {@code @SpringBootTest} context load.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationBeansTest {

    // ------------------------------------------------------------------
    // ApplicationConfig
    // ------------------------------------------------------------------

    @Test
    void testPasswordEncoder_isBCryptAndProducesVerifiableHashes() {
        // When
        PasswordEncoder encoder = new ApplicationConfig().passwordEncoder();

        // Then
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        String hash = encoder.encode("plain-text");
        assertTrue(encoder.matches("plain-text", hash));
    }

    @Test
    void testAuthenticationProvider_isWiredToTheUserDetailsService(@Mock UserDetailsService userDetailsService) {
        // When
        AuthenticationProvider provider = new ApplicationConfig().authenticationProvider(userDetailsService);

        // Then
        assertNotNull(provider);
    }

    @Test
    void testAuthenticationManager_isTakenFromTheAuthenticationConfiguration(
            @Mock AuthenticationConfiguration authenticationConfiguration,
            @Mock AuthenticationManager authenticationManager) throws Exception {
        // Given
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // When & Then
        assertSame(authenticationManager, new ApplicationConfig().authenticationManager(authenticationConfiguration));
    }

    // ------------------------------------------------------------------
    // MapperConfig
    // ------------------------------------------------------------------

    @Test
    void testModelMapper_beanIsCreated() {
        // When
        ModelMapper modelMapper = new MapperConfig().modelMapper();

        // Then
        assertNotNull(modelMapper);
    }

    // ------------------------------------------------------------------
    // MvcConfig
    // ------------------------------------------------------------------

    @Test
    void testAddCorsMappings_registersTheApiMapping() {
        // Given
        CorsRegistry registry = new CorsRegistry();

        // When
        new MvcConfig().addCorsMappings(registry);

        // Then - CorsRegistry#getCorsConfigurations is protected, so it is read reflectively
        Map<String, CorsConfiguration> registered =
                ReflectionTestUtils.invokeMethod(registry, "getCorsConfigurations");
        assertNotNull(registered);
        CorsConfiguration configuration = registered.get("/api/**");
        assertNotNull(configuration, "The /api/** mapping must be registered");
        assertEquals(List.of("*"), configuration.getAllowedOriginPatterns());
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getExposedHeaders().contains("Set-Cookie"));
    }

    // ------------------------------------------------------------------
    // SecurityConfig
    // ------------------------------------------------------------------

    @Test
    void testCorsConfigurationSource_allowsCredentialsFromAnyOrigin() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null, null);

        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        // Then
        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
        CorsConfiguration configuration =
                ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");
        assertNotNull(configuration);
        assertEquals(List.of("*"), configuration.getAllowedOriginPatterns());
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
        assertTrue(configuration.getExposedHeaders().contains("Authorization"));
    }

    // ------------------------------------------------------------------
    // WebSocketConfig
    // ------------------------------------------------------------------

    @Test
    void testHeartbeatScheduler_isInitialised() {
        // Given
        WebSocketConfig webSocketConfig = new WebSocketConfig(null, null);

        // When
        TaskScheduler scheduler = webSocketConfig.heartbeatScheduler();

        // Then
        assertNotNull(scheduler);
    }

    @Test
    void testRegisterStompEndpoints_exposesTheWsEndpointWithSockJs(
            @Mock(answer = Answers.RETURNS_DEEP_STUBS) StompEndpointRegistry registry,
            @Mock WebSocketAuthInterceptor authInterceptor,
            @Mock JwtHandshakeInterceptor handshakeInterceptor) {
        // Given
        WebSocketConfig webSocketConfig = new WebSocketConfig(authInterceptor, handshakeInterceptor);

        // When
        webSocketConfig.registerStompEndpoints(registry);

        // Then
        verify(registry).addEndpoint("/ws");
    }

    @Test
    void testConfigureMessageBroker_enablesTheSimpleBrokerOnTopic(
            @Mock(answer = Answers.RETURNS_DEEP_STUBS) MessageBrokerRegistry registry) {
        // Given
        WebSocketConfig webSocketConfig = new WebSocketConfig(null, null);

        // When
        webSocketConfig.configureMessageBroker(registry);

        // Then
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).enableSimpleBroker("/topic");
    }

    @Test
    void testConfigureClientInboundChannel_registersTheAuthInterceptor(
            @Mock ChannelRegistration registration,
            @Mock WebSocketAuthInterceptor authInterceptor,
            @Mock JwtHandshakeInterceptor handshakeInterceptor) {
        // Given
        WebSocketConfig webSocketConfig = new WebSocketConfig(authInterceptor, handshakeInterceptor);

        // When
        webSocketConfig.configureClientInboundChannel(registration);

        // Then
        verify(registration).interceptors(authInterceptor);
    }

}
