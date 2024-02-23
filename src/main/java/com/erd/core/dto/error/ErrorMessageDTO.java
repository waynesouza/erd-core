package com.erd.core.dto.error;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ErrorMessageDTO implements Serializable {

    private Integer statusCode;
    private LocalDateTime timestamp;
    private String message;
    private String description;

    public ErrorMessageDTO() { }

    public ErrorMessageDTO(Integer statusCode, LocalDateTime timestamp, String message, String description) {
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.message = message;
        this.description = description;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

}
