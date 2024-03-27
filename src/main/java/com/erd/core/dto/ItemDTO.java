package com.erd.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemDTO {

    @JsonProperty
    private String name;

    @JsonProperty
    private String type;

    @JsonProperty
    private Boolean pk;

    @JsonProperty
    private Boolean unique;

    @JsonProperty
    private String defaultValue;

    @JsonProperty
    private Boolean nullable;

    @JsonProperty
    private Boolean autoIncrement;

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

    public Boolean getPk() {
        return pk;
    }

    public void setPk(Boolean pk) {
        this.pk = pk;
    }

    public Boolean getUnique() {
        return unique;
    }

    public void setUnique(Boolean unique) {
        this.unique = unique;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Boolean getAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(Boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

}
