package com.erd.core.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ProjectCreateRequestDTO implements Serializable {

    private String name;
    private String description;
    private String userEmail;

    public ProjectCreateRequestDTO() { }

    public ProjectCreateRequestDTO(String name, String description, String userEmail) {
        this.name = name;
        this.description = description;
        this.userEmail = userEmail;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

}
