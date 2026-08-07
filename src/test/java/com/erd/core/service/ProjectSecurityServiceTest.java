package com.erd.core.service;

import com.erd.core.enumeration.RoleProjectEnum;
import com.erd.core.model.Project;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The SpEL bean behind every {@code @PreAuthorize} expression in the application. Each public method
 * wraps its body in a catch-all, so the tests cover both the authorisation outcomes and the failure
 * paths (unparsable identifiers, unknown users, an unreadable security context).
 */
@ExtendWith(MockitoExtension.class)
class ProjectSecurityServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectSecurityService projectSecurityService;

    private UUID projectId;
    private String projectIdAsString;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        projectIdAsString = projectId.toString();

        member = new User("Ada", "Lovelace", "ada@erd.com", "encoded", null);
        member.setId(UUID.randomUUID());

        project = new Project("Sales ERD", "desc");
        project.setId(projectId);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenProjectWithMember(RoleProjectEnum role) {
        project.setTeams(List.of(new Team(member, project, role)));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userService.findByEmail("ada@erd.com")).thenReturn(member);
    }

    private void givenAnotherUserIsTheOnlyMember() {
        User other = new User("Grace", "Hopper", "grace@erd.com", "encoded", null);
        other.setId(UUID.randomUUID());
        project.setTeams(List.of(new Team(other, project, RoleProjectEnum.OWNER)));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userService.findByEmail("ada@erd.com")).thenReturn(member);
    }

    // ------------------------------------------------------------------
    // isUserOwnerOrMember
    // ------------------------------------------------------------------

    @Test
    void testIsUserOwnerOrMember_allowsAMemberOfTheProject() {
        // Given
        givenProjectWithMember(RoleProjectEnum.VIEWER);

        // When & Then
        assertTrue(projectSecurityService.isUserOwnerOrMember(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsUserOwnerOrMember_deniesANonMember() {
        // Given
        givenAnotherUserIsTheOnlyMember();

        // When & Then
        assertFalse(projectSecurityService.isUserOwnerOrMember(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsUserOwnerOrMember_deniesWhenTheProjectDoesNotExist() {
        // Given
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // When & Then
        assertFalse(projectSecurityService.isUserOwnerOrMember(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsUserOwnerOrMember_deniesWhenTheProjectIdIsNotAUuid() {
        // When & Then - UUID.fromString throws and the catch-all must turn it into a denial
        assertFalse(projectSecurityService.isUserOwnerOrMember("not-a-uuid", "ada@erd.com"));
    }

    @Test
    void testIsUserOwnerOrMember_deniesWhenTheUserIsUnknown() {
        // Given
        project.setTeams(List.of());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userService.findByEmail("ghost@erd.com")).thenThrow(new UsernameNotFoundException("User not found"));

        // When & Then
        assertFalse(projectSecurityService.isUserOwnerOrMember(projectIdAsString, "ghost@erd.com"));
    }

    // ------------------------------------------------------------------
    // canUserAccessProject
    // ------------------------------------------------------------------

    @Test
    void testCanUserAccessProject_readsTheEmailFromTheSecurityContext() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member, null, List.of()));
        givenProjectWithMember(RoleProjectEnum.EDITOR);

        // When & Then
        assertTrue(projectSecurityService.canUserAccessProject(projectIdAsString));
    }

    @Test
    void testCanUserAccessProject_deniesWhenThereIsNoAuthentication() {
        // Given - empty security context

        // When & Then
        assertFalse(projectSecurityService.canUserAccessProject(projectIdAsString));
    }

    @Test
    void testCanUserAccessProject_deniesWhenThePrincipalIsNotAnApplicationUser() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        // When & Then
        assertFalse(projectSecurityService.canUserAccessProject(projectIdAsString));
    }

    @Test
    void testCanUserAccessProject_deniesWhenTheSecurityContextCannotBeRead() {
        // Given - the only way to reach the catch block guarding SecurityContextHolder access
        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            holder.when(SecurityContextHolder::getContext).thenThrow(new IllegalStateException("no context"));

            // When & Then
            assertFalse(projectSecurityService.canUserAccessProject(projectIdAsString));
        }
    }

    // ------------------------------------------------------------------
    // isProjectOwner
    // ------------------------------------------------------------------

    @Test
    void testIsProjectOwner_allowsTheOwner() {
        // Given
        givenProjectWithMember(RoleProjectEnum.OWNER);

        // When & Then
        assertTrue(projectSecurityService.isProjectOwner(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsProjectOwner_deniesAnEditor() {
        // Given
        givenProjectWithMember(RoleProjectEnum.EDITOR);

        // When & Then
        assertFalse(projectSecurityService.isProjectOwner(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsProjectOwner_deniesWhenTheOwnerIsSomebodyElse() {
        // Given - the project has an OWNER, but it is a different user
        givenAnotherUserIsTheOnlyMember();

        // When & Then
        assertFalse(projectSecurityService.isProjectOwner(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsProjectOwner_deniesWhenTheProjectDoesNotExist() {
        // Given
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // When & Then
        assertFalse(projectSecurityService.isProjectOwner(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testIsProjectOwner_deniesWhenTheProjectIdIsNotAUuid() {
        // When & Then
        assertFalse(projectSecurityService.isProjectOwner("not-a-uuid", "ada@erd.com"));
    }

    // ------------------------------------------------------------------
    // canUserEditProject
    // ------------------------------------------------------------------

    @Test
    void testCanUserEditProject_allowsTheOwner() {
        // Given
        givenProjectWithMember(RoleProjectEnum.OWNER);

        // When & Then
        assertTrue(projectSecurityService.canUserEditProject(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testCanUserEditProject_allowsAnEditor() {
        // Given
        givenProjectWithMember(RoleProjectEnum.EDITOR);

        // When & Then
        assertTrue(projectSecurityService.canUserEditProject(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testCanUserEditProject_deniesAViewer() {
        // Given
        givenProjectWithMember(RoleProjectEnum.VIEWER);

        // When & Then
        assertFalse(projectSecurityService.canUserEditProject(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testCanUserEditProject_deniesWhenTheEditorIsSomebodyElse() {
        // Given - the project has an OWNER, but it is a different user
        givenAnotherUserIsTheOnlyMember();

        // When & Then
        assertFalse(projectSecurityService.canUserEditProject(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testCanUserEditProject_deniesWhenTheProjectDoesNotExist() {
        // Given
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // When & Then
        assertFalse(projectSecurityService.canUserEditProject(projectIdAsString, "ada@erd.com"));
    }

    @Test
    void testCanUserEditProject_deniesWhenTheProjectIdIsNotAUuid() {
        // When & Then
        assertFalse(projectSecurityService.canUserEditProject("not-a-uuid", "ada@erd.com"));
    }

}
