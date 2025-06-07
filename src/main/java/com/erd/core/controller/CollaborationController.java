package com.erd.core.controller;

import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.service.CollaborationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration")
@CrossOrigin(origins = "http://localhost:4200")
public class CollaborationController {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;

    public CollaborationController(CollaborationService collaborationService, SimpMessagingTemplate messagingTemplate) {
        this.collaborationService = collaborationService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/lock-entity")
    public ResponseEntity<EntityLockDTO> lockEntity(@RequestBody Map<String, String> request) {
        try {
            String entityId = request.get("entityId");
            String projectId = request.get("projectId");
            String userEmail = request.get("userEmail");
            String userName = request.get("userName");

            logger.info("Lock entity request: entityId={}, userEmail={}", entityId, userEmail);

            EntityLockDTO lock = collaborationService.lockEntity(entityId, projectId, userEmail, userName);
            
            // Send WebSocket notification to other project users
            sendLockNotification("ENTITY_LOCKED", lock);
            
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
    public ResponseEntity<Void> unlockEntity(@RequestBody Map<String, String> request) {
        try {
            String entityId = request.get("entityId");
            String userEmail = request.get("userEmail");

            logger.info("Unlock entity request: entityId={}, userEmail={}", entityId, userEmail);

            // Get lock information before removing
            EntityLockDTO lockToRemove = collaborationService.getEntityLock(entityId);
            
            collaborationService.unlockEntity(entityId, userEmail);
            
            // Send WebSocket notification to other project users
            if (lockToRemove != null) {
                sendUnlockNotification("ENTITY_UNLOCKED", lockToRemove);
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

    private void sendLockNotification(String type, EntityLockDTO lock) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("payload", lock);
            message.put("projectId", lock.getProjectId());
            message.put("userEmail", lock.getUserEmail());

            logger.info("Sending WebSocket notification: type={}, entityId={}, projectId={}", 
                       type, lock.getEntityId(), lock.getProjectId());

            messagingTemplate.convertAndSend("/topic/collaboration", message);
        } catch (Exception e) {
            logger.error("Error sending lock notification", e);
        }
    }

    private void sendUnlockNotification(String type, EntityLockDTO lock) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            Map<String, Object> payload = new HashMap<>();
            payload.put("entityId", lock.getEntityId());
            payload.put("userEmail", lock.getUserEmail());
            message.put("payload", payload);
            message.put("projectId", lock.getProjectId());
            message.put("userEmail", lock.getUserEmail());

            logger.info("Sending WebSocket notification: type={}, entityId={}, projectId={}", 
                       type, lock.getEntityId(), lock.getProjectId());

            messagingTemplate.convertAndSend("/topic/collaboration", message);
        } catch (Exception e) {
            logger.error("Error sending unlock notification", e);
        }
    }

}
