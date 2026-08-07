package com.erd.core.controller;

import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.service.DiagramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiagramControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DiagramService diagramService;

    @InjectMocks
    private DiagramController diagramController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(diagramController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreate_returnsOk() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("projectId", "project-1"))))
                .andExpect(status().isOk());

        verify(diagramService).createDiagram(any(CreateDiagramRequestDTO.class));
    }

    @Test
    void testGetDiagramByProjectId_returnsTheDiagram() throws Exception {
        // Given
        DiagramDataResponseDTO responseDto = new DiagramDataResponseDTO(List.of(), List.of());
        responseDto.setProjectId("project-1");
        when(diagramService.getDiagramByProjectId("project-1")).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/diagram/{projectId}", "project-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("project-1"))
                .andExpect(jsonPath("$.nodeDataArray").isArray());
    }

    @Test
    void testGetDiagramByProjectId_returnsNotFoundWhenTheServiceFails() throws Exception {
        // Given
        when(diagramService.getDiagramByProjectId("missing"))
                .thenThrow(new RuntimeException("Diagram not found for projectId: missing"));

        // When & Then
        mockMvc.perform(get("/api/diagram/{projectId}", "missing"))
                .andExpect(status().isNotFound());
    }

}
