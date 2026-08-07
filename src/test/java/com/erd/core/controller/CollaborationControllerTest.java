package com.erd.core.controller;

import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.service.CollaborationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every endpoint of this controller is a {@code try} / {@code catch (RuntimeException)} /
 * {@code catch (Exception)} triple. The last arm is only reachable via an undeclared checked
 * exception, which is produced with {@code doAnswer} - {@code thenThrow} would be rejected by
 * Mockito because the service method does not declare it.
 */
@ExtendWith(MockitoExtension.class)
class CollaborationControllerTest {

    private static final String ENTITY_ID = "table-users";
    private static final String PROJECT_ID = "project-1";
    private static final String USER_EMAIL = "ada@erd.com";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CollaborationController collaborationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(collaborationController).build();
        objectMapper = new ObjectMapper();
    }

    private EntityLockDTO lock() {
        return new EntityLockDTO(ENTITY_ID, USER_EMAIL, USER_EMAIL, "Ada Lovelace", LocalDateTime.now(), PROJECT_ID);
    }

    private String lockPayload() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "entityId", ENTITY_ID,
                "projectId", PROJECT_ID,
                "userEmail", USER_EMAIL,
                "userName", "Ada Lovelace"));
    }

    private String unlockPayload() throws Exception {
        return objectMapper.writeValueAsString(Map.of("entityId", ENTITY_ID, "userEmail", USER_EMAIL));
    }

    // ------------------------------------------------------------------
    // lock-entity
    // ------------------------------------------------------------------

    @Test
    void testLockEntity_returnsTheLockAndNotifiesTheProjectTopic() throws Exception {
        // Given
        when(collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, USER_EMAIL, "Ada Lovelace")).thenReturn(lock());

        // When & Then
        mockMvc.perform(post("/api/collaboration/lock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(ENTITY_ID))
                .andExpect(jsonPath("$.userEmail").value(USER_EMAIL));

        verify(messagingTemplate).convertAndSend(eq("/topic/collaboration/" + PROJECT_ID), any(Object.class));
    }

    @Test
    void testLockEntity_returnsBadRequestWhenTheEntityIsAlreadyLocked() throws Exception {
        // Given
        when(collaborationService.lockEntity(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Entity is already locked by grace@erd.com"));

        // When & Then
        mockMvc.perform(post("/api/collaboration/lock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockPayload()))
                .andExpect(status().isBadRequest());

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testLockEntity_returnsServerErrorOnAnUnexpectedFailure() throws Exception {
        // Given
        doAnswer(invocation -> {
            throw new IOException("unexpected");
        }).when(collaborationService).lockEntity(anyString(), anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/collaboration/lock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockPayload()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testLockEntity_stillSucceedsWhenTheNotificationCannotBeSent() throws Exception {
        // Given
        when(collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, USER_EMAIL, "Ada Lovelace")).thenReturn(lock());
        doThrow(new MessagingException("broker unavailable"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When & Then
        mockMvc.perform(post("/api/collaboration/lock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockPayload()))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // unlock-entity
    // ------------------------------------------------------------------

    @Test
    void testUnlockEntity_releasesTheLockAndNotifiesTheProjectTopic() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(lock());

        // When & Then
        mockMvc.perform(post("/api/collaboration/unlock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unlockPayload()))
                .andExpect(status().isOk());

        verify(collaborationService).unlockEntity(ENTITY_ID, USER_EMAIL);
        verify(messagingTemplate).convertAndSend(eq("/topic/collaboration/" + PROJECT_ID), any(Object.class));
    }

    @Test
    void testUnlockEntity_skipsTheNotificationWhenNoLockWasHeld() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/collaboration/unlock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unlockPayload()))
                .andExpect(status().isOk());

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testUnlockEntity_returnsBadRequestWhenTheLockBelongsToAnotherUser() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(lock());
        doThrow(new RuntimeException("Cannot unlock entity locked by another user"))
                .when(collaborationService).unlockEntity(anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/collaboration/unlock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unlockPayload()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnlockEntity_returnsServerErrorOnAnUnexpectedFailure() throws Exception {
        // Given
        doAnswer(invocation -> {
            throw new IOException("unexpected");
        }).when(collaborationService).getEntityLock(anyString());

        // When & Then
        mockMvc.perform(post("/api/collaboration/unlock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unlockPayload()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUnlockEntity_stillSucceedsWhenTheNotificationCannotBeSent() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(lock());
        doThrow(new MessagingException("broker unavailable"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When & Then
        mockMvc.perform(post("/api/collaboration/unlock-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unlockPayload()))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // project-locks
    // ------------------------------------------------------------------

    @Test
    void testGetProjectLocks_returnsTheLocksOfTheProject() throws Exception {
        // Given
        when(collaborationService.getProjectLocks(PROJECT_ID)).thenReturn(List.of(lock()));

        // When & Then
        mockMvc.perform(get("/api/collaboration/project-locks/{projectId}", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(ENTITY_ID));
    }

    @Test
    void testGetProjectLocks_returnsServerErrorOnFailure() throws Exception {
        // Given
        when(collaborationService.getProjectLocks(PROJECT_ID)).thenThrow(new RuntimeException("boom"));

        // When & Then
        mockMvc.perform(get("/api/collaboration/project-locks/{projectId}", PROJECT_ID))
                .andExpect(status().isInternalServerError());
    }

    // ------------------------------------------------------------------
    // entity-lock
    // ------------------------------------------------------------------

    @Test
    void testGetEntityLock_returnsTheLockWhenHeld() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(lock());

        // When & Then
        mockMvc.perform(get("/api/collaboration/entity-lock/{entityId}", ENTITY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(USER_EMAIL));
    }

    @Test
    void testGetEntityLock_returnsNotFoundWhenTheEntityIsFree() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/collaboration/entity-lock/{entityId}", ENTITY_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetEntityLock_returnsServerErrorOnFailure() throws Exception {
        // Given
        when(collaborationService.getEntityLock(ENTITY_ID)).thenThrow(new RuntimeException("boom"));

        // When & Then
        mockMvc.perform(get("/api/collaboration/entity-lock/{entityId}", ENTITY_ID))
                .andExpect(status().isInternalServerError());
    }

    // ------------------------------------------------------------------
    // user-locks / cleanup-stale-locks
    // ------------------------------------------------------------------

    @Test
    void testClearUserLocks_returnsOk() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/collaboration/user-locks/{userEmail}", USER_EMAIL))
                .andExpect(status().isOk());

        verify(collaborationService).clearUserLocks(USER_EMAIL);
    }

    @Test
    void testClearUserLocks_returnsServerErrorOnFailure() throws Exception {
        // Given
        doThrow(new RuntimeException("boom")).when(collaborationService).clearUserLocks(USER_EMAIL);

        // When & Then
        mockMvc.perform(delete("/api/collaboration/user-locks/{userEmail}", USER_EMAIL))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCleanupStaleLocks_returnsOk() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/collaboration/cleanup-stale-locks"))
                .andExpect(status().isOk());

        verify(collaborationService).cleanupStaleLocks();
    }

    @Test
    void testCleanupStaleLocks_returnsServerErrorOnFailure() throws Exception {
        // Given
        doThrow(new RuntimeException("boom")).when(collaborationService).cleanupStaleLocks();

        // When & Then
        mockMvc.perform(post("/api/collaboration/cleanup-stale-locks"))
                .andExpect(status().isInternalServerError());
    }

}
