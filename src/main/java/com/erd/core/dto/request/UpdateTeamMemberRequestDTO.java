package com.erd.core.dto.request;

import com.erd.core.enumeration.RoleProjectEnum;

import java.io.Serializable;
import java.util.UUID;

public class UpdateTeamMemberRequestDTO implements Serializable {

    private UUID userId;
    private UUID projectId;
    private RoleProjectEnum role;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public RoleProjectEnum getRole() {
        return role;
    }

    public void setRole(RoleProjectEnum role) {
        this.role = role;
    }

}
