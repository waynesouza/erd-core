package com.erd.core.dto.request;

import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.NodeDataDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The changes one client made since the last diagram state it received: the tables and links it
 * added or modified, and the ids of the ones it removed. The server applies them to the stored
 * diagram element by element, so concurrent edits to different elements no longer overwrite each
 * other the way sending the whole diagram did.
 */
public class DiagramPatchRequestDTO implements Serializable {

    private UUID projectId;

    private List<NodeDataDTO> upsertNodes = new ArrayList<>();
    private List<String> removedNodeIds = new ArrayList<>();
    private List<LinkDataDTO> upsertLinks = new ArrayList<>();
    private List<String> removedLinkIds = new ArrayList<>();

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public List<NodeDataDTO> getUpsertNodes() {
        return upsertNodes;
    }

    public void setUpsertNodes(List<NodeDataDTO> upsertNodes) {
        this.upsertNodes = upsertNodes;
    }

    public List<String> getRemovedNodeIds() {
        return removedNodeIds;
    }

    public void setRemovedNodeIds(List<String> removedNodeIds) {
        this.removedNodeIds = removedNodeIds;
    }

    public List<LinkDataDTO> getUpsertLinks() {
        return upsertLinks;
    }

    public void setUpsertLinks(List<LinkDataDTO> upsertLinks) {
        this.upsertLinks = upsertLinks;
    }

    public List<String> getRemovedLinkIds() {
        return removedLinkIds;
    }

    public void setRemovedLinkIds(List<String> removedLinkIds) {
        this.removedLinkIds = removedLinkIds;
    }

}
