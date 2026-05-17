package com.erd.core.dto.request;

import java.io.Serializable;

public class ProjectCreateRequestDTO implements Serializable {

    private String name;
    private String description;

    public ProjectCreateRequestDTO() { }

    public ProjectCreateRequestDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
