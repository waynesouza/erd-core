package com.erd.core.model;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "diagram")
public class Diagram {

    @Id
    @UuidGenerator
    private UUID id;

    private String nodeData;

    private String linkData;

    private String projectId;

    public Diagram(String nodeData, String linkData, String projectId) {
        this.nodeData = nodeData;
        this.linkData = linkData;
        this.projectId = projectId;
    }

    public UUID getId() {
        return id;
    }

    public String getNodeData() {
        return nodeData;
    }

    public void setNodeData(String nodeData) {
        this.nodeData = nodeData;
    }

    public String getLinkData() {
        return linkData;
    }

    public void setLinkData(String linkData) {
        this.linkData = linkData;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
