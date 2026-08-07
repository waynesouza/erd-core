package com.erd.core.service;

import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamService teamService;

    @Mock
    private UserService userService;

    @Mock
    private DiagramService diagramService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProjectService projectService;

    private UUID projectId;
    private UUID userId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        project = new Project("Sales ERD", "Model for the sales domain");
        project.setId(projectId);
    }

    private ProjectUpdateRequestDTO updateRequest() {
        ProjectUpdateRequestDTO dto = new ProjectUpdateRequestDTO();
        dto.setId(projectId);
        dto.setName("Renamed");
        dto.setDescription("New description");
        return dto;
    }

    private TeamMemberRequestDTO memberRequest() {
        TeamMemberRequestDTO dto = new TeamMemberRequestDTO();
        dto.setUserEmail("member@erd.com");
        dto.setProjectId(projectId);
        ReflectionTestUtils.setField(dto, "roleProjectEnum", RoleProjectEnum.EDITOR);
        return dto;
    }

    private UpdateTeamMemberRequestDTO updateMemberRequest() {
        UpdateTeamMemberRequestDTO dto = new UpdateTeamMemberRequestDTO();
        dto.setUserId(userId);
        dto.setProjectId(projectId);
        dto.setRole(RoleProjectEnum.VIEWER);
        return dto;
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void testCreate_persistsTheProjectAndRegistersTheOwner() {
        // Given
        ProjectCreateRequestDTO request = new ProjectCreateRequestDTO("Sales ERD", "desc", "owner@erd.com");
        ProjectResponseDTO responseDto = new ProjectResponseDTO();
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(modelMapper.map(project, ProjectResponseDTO.class)).thenReturn(responseDto);

        // When
        ProjectResponseDTO result = projectService.create(request);

        // Then
        assertSame(responseDto, result);
        verify(teamService).create("owner@erd.com", project);
    }

    // ------------------------------------------------------------------
    // getProjectsByUserEmail
    // ------------------------------------------------------------------

    @Test
    void testGetProjectsByUserEmail_returnsTheProjectsWithTheirMembers() {
        // Given
        ProjectDetailsResponseDTO detailsDto =
                new ProjectDetailsResponseDTO(projectId, "Sales ERD", "desc", LocalDateTime.now());
        List<UserProjectDetailsResponseDTO> members = List.of(
                new UserProjectDetailsResponseDTO(userId, "owner@erd.com", "Ada", "Lovelace", RoleProjectEnum.OWNER));
        when(projectRepository.findByUserEmail("owner@erd.com")).thenReturn(List.of(project));
        when(modelMapper.map(project, ProjectDetailsResponseDTO.class)).thenReturn(detailsDto);
        when(teamService.findByProjectId(projectId)).thenReturn(members);

        // When
        List<ProjectDetailsResponseDTO> result = projectService.getProjectsByUserEmail("owner@erd.com");

        // Then
        assertEquals(1, result.size());
        assertEquals(members, result.getFirst().getUsersDto());
    }

    @Test
    void testGetProjectsByUserEmail_returnsAnEmptyListWhenTheUserHasNoProject() {
        // Given
        when(projectRepository.findByUserEmail("nobody@erd.com")).thenReturn(List.of());

        // When & Then
        assertTrue(projectService.getProjectsByUserEmail("nobody@erd.com").isEmpty());
        verify(teamService, never()).findByProjectId(any(UUID.class));
    }

    @Test
    void testGetProjectsByUserEmail_rejectsANullEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> projectService.getProjectsByUserEmail(null));
    }

    @Test
    void testGetProjectsByUserEmail_rejectsABlankEmail() {
        // When
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> projectService.getProjectsByUserEmail("   "));

        // Then
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    // ------------------------------------------------------------------
    // getProjectDetailsById
    // ------------------------------------------------------------------

    @Test
    void testGetProjectDetailsById_returnsTheDetailsWithTheTeam() {
        // Given
        ProjectDetailsResponseDTO detailsDto =
                new ProjectDetailsResponseDTO(projectId, "Sales ERD", "desc", LocalDateTime.now());
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectRepository.findProjectDetailsById(projectId)).thenReturn(detailsDto);
        when(teamService.findByProjectId(projectId)).thenReturn(List.of());

        // When
        ProjectDetailsResponseDTO result = projectService.getProjectDetailsById(projectId);

        // Then
        assertSame(detailsDto, result);
        assertTrue(result.getUsersDto().isEmpty());
    }

    @Test
    void testGetProjectDetailsById_throwsWhenTheProjectIsUnknown() {
        // Given
        when(projectRepository.existsById(projectId)).thenReturn(false);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.getProjectDetailsById(projectId));

        // Then
        assertEquals("Project not found for id: " + projectId, exception.getMessage());
    }

    // ------------------------------------------------------------------
    // update
    // ------------------------------------------------------------------

    @Test
    void testUpdate_savesTheProjectForTheOwner() {
        // Given
        ProjectUpdateRequestDTO request = updateRequest();
        ProjectResponseDTO responseDto = new ProjectResponseDTO();
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);
        when(modelMapper.map(request, Project.class)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(modelMapper.map(project, ProjectResponseDTO.class)).thenReturn(responseDto);

        // When & Then
        assertSame(responseDto, projectService.update(request));
    }

    @Test
    void testUpdate_throwsWhenTheProjectIsUnknown() {
        // Given
        ProjectUpdateRequestDTO request = updateRequest();
        when(projectRepository.existsById(projectId)).thenReturn(false);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> projectService.update(request));

        // Then
        assertEquals("Project not found for id: " + projectId, exception.getMessage());
    }

    @Test
    void testUpdate_throwsWhenTheCallerIsNotTheOwner() {
        // Given
        ProjectUpdateRequestDTO request = updateRequest();
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(false);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> projectService.update(request));

        // Then
        assertEquals("Only the OWNER can update the project", exception.getMessage());
        verify(projectRepository, never()).save(any(Project.class));
    }

    // ------------------------------------------------------------------
    // addTeamMember
    // ------------------------------------------------------------------

    @Test
    void testAddTeamMember_delegatesToTheTeamServiceForTheOwner() {
        // Given
        TeamMemberRequestDTO request = memberRequest();
        UserProjectDetailsResponseDTO added = new UserProjectDetailsResponseDTO(
                userId, "member@erd.com", "Grace", "Hopper", RoleProjectEnum.EDITOR);
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamService.addTeamMember(request, project)).thenReturn(added);

        // When & Then
        assertSame(added, projectService.addTeamMember(request));
    }

    @Test
    void testAddTeamMember_throwsWhenTheCallerIsNotTheOwner() {
        // Given
        TeamMemberRequestDTO request = memberRequest();
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(false);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.addTeamMember(request));

        // Then
        assertEquals("Only the OWNER can add team members to the project", exception.getMessage());
    }

    @Test
    void testAddTeamMember_throwsWhenTheProjectIsUnknown() {
        // Given
        TeamMemberRequestDTO request = memberRequest();
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.addTeamMember(request));

        // Then
        assertEquals("Project not found for id: " + projectId, exception.getMessage());
    }

    // ------------------------------------------------------------------
    // updateTeamMember / removeTeamMember
    // ------------------------------------------------------------------

    @Test
    void testUpdateTeamMember_delegatesToTheTeamServiceForTheOwner() {
        // Given
        UpdateTeamMemberRequestDTO request = updateMemberRequest();
        UserProjectDetailsResponseDTO updated = new UserProjectDetailsResponseDTO(
                userId, "member@erd.com", "Grace", "Hopper", RoleProjectEnum.VIEWER);
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);
        when(teamService.updateTeamMember(request)).thenReturn(updated);

        // When & Then
        assertSame(updated, projectService.updateTeamMember(request));
    }

    @Test
    void testUpdateTeamMember_throwsWhenTheCallerIsNotTheOwner() {
        // Given
        UpdateTeamMemberRequestDTO request = updateMemberRequest();
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(false);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.updateTeamMember(request));

        // Then
        assertEquals("Only the OWNER can update team members", exception.getMessage());
    }

    @Test
    void testRemoveTeamMember_delegatesToTheTeamServiceForTheOwner() {
        // Given
        UUID memberId = UUID.randomUUID();
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);

        // When
        projectService.removeTeamMember(memberId, projectId);

        // Then
        verify(teamService).removeTeamMember(memberId, projectId);
    }

    @Test
    void testRemoveTeamMember_throwsWhenTheCallerIsNotTheOwner() {
        // Given
        UUID memberId = UUID.randomUUID();
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(false);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.removeTeamMember(memberId, projectId));

        // Then
        assertEquals("Only the OWNER can remove team members", exception.getMessage());
        verify(teamService, never()).removeTeamMember(any(UUID.class), any(UUID.class));
    }

    // ------------------------------------------------------------------
    // deleteById
    // ------------------------------------------------------------------

    @Test
    void testDeleteById_removesTheDiagramBeforeTheProject() {
        // Given
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);

        // When
        projectService.deleteById(projectId);

        // Then
        verify(diagramService).deleteDiagramByProjectId(projectId.toString());
        verify(projectRepository).deleteById(projectId);
    }

    @Test
    void testDeleteById_throwsWhenTheCallerIsNotTheOwner() {
        // Given
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(false);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.deleteById(projectId));

        // Then
        assertEquals("Only the OWNER can delete the project", exception.getMessage());
        verify(projectRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testDeleteById_wrapsFailuresFromTheCascadingDeletion() {
        // Given
        when(userService.getUserIdByLoggedUserEmail()).thenReturn(userId);
        when(teamService.isUserOwner(userId, projectId)).thenReturn(true);
        doThrow(new IllegalStateException("mongo down"))
                .when(diagramService).deleteDiagramByProjectId(projectId.toString());

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> projectService.deleteById(projectId));

        // Then
        assertEquals("Failed to delete project and associated data", exception.getMessage());
        verify(projectRepository, never()).deleteById(any(UUID.class));
    }

}
