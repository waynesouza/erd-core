package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkDataDTO {

    @JsonProperty
    private String id;

    @JsonProperty
    private String from;

    @JsonProperty
    private String to;

    @JsonProperty
    private String text;

    @JsonProperty
    private String toText;

}
