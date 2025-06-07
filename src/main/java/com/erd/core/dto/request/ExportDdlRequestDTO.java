package com.erd.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExportDdlRequestDTO {

    @JsonProperty
    private String projectId;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
} 