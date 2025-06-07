package com.erd.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExportDdlResponseDTO {

    @JsonProperty
    private String ddlContent;

    @JsonProperty
    private String projectId;

    public ExportDdlResponseDTO() {
    }

    public ExportDdlResponseDTO(String ddlContent, String projectId) {
        this.ddlContent = ddlContent;
        this.projectId = projectId;
    }

    public String getDdlContent() {
        return ddlContent;
    }

    public void setDdlContent(String ddlContent) {
        this.ddlContent = ddlContent;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
} 