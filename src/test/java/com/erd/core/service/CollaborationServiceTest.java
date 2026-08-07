package com.erd.core.service;

import com.erd.core.dto.collaboration.EntityLockDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CollaborationService} keeps its state in an in-memory map and has no collaborators, so it
 * is driven directly rather than through mocks. A fresh instance per test keeps the registry clean.
 */
class CollaborationServiceTest {

    private static final String ENTITY_ID = "table-users";
    private static final String PROJECT_ID = "project-1";
    private static final String OWNER_EMAIL = "ada@erd.com";

    private CollaborationService collaborationService;

    @BeforeEach
    void setUp() {
        collaborationService = new CollaborationService();
    }

    // ------------------------------------------------------------------
    // lockEntity
    // ------------------------------------------------------------------

    @Test
    void testLockEntity_createsALockForAFreeEntity() {
        // When
        EntityLockDTO lock = collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // Then
        assertEquals(ENTITY_ID, lock.getEntityId());
        assertEquals(PROJECT_ID, lock.getProjectId());
        assertEquals(OWNER_EMAIL, lock.getUserEmail());
        assertEquals(OWNER_EMAIL, lock.getUserId(), "The service uses the e-mail as the user identifier");
        assertEquals("Ada Lovelace", lock.getUserName());
        assertNotNull(lock.getLockedAt());
        assertTrue(collaborationService.isEntityLocked(ENTITY_ID));
    }

    @Test
    void testLockEntity_isIdempotentForTheSameUser() {
        // Given
        EntityLockDTO first = collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // When
        EntityLockDTO second = collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // Then
        assertSame(first, second, "Re-locking by the same user must return the existing lock");
    }

    @Test
    void testLockEntity_refusesAnEntityHeldByAnotherUser() {
        // Given
        collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, "grace@erd.com", "Grace Hopper"));

        // Then
        assertEquals("Entity is already locked by " + OWNER_EMAIL, exception.getMessage());
    }

    // ------------------------------------------------------------------
    // unlockEntity
    // ------------------------------------------------------------------

    @Test
    void testUnlockEntity_releasesTheLockForItsOwner() {
        // Given
        collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // When
        collaborationService.unlockEntity(ENTITY_ID, OWNER_EMAIL);

        // Then
        assertFalse(collaborationService.isEntityLocked(ENTITY_ID));
    }

    @Test
    void testUnlockEntity_refusesToReleaseALockHeldByAnotherUser() {
        // Given
        collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada Lovelace");

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> collaborationService.unlockEntity(ENTITY_ID, "grace@erd.com"));

        // Then
        assertEquals("Cannot unlock entity locked by another user", exception.getMessage());
        assertTrue(collaborationService.isEntityLocked(ENTITY_ID));
    }

    @Test
    void testUnlockEntity_isANoOpWhenNoLockExists() {
        // When - must not throw
        collaborationService.unlockEntity("unknown-entity", OWNER_EMAIL);

        // Then
        assertFalse(collaborationService.isEntityLocked("unknown-entity"));
    }

    // ------------------------------------------------------------------
    // queries
    // ------------------------------------------------------------------

    @Test
    void testGetProjectLocks_returnsOnlyTheLocksOfThatProject() {
        // Given
        collaborationService.lockEntity("table-a", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.lockEntity("table-b", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.lockEntity("table-c", "project-2", OWNER_EMAIL, "Ada");

        // When
        List<EntityLockDTO> locks = collaborationService.getProjectLocks(PROJECT_ID);

        // Then
        assertEquals(2, locks.size());
        assertTrue(locks.stream().allMatch(lock -> PROJECT_ID.equals(lock.getProjectId())));
    }

    @Test
    void testGetProjectLocks_returnsAnEmptyListForAnUnknownProject() {
        // When & Then
        assertTrue(collaborationService.getProjectLocks("project-without-locks").isEmpty());
    }

    @Test
    void testGetEntityLock_returnsNullWhenTheEntityIsFree() {
        // When & Then
        assertNull(collaborationService.getEntityLock("free-entity"));
        assertFalse(collaborationService.isEntityLocked("free-entity"));
    }

    @Test
    void testGetEntityLock_returnsTheLockWhenHeld() {
        // Given
        EntityLockDTO lock = collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada");

        // When & Then
        assertSame(lock, collaborationService.getEntityLock(ENTITY_ID));
    }

    // ------------------------------------------------------------------
    // bulk release
    // ------------------------------------------------------------------

    @Test
    void testClearUserLocks_removesOnlyThatUsersLocks() {
        // Given
        collaborationService.lockEntity("table-a", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.lockEntity("table-b", PROJECT_ID, "grace@erd.com", "Grace");

        // When
        collaborationService.clearUserLocks(OWNER_EMAIL);

        // Then
        assertFalse(collaborationService.isEntityLocked("table-a"));
        assertTrue(collaborationService.isEntityLocked("table-b"));
    }

    @Test
    void testClearProjectLocks_removesOnlyThatProjectsLocks() {
        // Given
        collaborationService.lockEntity("table-a", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.lockEntity("table-c", "project-2", OWNER_EMAIL, "Ada");

        // When
        collaborationService.clearProjectLocks(PROJECT_ID);

        // Then
        assertFalse(collaborationService.isEntityLocked("table-a"));
        assertTrue(collaborationService.isEntityLocked("table-c"));
    }

    // ------------------------------------------------------------------
    // scheduled cleanup
    // ------------------------------------------------------------------

    @Test
    void testCleanupStaleLocks_dropsLocksOlderThanFiveMinutesAndKeepsFreshOnes() {
        // Given
        collaborationService.lockEntity("stale-table", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.lockEntity("fresh-table", PROJECT_ID, OWNER_EMAIL, "Ada");
        collaborationService.getEntityLock("stale-table").setLockedAt(LocalDateTime.now().minusMinutes(10));

        // When
        collaborationService.cleanupStaleLocks();

        // Then
        assertFalse(collaborationService.isEntityLocked("stale-table"));
        assertTrue(collaborationService.isEntityLocked("fresh-table"));
    }

    @Test
    void testCleanupStaleLocks_isANoOpWhenThereIsNothingToReap() {
        // Given
        collaborationService.lockEntity(ENTITY_ID, PROJECT_ID, OWNER_EMAIL, "Ada");

        // When
        collaborationService.cleanupStaleLocks();

        // Then
        assertTrue(collaborationService.isEntityLocked(ENTITY_ID));
    }

}
