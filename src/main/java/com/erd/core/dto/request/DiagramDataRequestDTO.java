package com.erd.core.dto.request;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;

import java.util.List;
import java.util.UUID;

public class DiagramDataRequestDTO {

    private UUID projectId;

    private List<NodeDataDTO> nodeDataArray;
    private List<LinkDataDTO> linkDataArray;

    public UUID getProjectId() {
        return projectId;
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
}
