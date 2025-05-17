package com.erd.core.dto.response;

import com.erd.core.enumeration.RoleEnum;

import java.io.Serializable;
import java.util.UUID;

public class UserResponseDTO implements Serializable {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private RoleEnum role;

    public UserResponseDTO() { }

    public UserResponseDTO(UUID id, String firstName, String lastName, String email, RoleEnum role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

}
