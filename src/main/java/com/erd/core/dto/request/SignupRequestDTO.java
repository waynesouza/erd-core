package com.erd.core.dto.request;

import com.erd.core.enumeration.RoleEnum;

import java.io.Serializable;

public class SignupRequestDTO implements Serializable {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private RoleEnum role;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public RoleEnum getRole() {
        return role;
    }

}
