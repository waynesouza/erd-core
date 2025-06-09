package com.erd.core.dto.collaboration;

import java.time.LocalDateTime;

public class EntityLockDTO {
    private String entityId;
    private String userId;
    private String userEmail;
    private String userName;
    private LocalDateTime lockedAt;
    private String projectId;

    public EntityLockDTO() {}

    public EntityLockDTO(String entityId, String userId, String userEmail, String userName, LocalDateTime lockedAt, String projectId) {
        this.entityId = entityId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.lockedAt = lockedAt;
        this.projectId = projectId;
    }

    // Getters and Setters
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

}
