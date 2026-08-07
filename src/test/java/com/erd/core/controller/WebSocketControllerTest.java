package com.erd.core.controller;

import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.service.WebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * {@link WebSocketController} handles a STOMP message rather than an HTTP request, so it is invoked
 * directly instead of through MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private WebSocketController webSocketController;

    @Test
    void testHandle_forwardsThePayloadToTheService() {
        // Given
        DiagramDataRequestDTO requestDto = new DiagramDataRequestDTO();

        // When
        webSocketController.handle(requestDto);

        // Then
        verify(webSocketService).save(requestDto);
    }

}
