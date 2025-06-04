package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationDTO {

    @JsonProperty
    private String x;

    @JsonProperty
    private String y;

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}
