package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemDTO {

    @JsonProperty
    private String name;

    @JsonProperty
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
