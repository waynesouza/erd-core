package com.erd.core.model.mongo;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "diagram")
public class Diagram {

    @Id
    private UUID id;

    private String nodeData;

    private String linkData;

    private String projectId;

    // Incremented each time the whole diagram is replaced, as an import does. A patch carries the
    // revision it was computed against, so one built on the replaced content can be told apart.
    private long revision;

    public Diagram() {
        this.id = UUID.randomUUID();
    }

    public Diagram(String nodeData, String linkData, String projectId) {
        this.id = UUID.randomUUID();
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

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}
