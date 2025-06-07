package com.erd.core.dto.response;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class DiagramDataResponseDTO implements Serializable {

    @JsonProperty
    private List<NodeDataDTO> nodeDataArray;

    @JsonProperty
    private List<LinkDataDTO> linkDataArray;

    @JsonProperty
    private String nodeData;

    @JsonProperty
    private String linkData;

    @JsonProperty
    private String projectId;

    public DiagramDataResponseDTO() { }

    public DiagramDataResponseDTO(List<NodeDataDTO> nodeDataArray, List<LinkDataDTO> linkDataArray) {
        this.nodeDataArray = nodeDataArray;
        this.linkDataArray = linkDataArray;
    }

    public List<NodeDataDTO> getNodeDataArray() {
        return nodeDataArray;
    }

    public void setNodeDataArray(List<NodeDataDTO> nodeDataArray) {
        this.nodeDataArray = nodeDataArray;
    }

    public List<LinkDataDTO> getLinkDataArray() {
        return linkDataArray;
    }

    public void setLinkDataArray(List<LinkDataDTO> linkDataArray) {
        this.linkDataArray = linkDataArray;
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
