package com.erd.core.dto.request;

import java.io.Serializable;

public class CreateDiagramRequestDTO implements Serializable {

    private String projectId;

    public CreateDiagramRequestDTO() { }

    public CreateDiagramRequestDTO(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

}
