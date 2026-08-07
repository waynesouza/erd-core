package com.erd.core.mapper;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.model.mongo.Diagram;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses a real Jackson {@link ObjectMapper} so the JSON round-trip is genuinely exercised; the
 * failure paths are triggered with input Jackson cannot process rather than with mocks.
 */
class DiagramMapperTest {

    private DiagramMapper diagramMapper;

    @BeforeEach
    void setUp() {
        diagramMapper = new DiagramMapper(new ObjectMapper());
    }

    private NodeDataDTO node(String key) {
        ItemDTO item = new ItemDTO();
        item.setName("id");
        item.setType("INTEGER");
        item.setPk(true);

        NodeDataDTO nodeData = new NodeDataDTO();
        nodeData.setId(UUID.randomUUID());
        nodeData.setKey(key);
        nodeData.setItems(List.of(item));
        return nodeData;
    }

    private LinkDataDTO link(String from, String to) {
        LinkDataDTO linkData = new LinkDataDTO();
        linkData.setFrom(from);
        linkData.setTo(to);
        linkData.setText("N:1");
        linkData.setToText(1);
        return linkData;
    }

    @Test
    void testToEntity_fromDiagramData_serialisesBothArrays() {
        // Given
        UUID projectId = UUID.randomUUID();
        DiagramDataRequestDTO requestDto = new DiagramDataRequestDTO();
        ReflectionTestUtils.setField(requestDto, "projectId", projectId);
        requestDto.setNodeDataArray(List.of(node("users")));
        requestDto.setLinkDataArray(List.of(link("orders", "users")));

        // When
        Diagram entity = diagramMapper.toEntity(requestDto);

        // Then
        assertEquals(projectId.toString(), entity.getProjectId());
        assertTrue(entity.getNodeData().contains("\"key\":\"users\""));
        assertTrue(entity.getLinkData().contains("\"from\":\"orders\""));
        assertNotNull(entity.getId());
    }

    @Test
    void testToEntity_fromCreateRequest_onlyCarriesTheProjectId() {
        // Given
        CreateDiagramRequestDTO requestDto = new CreateDiagramRequestDTO("project-1");

        // When
        Diagram entity = diagramMapper.toEntity(requestDto);

        // Then
        assertEquals("project-1", entity.getProjectId());
        assertNull(entity.getNodeData());
        assertNull(entity.getLinkData());
    }

    @Test
    void testToResponseDto_deserialisesBothArrays() {
        // Given
        Diagram diagram = new Diagram(
                "[{\"key\":\"users\",\"items\":[{\"name\":\"id\",\"type\":\"INTEGER\",\"pk\":true}]}]",
                "[{\"from\":\"orders\",\"to\":\"users\",\"text\":\"N:1\",\"toText\":1}]",
                "project-1");

        // When
        DiagramDataResponseDTO responseDto = diagramMapper.toResponseDto(diagram);

        // Then
        assertEquals(1, responseDto.getNodeDataArray().size());
        assertEquals("users", responseDto.getNodeDataArray().getFirst().getKey());
        assertEquals(1, responseDto.getLinkDataArray().size());
        assertEquals("orders", responseDto.getLinkDataArray().getFirst().getFrom());
    }

    @Test
    void testToResponseDto_fallsBackToEmptyListsForUnparsableJson() {
        // Given
        Diagram diagram = new Diagram("{not-json", "also-not-json", "project-1");

        // When
        DiagramDataResponseDTO responseDto = diagramMapper.toResponseDto(diagram);

        // Then
        assertNotNull(responseDto.getNodeDataArray());
        assertTrue(responseDto.getNodeDataArray().isEmpty());
        assertNotNull(responseDto.getLinkDataArray());
        assertTrue(responseDto.getLinkDataArray().isEmpty());
    }

    @Test
    void testConvertToSting_serialisesAnObjectToJson() {
        // When
        String json = diagramMapper.convertToSting(List.of(link("orders", "users")));

        // Then
        assertTrue(json.contains("\"to\":\"users\""));
    }

    @Test
    void testConvertToSting_returnsNullWhenSerialisationFails() {
        // Given - a value Jackson cannot serialise
        Object unserialisable = new Object() {
            public Object getSelf() {
                throw new UnsupportedOperationException("boom");
            }
        };

        // When & Then
        assertNull(diagramMapper.convertToSting(unserialisable));
    }

}
