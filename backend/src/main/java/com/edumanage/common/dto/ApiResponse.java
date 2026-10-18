package com.edumanage.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private T errors;
    private LocalDateTime timestamp;
    private String path;

    public ApiResponse(boolean success, String message, T data, T errors, LocalDateTime timestamp, String path) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = timestamp;
        this.path = path;
    }
}
