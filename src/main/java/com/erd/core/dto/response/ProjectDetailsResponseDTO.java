package com.erd.core.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ProjectDetailsResponseDTO implements Serializable {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private List<UserProjectDetailsResponseDTO> usersDto;

    public ProjectDetailsResponseDTO() { }

    public ProjectDetailsResponseDTO(UUID id, String name, String description, LocalDateTime createdAt, List<UserProjectDetailsResponseDTO> usersDto) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.usersDto = usersDto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<UserProjectDetailsResponseDTO> getUsersDto() {
        return usersDto;
    }

    public void setUsersDto(List<UserProjectDetailsResponseDTO> usersDto) {
        this.usersDto = usersDto;
    }

}
