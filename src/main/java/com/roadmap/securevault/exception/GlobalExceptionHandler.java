package com.roadmap.securevault.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {
            CredentialsTakenException.class,
            InvalidStateException.class,
            OwnerDeletionException.class})
    public ResponseEntity<String> handleConflictException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(value = {
            UsernameNotFoundException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<String> handleUnauthorizedException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(value = {EntityNotFoundException.class})
    public ResponseEntity<String> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors
                        .toMap(FieldError::getField,
                                FieldError::getDefaultMessage,
                                (existing, duplicate) -> existing));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
