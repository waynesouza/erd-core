package com.erd.core.dto.response;

public class EmailResponseDTO {

    private String id;
    private String status;
    private String message;

    public EmailResponseDTO() {
    }

    public EmailResponseDTO(String id, String status, String message) {
        this.id = id;
        this.status = status;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
