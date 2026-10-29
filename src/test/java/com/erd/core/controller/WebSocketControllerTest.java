package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.request.DiagramPatchRequestDTO;
import com.erd.core.service.ProjectSecurityService;
import com.erd.core.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WebSocketController} handles a STOMP message rather than an HTTP request, so it is invoked
 * directly instead of through MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final Principal ADA = new TestingAuthenticationToken("ada@erd.com", null);

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private ProjectSecurityService projectSecurityService;

    @InjectMocks
    private WebSocketController webSocketController;

    // DiagramDataRequestDTO has no projectId setter; Jackson fills it the same way the STOMP converter does.
    private DiagramDataRequestDTO requestFor(UUID projectId) {
        return new ObjectMapper().convertValue(
                projectId == null ? Map.of() : Map.of("projectId", projectId.toString()), DiagramDataRequestDTO.class);
    }

    @Test
    void testHandle_forwardsThePayloadWhenTheSenderCanEdit() {
        // Given
        DiagramDataRequestDTO requestDto = requestFor(PROJECT_ID);
        when(projectSecurityService.canUserEditProject(PROJECT_ID.toString(), "ada@erd.com")).thenReturn(true);

        // When
        webSocketController.handle(requestDto, ADA);

        // Then
        verify(webSocketService).save(requestDto);
    }

    @Test
    void testHandle_dropsThePayloadWhenTheSenderCannotEdit() {
        // Given - a viewer, or a user outside the project
        DiagramDataRequestDTO requestDto = requestFor(PROJECT_ID);
        when(projectSecurityService.canUserEditProject(PROJECT_ID.toString(), "ada@erd.com")).thenReturn(false);

        // When
        webSocketController.handle(requestDto, ADA);

        // Then
        verify(webSocketService, never()).save(any());
    }

    @Test
    void testHandle_dropsThePayloadOfAnUnauthenticatedSession() {
        // When
        webSocketController.handle(requestFor(PROJECT_ID), null);

        // Then
        verify(projectSecurityService, never()).canUserEditProject(anyString(), anyString());
        verify(webSocketService, never()).save(any());
    }

    @Test
    void testHandlePatch_appliesThePatchWhenTheSenderCanEdit() {
        // Given
        DiagramPatchRequestDTO patch = new DiagramPatchRequestDTO();
        patch.setProjectId(PROJECT_ID);
        when(projectSecurityService.canUserEditProject(PROJECT_ID.toString(), "ada@erd.com")).thenReturn(true);

        // When
        webSocketController.handlePatch(patch, ADA);

        // Then
        verify(webSocketService).applyPatch(patch);
    }

    @Test
    void testHandlePatch_dropsThePatchWhenTheSenderCannotEdit() {
        // Given
        DiagramPatchRequestDTO patch = new DiagramPatchRequestDTO();
        patch.setProjectId(PROJECT_ID);
        when(projectSecurityService.canUserEditProject(PROJECT_ID.toString(), "ada@erd.com")).thenReturn(false);

        // When
        webSocketController.handlePatch(patch, ADA);

        // Then
        verify(webSocketService, never()).applyPatch(any());
    }

    @Test
    void testHandle_dropsAPayloadWithoutAProject() {
        // When
        webSocketController.handle(requestFor(null), ADA);

        // Then
        verify(projectSecurityService, never()).canUserEditProject(anyString(), anyString());
        verify(webSocketService, never()).save(any());
    }

}
