package com.anju.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        String msg = "Validation failed";
        if (ex instanceof MethodArgumentNotValidException manv
                && manv.getBindingResult().getFieldError() != null) {
            msg = manv.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getStatusCode().value(), ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unhandled server error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal server error"));
    }
}
