package com.erd.core.service;

import com.erd.core.dto.collaboration.EntityLockDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollaborationService {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationService.class);

    // In-memory storage for entity locks (in production, use Redis or database)
    private final Map<String, EntityLockDTO> entityLocks = new ConcurrentHashMap<>();

    public EntityLockDTO lockEntity(String entityId, String projectId, String userEmail, String userName) {
        logger.info("Attempting to lock entity {} by user {}", entityId, userEmail);

        // Check if entity is already locked
        EntityLockDTO existingLock = entityLocks.get(entityId);
        if (existingLock != null) {
            if (!existingLock.getUserEmail().equals(userEmail)) {
                throw new RuntimeException("Entity is already locked by " + existingLock.getUserEmail());
            }
            // Entity already locked by same user, return existing lock
            return existingLock;
        }

        // Create new lock
        EntityLockDTO lock = new EntityLockDTO(
                entityId,
                userEmail, // Using email as userId
                userEmail,
                userName,
                LocalDateTime.now(),
                projectId
        );

        entityLocks.put(entityId, lock);
        logger.info("Entity {} locked successfully by user {}", entityId, userEmail);

        return lock;
    }

    public void unlockEntity(String entityId, String userEmail) {
        logger.info("Attempting to unlock entity {} by user {}", entityId, userEmail);

        EntityLockDTO lock = entityLocks.get(entityId);
        if (lock != null) {
            if (lock.getUserEmail().equals(userEmail)) {
                entityLocks.remove(entityId);
                logger.info("Entity {} unlocked successfully", entityId);
            } else {
                logger.warn("User {} attempted to unlock entity {} but it's owned by {}", 
                           userEmail, entityId, lock.getUserEmail());
                throw new RuntimeException("Cannot unlock entity locked by another user");
            }
        } else {
            logger.warn("Attempted to unlock entity {} but no lock found", entityId);
        }
    }

    public List<EntityLockDTO> getProjectLocks(String projectId) {
        logger.info("Getting locks for project {}", projectId);

        List<EntityLockDTO> projectLocks = new ArrayList<>();
        for (EntityLockDTO lock : entityLocks.values()) {
            if (lock.getProjectId().equals(projectId)) {
                projectLocks.add(lock);
            }
        }

        return projectLocks;
    }

    public boolean isEntityLocked(String entityId) {
        return entityLocks.containsKey(entityId);
    }

    public EntityLockDTO getEntityLock(String entityId) {
        return entityLocks.get(entityId);
    }

    public void clearUserLocks(String userEmail) {
        logger.info("Clearing all locks for user {}", userEmail);

        entityLocks.entrySet().removeIf(entry -> entry.getValue().getUserEmail().equals(userEmail));
    }

    public void clearProjectLocks(String projectId) {
        logger.info("Clearing all locks for project {}", projectId);

        entityLocks.entrySet().removeIf(entry -> entry.getValue().getProjectId().equals(projectId));
    }

    // Auto-cleanup stale locks (older than 30 minutes)
    public void cleanupStaleLocks() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        entityLocks.entrySet().removeIf(entry -> {
            boolean isStale = entry.getValue().getLockedAt().isBefore(thirtyMinutesAgo);
            if (isStale) {
                logger.info("Removing stale lock for entity {} locked by {}", 
                           entry.getKey(), entry.getValue().getUserEmail());
            }
            return isStale;
        });
    }

}
