package com.erd.core.service;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private DiagramMapper diagramMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    private UUID projectId;
    private DiagramDataRequestDTO requestDto;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        requestDto = new DiagramDataRequestDTO();
        ReflectionTestUtils.setField(requestDto, "projectId", projectId);
        requestDto.setNodeDataArray(List.of(new NodeDataDTO()));
        requestDto.setLinkDataArray(List.of(new LinkDataDTO()));
    }

    @Test
    void testSave_updatesAnExistingDiagramAndBroadcastsIt() {
        // Given
        Diagram existing = new Diagram("old-nodes", "old-links", projectId.toString());
        DiagramDataResponseDTO responseDto = new DiagramDataResponseDTO();
        responseDto.setProjectId(projectId.toString());
        when(diagramRepository.findByProjectId(projectId.toString())).thenReturn(Optional.of(existing));
        when(diagramMapper.convertToSting(requestDto.getNodeDataArray())).thenReturn("new-nodes");
        when(diagramMapper.convertToSting(requestDto.getLinkDataArray())).thenReturn("new-links");
        when(diagramRepository.save(existing)).thenReturn(existing);
        when(diagramMapper.toResponseDto(existing)).thenReturn(responseDto);

        // When
        webSocketService.save(requestDto);

        // Then
        assertEquals("new-nodes", existing.getNodeData());
        assertEquals("new-links", existing.getLinkData());
        verify(messagingTemplate).convertAndSend("/topic/diagram/" + projectId, responseDto);
    }

    @Test
    void testSave_createsANewDiagramWhenNoneExists() {
        // Given
        Diagram mapped = new Diagram("nodes", "links", projectId.toString());
        DiagramDataResponseDTO responseDto = new DiagramDataResponseDTO();
        when(diagramRepository.findByProjectId(projectId.toString())).thenReturn(Optional.empty());
        when(diagramMapper.toEntity(requestDto)).thenReturn(mapped);
        when(diagramRepository.save(mapped)).thenReturn(mapped);
        when(diagramMapper.toResponseDto(mapped)).thenReturn(responseDto);

        // When
        webSocketService.save(requestDto);

        // Then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(anyString(), payloadCaptor.capture());
        assertSame(responseDto, payloadCaptor.getValue());
        verify(diagramMapper, never()).convertToSting(any());
    }

    @Test
    void testSave_swallowsPersistenceFailures() {
        // Given
        when(diagramRepository.findByProjectId(projectId.toString()))
                .thenThrow(new IllegalStateException("mongo down"));

        // When - the STOMP handler must not propagate the failure to the broker
        webSocketService.save(requestDto);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testSave_swallowsBroadcastFailures() {
        // Given
        Diagram mapped = new Diagram("nodes", "links", projectId.toString());
        DiagramDataResponseDTO responseDto = new DiagramDataResponseDTO();
        when(diagramRepository.findByProjectId(projectId.toString())).thenReturn(Optional.empty());
        when(diagramMapper.toEntity(requestDto)).thenReturn(mapped);
        when(diagramRepository.save(mapped)).thenReturn(mapped);
        when(diagramMapper.toResponseDto(mapped)).thenReturn(responseDto);
        doThrow(new MessagingException("broker unavailable"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When - must not throw
        webSocketService.save(requestDto);

        // Then
        verify(diagramRepository).save(mapped);
    }

}
