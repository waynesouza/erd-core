package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class LocationDTO {

    @JsonProperty
    private BigDecimal x;

    @JsonProperty
    private BigDecimal y;

}
