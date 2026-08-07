package com.erd.core.controller;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.service.ProjectService;
import com.erd.core.service.TeamService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProjectService projectService;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private ProjectController projectController;

    private UUID projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
        objectMapper = new ObjectMapper();
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private UserProjectDetailsResponseDTO member(RoleProjectEnum role) {
        return new UserProjectDetailsResponseDTO(userId, "ada@erd.com", "Ada", "Lovelace", role);
    }

    @Test
    void testCreate_returnsCreated() throws Exception {
        // Given
        ProjectResponseDTO responseDto = new ProjectResponseDTO();
        responseDto.setId(projectId);
        responseDto.setName("Sales ERD");
        when(projectService.create(any(ProjectCreateRequestDTO.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sales ERD", "description", "desc", "userEmail", "ada@erd.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sales ERD"));
    }

    @Test
    void testGetProjectsByUserEmail_returnsTheProjectList() throws Exception {
        // Given
        ProjectDetailsResponseDTO detailsDto =
                new ProjectDetailsResponseDTO(projectId, "Sales ERD", "desc", LocalDateTime.now());
        when(projectService.getProjectsByUserEmail("ada@erd.com")).thenReturn(List.of(detailsDto));

        // When & Then
        mockMvc.perform(get("/api/project/user-email/{email}", "ada@erd.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sales ERD"));
    }

    @Test
    void testGetProjectDetailsById_returnsTheDetails() throws Exception {
        // Given
        ProjectDetailsResponseDTO detailsDto =
                new ProjectDetailsResponseDTO(projectId, "Sales ERD", "desc", LocalDateTime.now());
        when(projectService.getProjectDetailsById(projectId)).thenReturn(detailsDto);

        // When & Then
        mockMvc.perform(get("/api/project/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    void testUpdate_returnsOk() throws Exception {
        // Given
        ProjectResponseDTO responseDto = new ProjectResponseDTO();
        responseDto.setId(projectId);
        responseDto.setName("Renamed");
        when(projectService.update(any(ProjectUpdateRequestDTO.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(put("/api/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", projectId.toString(), "name", "Renamed", "description", "desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void testAddTeamMember_returnsCreated() throws Exception {
        // Given
        when(projectService.addTeamMember(any(TeamMemberRequestDTO.class)))
                .thenReturn(member(RoleProjectEnum.EDITOR));

        // When & Then
        mockMvc.perform(post("/api/project/team-member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userEmail", "ada@erd.com",
                                "projectId", projectId.toString(),
                                "roleProjectEnum", "EDITOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void testUpdateTeamMember_returnsOk() throws Exception {
        // Given
        when(projectService.updateTeamMember(any(UpdateTeamMemberRequestDTO.class)))
                .thenReturn(member(RoleProjectEnum.VIEWER));

        // When & Then
        mockMvc.perform(put("/api/project/team-member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId.toString(),
                                "projectId", projectId.toString(),
                                "role", "VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void testGetProjectMembers_returnsTheTeam() throws Exception {
        // Given
        when(teamService.findByProjectId(projectId)).thenReturn(List.of(member(RoleProjectEnum.OWNER)));

        // When & Then
        mockMvc.perform(get("/api/project/{id}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ada@erd.com"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void testRemoveTeamMember_returnsNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/project/team-member/{memberId}/project/{projectId}", userId, projectId))
                .andExpect(status().isNoContent());

        verify(projectService).removeTeamMember(userId, projectId);
    }

    @Test
    void testDeleteById_returnsNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/project/{id}", projectId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteById(projectId);
    }

}
