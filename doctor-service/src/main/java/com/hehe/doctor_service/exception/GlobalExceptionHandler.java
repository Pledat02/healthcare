package com.hehe.doctor_service.exception;

import com.hehe.doctor_service.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi validate @Valid  ->  400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Object>builder().code(400).message(message).build());
    }


    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handle(AppException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getCode())
                .body(ApiResponse.<Object>builder()
                        .code(ec.getCode())
                        .message(ec.getMessage())
                        .build());
    }
    //  Mọi lỗi còn lại  ->  500 (lưới an toàn cuối cùng)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Object>builder()
                        .code(500).message("Lỗi hệ thống: " + ex.getMessage()).build());
    }

}