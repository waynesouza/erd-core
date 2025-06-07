package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkDataDTO {

    @JsonProperty
    private String from;

    @JsonProperty
    private String to;

    @JsonProperty
    private String text;

    @JsonProperty
    private String toText;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getToText() {
        return toText;
    }

    public void setToText(String toText) {
        this.toText = toText;
    }
}
