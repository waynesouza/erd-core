package com.erd.core.dto.response;

import com.erd.core.enumeration.RoleProjectEnum;

import java.io.Serializable;
import java.util.UUID;

public class UserProjectDetailsResponseDTO implements Serializable {

    private UUID id;

    private String email;
    
    private String firstName;
    
    private String lastName;

    private RoleProjectEnum role;

    public UserProjectDetailsResponseDTO(UUID id, String email, String firstName, String lastName, RoleProjectEnum role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleProjectEnum getRole() {
        return role;
    }

    public void setRole(RoleProjectEnum role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
