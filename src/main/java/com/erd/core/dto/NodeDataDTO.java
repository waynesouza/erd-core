package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class NodeDataDTO {

    @JsonProperty
    private UUID id;

    @JsonProperty
    private String key;

    @JsonProperty
    private List<ItemDTO> items;

    @JsonProperty
    private LocationDTO location;

    // Set only on the intermediary table of a many-to-many relationship: the two tables it joins.
    // Without them the client cannot clean the intermediary table up once the diagram is reloaded.
    @JsonProperty
    private String firstEntityId;

    @JsonProperty
    private String secondEntityId;

    public String getFirstEntityId() {
        return firstEntityId;
    }

    public void setFirstEntityId(String firstEntityId) {
        this.firstEntityId = firstEntityId;
    }

    public String getSecondEntityId() {
        return secondEntityId;
    }

    public void setSecondEntityId(String secondEntityId) {
        this.secondEntityId = secondEntityId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<ItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemDTO> items) {
        this.items = items;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }
}
