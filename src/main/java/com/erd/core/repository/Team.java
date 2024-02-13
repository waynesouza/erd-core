package com.erd.core.repository;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table
public class Team {

    @Id
    @UuidGenerator
    private UUID id;
    private String name;
    private LocalDateTime creationDate;
    private boolean active;

    @OneToMany
    private List<User> users;

    public Team() {}

    public Team(String name, LocalDateTime creationDate, boolean active, List<User> users) {
        this.name = name;
        this.creationDate = creationDate;
        this.active = active;
        this.users = users;
    }

    // getters and setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

}