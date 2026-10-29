package com.erd.core.service;

import com.erd.core.dto.collaboration.EntityLockDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tells the members of a project that an entity was locked or released. Shared by the lock
 * endpoints and by the disconnect listener, so a lock dropped because its owner's session ended
 * reaches the other clients in the same format as one released by hand.
 */
@Component
public class CollaborationNotifier {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationNotifier.class);

    private final SimpMessagingTemplate messagingTemplate;

    public CollaborationNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void lockAcquired(EntityLockDTO lock) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ENTITY_LOCKED");
            message.put("payload", lock);
            message.put("projectId", lock.getProjectId());
            message.put("userEmail", lock.getUserEmail());

            logger.info("Sending WebSocket notification: type=ENTITY_LOCKED, entityId={}, projectId={}",
                       lock.getEntityId(), lock.getProjectId());

            messagingTemplate.convertAndSend("/topic/collaboration/" + lock.getProjectId(), message);
        } catch (Exception e) {
            logger.error("Error sending lock notification", e);
        }
    }

    public void lockReleased(EntityLockDTO lock) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ENTITY_UNLOCKED");
            Map<String, Object> payload = new HashMap<>();
            payload.put("entityId", lock.getEntityId());
            payload.put("userEmail", lock.getUserEmail());
            message.put("payload", payload);
            message.put("projectId", lock.getProjectId());
            message.put("userEmail", lock.getUserEmail());

            logger.info("Sending WebSocket notification: type=ENTITY_UNLOCKED, entityId={}, projectId={}",
                       lock.getEntityId(), lock.getProjectId());

            messagingTemplate.convertAndSend("/topic/collaboration/" + lock.getProjectId(), message);
        } catch (Exception e) {
            logger.error("Error sending unlock notification", e);
        }
    }

}
