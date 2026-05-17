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
    private Integer toText;

    @JsonProperty
    private String fromColumn;

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

    public Integer getToText() {
        return toText;
    }

    public void setToText(Integer toText) {
        this.toText = toText;
    }

    public String getFromColumn() {
        return fromColumn;
    }

    public void setFromColumn(String fromColumn) {
        this.fromColumn = fromColumn;
    }
}
