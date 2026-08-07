package com.erd.core.service;

import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.mapper.DiagramMapper;
import com.erd.core.model.mongo.Diagram;
import com.erd.core.repository.mongo.DiagramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagramServiceTest {

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private DiagramMapper diagramMapper;

    @InjectMocks
    private DiagramService diagramService;

    @Test
    void testCreateDiagram_persistsTheMappedEntity() {
        // Given
        CreateDiagramRequestDTO requestDto = new CreateDiagramRequestDTO("project-1");
        Diagram entity = new Diagram();
        when(diagramMapper.toEntity(requestDto)).thenReturn(entity);

        // When
        diagramService.createDiagram(requestDto);

        // Then
        verify(diagramRepository).save(entity);
    }

    @Test
    void testGetDiagramByProjectId_returnsTheMappedResponse() {
        // Given
        Diagram stored = new Diagram("[]", "[]", "project-1");
        DiagramDataResponseDTO responseDto = new DiagramDataResponseDTO();
        when(diagramRepository.findByProjectId("project-1")).thenReturn(Optional.of(stored));
        when(diagramMapper.toResponseDto(stored)).thenReturn(responseDto);
        when(diagramMapper.convertToSting(responseDto)).thenReturn("{}");

        // When
        DiagramDataResponseDTO result = diagramService.getDiagramByProjectId("project-1");

        // Then
        assertSame(responseDto, result);
    }

    @Test
    void testGetDiagramByProjectId_throwsWhenNoDiagramExists() {
        // Given
        when(diagramRepository.findByProjectId("missing")).thenReturn(Optional.empty());

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> diagramService.getDiagramByProjectId("missing"));

        // Then
        assertEquals("Diagram not found for projectId: missing", exception.getMessage());
    }

    @Test
    void testSaveOrUpdateDiagram_updatesTheExistingDocument() {
        // Given
        Diagram existing = new Diagram("old-nodes", "old-links", "project-1");
        when(diagramRepository.findByProjectId("project-1")).thenReturn(Optional.of(existing));

        // When
        diagramService.saveOrUpdateDiagram("project-1", "new-nodes", "new-links");

        // Then
        ArgumentCaptor<Diagram> captor = ArgumentCaptor.forClass(Diagram.class);
        verify(diagramRepository).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertEquals("new-nodes", captor.getValue().getNodeData());
        assertEquals("new-links", captor.getValue().getLinkData());
    }

    @Test
    void testSaveOrUpdateDiagram_createsANewDocumentWhenNoneExists() {
        // Given
        when(diagramRepository.findByProjectId("project-2")).thenReturn(Optional.empty());

        // When
        diagramService.saveOrUpdateDiagram("project-2", "nodes", "links");

        // Then
        ArgumentCaptor<Diagram> captor = ArgumentCaptor.forClass(Diagram.class);
        verify(diagramRepository).save(captor.capture());
        assertNotNull(captor.getValue().getId(), "A brand new Diagram assigns its own identifier");
        assertEquals("project-2", captor.getValue().getProjectId());
        assertEquals("nodes", captor.getValue().getNodeData());
        assertEquals("links", captor.getValue().getLinkData());
    }

    @Test
    void testDeleteDiagramByProjectId_delegatesToTheRepository() {
        // When
        diagramService.deleteDiagramByProjectId("project-1");

        // Then
        verify(diagramRepository).deleteByProjectId("project-1");
    }

    @Test
    void testDeleteDiagramByProjectId_wrapsRepositoryFailures() {
        // Given
        doThrow(new IllegalStateException("mongo down")).when(diagramRepository).deleteByProjectId(any(String.class));

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> diagramService.deleteDiagramByProjectId("project-1"));

        // Then
        assertTrue(exception.getMessage().contains("Failed to delete diagram data for project: project-1"));
        assertNotNull(exception.getCause());
    }

}
