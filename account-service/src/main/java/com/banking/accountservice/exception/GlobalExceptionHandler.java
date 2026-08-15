package com.banking.accountservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("message", ex.getMessage());

        if (ex.getMessage() != null
                && ex.getMessage().contains("already exists")) {
            body.put("status", 400);
            body.put("error", "Bad Request");
            return ResponseEntity.badRequest().body(body);
        }

        if (ex.getMessage() != null
                && ex.getMessage().contains("not found")) {
            body.put("status", 404);
            body.put("error", "Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        body.put("status", 500);
        body.put("error", "Internal Server Error");
        return ResponseEntity.internalServerError().body(body);
    }
}