package com.erd.core.controller;

import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.service.DdlService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DdlControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DdlService ddlService;

    @InjectMocks
    private DdlController ddlController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ddlController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testImportDdl_Success() throws Exception {
        // Given
        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent("CREATE TABLE users (id INT PRIMARY KEY);");

        // When & Then
        mockMvc.perform(post("/api/ddl/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(ddlService).importDdl(any(ImportDdlRequestDTO.class));
    }

    @Test
    void testImportDdl_ServiceThrowsException() throws Exception {
        // Given
        ImportDdlRequestDTO requestDto = new ImportDdlRequestDTO();
        requestDto.setProjectId("test-project");
        requestDto.setDdlContent("INVALID DDL");

        doThrow(new RuntimeException("Import failed")).when(ddlService).importDdl(any(ImportDdlRequestDTO.class));

        // When & Then
        mockMvc.perform(post("/api/ddl/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExportDdl_Success() throws Exception {
        // Given
        String projectId = "test-project";
        String expectedDdl = "CREATE TABLE users (id INT PRIMARY KEY);";
        
        ExportDdlResponseDTO responseDto = new ExportDdlResponseDTO();
        responseDto.setProjectId(projectId);
        responseDto.setDdlContent(expectedDdl);

        when(ddlService.exportDdl(eq(projectId))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/ddl/export/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.ddlContent").value(expectedDdl));

        verify(ddlService).exportDdl(eq(projectId));
    }

    @Test
    void testExportDdl_ProjectNotFound() throws Exception {
        // Given
        String projectId = "non-existent-project";

        when(ddlService.exportDdl(eq(projectId)))
                .thenThrow(new RuntimeException("Project not found"));

        // When & Then
        mockMvc.perform(get("/api/ddl/export/{projectId}", projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testImportDdl_InvalidRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/ddl/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testImportDdl_EmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/ddl/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        verify(ddlService).importDdl(any(ImportDdlRequestDTO.class));
    }
} 