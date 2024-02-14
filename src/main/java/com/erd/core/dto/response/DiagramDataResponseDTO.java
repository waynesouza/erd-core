package com.erd.core.dto.response;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class DiagramDataResponseDTO implements Serializable {

    private List<NodeDataDTO> nodeDataArray;
    private List<LinkDataDTO> linkDataArray;

    public DiagramDataResponseDTO() { }

    public DiagramDataResponseDTO(List<NodeDataDTO> nodeDataArray, List<LinkDataDTO> linkDataArray) {
        this.nodeDataArray = nodeDataArray;
        this.linkDataArray = linkDataArray;
    }

    public void setNodeDataArray(List<NodeDataDTO> nodeDataArray) {
        this.nodeDataArray = nodeDataArray;
    }

    public void setLinkDataArray(List<LinkDataDTO> linkDataArray) {
        this.linkDataArray = linkDataArray;
    }
}
