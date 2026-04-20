package ru.platezh.admin.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(int status, String message, Instant timestamp) {
        static ErrorBody of(HttpStatus status, String message) {
            return new ErrorBody(status.value(), message, Instant.now());
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorBody> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorBody.of(HttpStatus.NOT_FOUND, ex.getMessage() != null ? ex.getMessage() : "Not found"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorBody.of(HttpStatus.FORBIDDEN, ex.getMessage()));
    }
}
