package com.erd.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportDdlRequestDTO {

    @JsonProperty
    private String projectId;

    @JsonProperty
    private String ddlContent;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDdlContent() {
        return ddlContent;
    }

    public void setDdlContent(String ddlContent) {
        this.ddlContent = ddlContent;
    }
} 