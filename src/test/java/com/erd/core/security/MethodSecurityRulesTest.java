package com.erd.core.security;

import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.service.DdlService;
import com.erd.core.service.DiagramService;
import com.erd.core.service.ProjectSecurityService;
import com.erd.core.service.ProjectService;
import com.erd.core.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that the {@code @PreAuthorize} expressions are actually enforced.
 * <p>
 * The controller unit tests use {@code MockMvcBuilders.standaloneSetup}, which bypasses method
 * security entirely, so authorization is asserted here against the real filter chain and the real
 * {@code @EnableMethodSecurity} infrastructure. Only {@link ProjectSecurityService} - the bean every
 * SpEL expression delegates to - and the controllers' business collaborators are replaced.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MethodSecurityRulesTest {

    private static final String USER_EMAIL = "ada@erd.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectSecurityService projectSecurityService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private DdlService ddlService;

    @Test
    @WithAnonymousUser
    void testProtectedRoute_rejectsAnAnonymousCaller() throws Exception {
        // When & Then - the JWT entry point answers 401 before method security is consulted
        mockMvc.perform(get("/api/project/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testGetProjectDetails_isDeniedWhenTheUserIsNotAMember() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        when(projectSecurityService.isUserOwnerOrMember(eq(projectId.toString()), eq(USER_EMAIL)))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/project/{id}", projectId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testGetProjectDetails_isAllowedForAMember() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        when(projectSecurityService.isUserOwnerOrMember(eq(projectId.toString()), eq(USER_EMAIL)))
                .thenReturn(true);
        when(projectService.getProjectDetailsById(projectId)).thenReturn(
                new ProjectDetailsResponseDTO(projectId, "Sales ERD", "desc", LocalDateTime.now()));

        // When & Then
        mockMvc.perform(get("/api/project/{id}", projectId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testExportDdl_isDeniedWhenTheUserIsNotAMember() throws Exception {
        // Given
        when(projectSecurityService.isUserOwnerOrMember(anyString(), eq(USER_EMAIL))).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/ddl/export/{projectId}", "project-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testExportDdl_isAllowedForAMember() throws Exception {
        // Given
        when(projectSecurityService.isUserOwnerOrMember(anyString(), eq(USER_EMAIL))).thenReturn(true);
        when(ddlService.exportDdl("project-1"))
                .thenReturn(new ExportDdlResponseDTO("CREATE TABLE users (id INT);", "project-1"));

        // When & Then
        mockMvc.perform(get("/api/ddl/export/{projectId}", "project-1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testGetProjectMembers_isDeniedWhenTheUserIsNotAMember() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        when(projectSecurityService.isUserOwnerOrMember(eq(projectId.toString()), eq(USER_EMAIL)))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/project/{id}/members", projectId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, authorities = "USER")
    void testUnprotectedRoute_doesNotConsultTheSecurityService() throws Exception {
        // Given - getProjectsByUserEmail carries no @PreAuthorize
        when(projectService.getProjectsByUserEmail(USER_EMAIL)).thenReturn(java.util.List.of());

        // When & Then
        mockMvc.perform(get("/api/project/user-email/{email}", USER_EMAIL))
                .andExpect(status().isOk());
    }

}
