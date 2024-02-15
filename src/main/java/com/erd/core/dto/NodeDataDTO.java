package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NodeDataDTO {

    @JsonProperty
    private String key;

    @JsonProperty
    private List<ItemDTO> items;

    @JsonProperty
    private LocationDTO location;



}
