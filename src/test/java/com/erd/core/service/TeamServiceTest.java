package com.erd.core.service;

import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TeamService teamService;

    private User user;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        user = new User("Ada", "Lovelace", "ada@erd.com", "encoded", null);
        user.setId(UUID.randomUUID());

        projectId = UUID.randomUUID();
        project = new Project("Sales ERD", "Model for the sales domain");
        project.setId(projectId);
    }

    private TeamMemberRequestDTO memberRequest(RoleProjectEnum role) {
        TeamMemberRequestDTO dto = new TeamMemberRequestDTO();
        dto.setUserEmail("ada@erd.com");
        dto.setProjectId(projectId);
        // The DTO exposes no setter for the role - it is populated by Jackson in production.
        ReflectionTestUtils.setField(dto, "roleProjectEnum", role);
        return dto;
    }

    private UpdateTeamMemberRequestDTO updateRequest(RoleProjectEnum role) {
        UpdateTeamMemberRequestDTO dto = new UpdateTeamMemberRequestDTO();
        dto.setUserId(user.getId());
        dto.setProjectId(projectId);
        dto.setRole(role);
        return dto;
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void testCreate_registersTheProjectCreatorAsOwner() {
        // Given
        when(userService.findByEmail("ada@erd.com")).thenReturn(user);

        // When
        teamService.create("ada@erd.com", project);

        // Then
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertSame(project, captor.getValue().getProject());
        assertEquals(RoleProjectEnum.OWNER, captor.getValue().getRole());
    }

    // ------------------------------------------------------------------
    // addTeamMember
    // ------------------------------------------------------------------

    @Test
    void testAddTeamMember_savesTheMemberAndReturnsItsDetails() {
        // Given
        TeamMemberRequestDTO request = memberRequest(RoleProjectEnum.EDITOR);
        when(userService.findByEmail("ada@erd.com")).thenReturn(user);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserProjectDetailsResponseDTO response = teamService.addTeamMember(request, project);

        // Then
        assertEquals(user.getId(), response.getId());
        assertEquals("ada@erd.com", response.getEmail());
        assertEquals("Ada", response.getFirstName());
        assertEquals("Lovelace", response.getLastName());
        assertEquals(RoleProjectEnum.EDITOR, response.getRole());
    }

    @Test
    void testAddTeamMember_acceptsAnOwnerWhenTheProjectHasNone() {
        // Given
        TeamMemberRequestDTO request = memberRequest(RoleProjectEnum.OWNER);
        when(teamRepository.existsByProjectIdAndRole(projectId, RoleProjectEnum.OWNER)).thenReturn(false);
        when(userService.findByEmail("ada@erd.com")).thenReturn(user);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserProjectDetailsResponseDTO response = teamService.addTeamMember(request, project);

        // Then
        assertEquals(RoleProjectEnum.OWNER, response.getRole());
    }

    @Test
    void testAddTeamMember_rejectsASecondOwner() {
        // Given
        TeamMemberRequestDTO request = memberRequest(RoleProjectEnum.OWNER);
        when(teamRepository.existsByProjectIdAndRole(projectId, RoleProjectEnum.OWNER)).thenReturn(true);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> teamService.addTeamMember(request, project));

        // Then
        assertEquals("There is already an OWNER for this project", exception.getMessage());
        verify(teamRepository, never()).save(any(Team.class));
    }

    // ------------------------------------------------------------------
    // updateTeamMember
    // ------------------------------------------------------------------

    @Test
    void testUpdateTeamMember_changesTheRole() {
        // Given
        UpdateTeamMemberRequestDTO request = updateRequest(RoleProjectEnum.VIEWER);
        Team team = new Team(user, project, RoleProjectEnum.EDITOR);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));
        when(teamRepository.save(team)).thenReturn(team);

        // When
        UserProjectDetailsResponseDTO response = teamService.updateTeamMember(request);

        // Then
        assertEquals(RoleProjectEnum.VIEWER, response.getRole());
        assertEquals(user.getId(), response.getId());
    }

    @Test
    void testUpdateTeamMember_promotesToOwnerWhenNoOwnerExists() {
        // Given
        UpdateTeamMemberRequestDTO request = updateRequest(RoleProjectEnum.OWNER);
        Team team = new Team(user, project, RoleProjectEnum.EDITOR);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByProjectIdAndRole(projectId, RoleProjectEnum.OWNER)).thenReturn(false);
        when(teamRepository.save(team)).thenReturn(team);

        // When
        UserProjectDetailsResponseDTO response = teamService.updateTeamMember(request);

        // Then
        assertEquals(RoleProjectEnum.OWNER, response.getRole());
    }

    @Test
    void testUpdateTeamMember_rejectsASecondOwner() {
        // Given
        UpdateTeamMemberRequestDTO request = updateRequest(RoleProjectEnum.OWNER);
        Team team = new Team(user, project, RoleProjectEnum.EDITOR);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByProjectIdAndRole(projectId, RoleProjectEnum.OWNER)).thenReturn(true);

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> teamService.updateTeamMember(request));

        // Then
        assertEquals("There is already an OWNER for this project", exception.getMessage());
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void testUpdateTeamMember_throwsWhenTheMemberDoesNotExist() {
        // Given
        UpdateTeamMemberRequestDTO request = updateRequest(RoleProjectEnum.VIEWER);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.empty());

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> teamService.updateTeamMember(request));

        // Then
        assertEquals("Member not found", exception.getMessage());
    }

    // ------------------------------------------------------------------
    // removeTeamMember
    // ------------------------------------------------------------------

    @Test
    void testRemoveTeamMember_deletesANonOwnerMembership() {
        // Given
        Team team = new Team(user, project, RoleProjectEnum.EDITOR);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));

        // When
        teamService.removeTeamMember(user.getId(), projectId);

        // Then
        verify(teamRepository).delete(team);
    }

    @Test
    void testRemoveTeamMember_refusesToRemoveTheOwner() {
        // Given
        Team team = new Team(user, project, RoleProjectEnum.OWNER);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> teamService.removeTeamMember(user.getId(), projectId));

        // Then
        assertEquals("It is not possible to remove the OWNER of the project", exception.getMessage());
        verify(teamRepository, never()).delete(any(Team.class));
    }

    @Test
    void testRemoveTeamMember_throwsWhenTheMemberDoesNotExist() {
        // Given
        UUID userId = user.getId();
        when(teamRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> teamService.removeTeamMember(userId, projectId));

        // Then
        assertEquals("Member not found", exception.getMessage());
    }

    // ------------------------------------------------------------------
    // queries
    // ------------------------------------------------------------------

    @Test
    void testFindByProjectId_delegatesToTheRepository() {
        // Given
        List<UserProjectDetailsResponseDTO> members = List.of(
                new UserProjectDetailsResponseDTO(user.getId(), "ada@erd.com", "Ada", "Lovelace", RoleProjectEnum.OWNER));
        when(teamRepository.findByProjectId(projectId)).thenReturn(members);

        // When & Then
        assertEquals(members, teamService.findByProjectId(projectId));
    }

    @Test
    void testFindByProjectIdAndRole_delegatesToTheRepository() {
        // Given
        when(teamRepository.findByProjectIdAndRole(projectId, RoleProjectEnum.VIEWER)).thenReturn(List.of());

        // When & Then
        assertTrue(teamService.findByProjectIdAndRole(projectId, RoleProjectEnum.VIEWER).isEmpty());
    }

    @Test
    void testFindByProjectIdAndNameContaining_delegatesToTheRepository() {
        // Given
        when(teamRepository.findByProjectIdAndNameContaining(projectId, "ada")).thenReturn(List.of());

        // When & Then
        assertTrue(teamService.findByProjectIdAndNameContaining(projectId, "ada").isEmpty());
    }

    @Test
    void testIsUserOwner_reflectsTheRepositoryAnswer() {
        // Given
        when(teamRepository.existsByUserIdAndProjectIdAndRole(user.getId(), projectId, RoleProjectEnum.OWNER))
                .thenReturn(true);

        // When & Then
        assertTrue(teamService.isUserOwner(user.getId(), projectId));
    }

    @Test
    void testCanUserEdit_allowsEditorsAndOwners() {
        // Given
        Team team = new Team(user, project, RoleProjectEnum.EDITOR);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));

        // When & Then
        assertTrue(teamService.canUserEdit(user.getId(), projectId));
    }

    @Test
    void testCanUserEdit_deniesViewers() {
        // Given
        Team team = new Team(user, project, RoleProjectEnum.VIEWER);
        when(teamRepository.findByUserIdAndProjectId(user.getId(), projectId)).thenReturn(Optional.of(team));

        // When & Then
        assertFalse(teamService.canUserEdit(user.getId(), projectId));
    }

    @Test
    void testCanUserEdit_throwsForANonMember() {
        // Given
        UUID userId = user.getId();
        when(teamRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        // When
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> teamService.canUserEdit(userId, projectId));

        // Then
        assertEquals("User is not a member of the project", exception.getMessage());
    }

}
