package com.erd.core.dto.request;

import com.erd.core.enumeration.RoleProjectEnum;

import java.io.Serializable;
import java.util.UUID;

public class TeamMemberRequestDTO implements Serializable {

    private String userEmail;
    private UUID projectId;
    private RoleProjectEnum roleProjectEnum;

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public RoleProjectEnum getRoleProjectEnum() {
        return roleProjectEnum;
    }

}
