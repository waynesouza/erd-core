package com.erd.core.controller;

import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.model.User;
import com.erd.core.service.CollaborationNotifier;
import com.erd.core.service.CollaborationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration")

public class CollaborationController {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    private final CollaborationService collaborationService;
    private final CollaborationNotifier collaborationNotifier;

    public CollaborationController(CollaborationService collaborationService, CollaborationNotifier collaborationNotifier) {
        this.collaborationService = collaborationService;
        this.collaborationNotifier = collaborationNotifier;
    }

    // The lock owner is always the authenticated caller; any userEmail/userName in the body is ignored.
    @PostMapping("/lock-entity")
    @PreAuthorize("@projectSecurityService.canUserEditProject(#request['projectId'], authentication.name)")
    public ResponseEntity<EntityLockDTO> lockEntity(@RequestBody Map<String, String> request,
                                                    Authentication authentication) {
        try {
            String entityId = request.get("entityId");
            String projectId = request.get("projectId");
            String userEmail = authentication.getName();
            String userName = displayName(authentication);

            logger.info("Lock entity request: entityId={}, userEmail={}", entityId, userEmail);

            EntityLockDTO lock = collaborationService.lockEntity(entityId, projectId, userEmail, userName);
            
            // Send WebSocket notification to other project users
            collaborationNotifier.lockAcquired(lock);
            
            return ResponseEntity.ok(lock);

        } catch (RuntimeException e) {
            logger.error("Error locking entity: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Unexpected error locking entity", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/unlock-entity")
    public ResponseEntity<Void> unlockEntity(@RequestBody Map<String, String> request,
                                             Authentication authentication) {
        try {
            String entityId = request.get("entityId");
            String userEmail = authentication.getName();

            logger.info("Unlock entity request: entityId={}, userEmail={}", entityId, userEmail);

            // Get lock information before removing
            EntityLockDTO lockToRemove = collaborationService.getEntityLock(entityId);
            
            collaborationService.unlockEntity(entityId, userEmail);
            
            // Send WebSocket notification to other project users
            if (lockToRemove != null) {
                collaborationNotifier.lockReleased(lockToRemove);
            }
            
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            logger.error("Error unlocking entity: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error unlocking entity", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/project-locks/{projectId}")
    @PreAuthorize("@projectSecurityService.isUserOwnerOrMember(#projectId, authentication.name)")
    public ResponseEntity<List<EntityLockDTO>> getProjectLocks(@PathVariable String projectId) {
        try {
            logger.info("Get project locks request: projectId={}", projectId);

            List<EntityLockDTO> locks = collaborationService.getProjectLocks(projectId);
            return ResponseEntity.ok(locks);

        } catch (Exception e) {
            logger.error("Error getting project locks", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/entity-lock/{entityId}")
    public ResponseEntity<EntityLockDTO> getEntityLock(@PathVariable String entityId) {
        try {
            EntityLockDTO lock = collaborationService.getEntityLock(entityId);
            if (lock != null) {
                return ResponseEntity.ok(lock);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting entity lock", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @DeleteMapping("/user-locks/{userEmail}")
    @PreAuthorize("#userEmail == authentication.name")
    public ResponseEntity<Void> clearUserLocks(@PathVariable String userEmail) {
        try {
            logger.info("Clear user locks request: userEmail={}", userEmail);

            collaborationService.clearUserLocks(userEmail);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error clearing user locks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup-stale-locks")
    public ResponseEntity<Void> cleanupStaleLocks() {
        try {
            logger.info("Cleanup stale locks request");

            collaborationService.cleanupStaleLocks();
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error cleaning up stale locks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String displayName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return authentication.getName();
    }

}
