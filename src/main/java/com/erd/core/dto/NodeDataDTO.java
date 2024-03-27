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



}
