package com.authentication.auth.exception;

import com.authentication.auth.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 애플리케이션에서 발생하는 모든 예외를 중앙에서 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        log.error("CustomException occurred: {}", e.getMessage(), e);
        
        ErrorType errorType = e.getErrorType();
        HttpStatus status = HttpStatus.valueOf(errorType.getStatusCode());
        
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            errorType,
            Map.of("details", e.getMessage())
        );
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 파일 크기 제한 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File size exceeds maximum limit: {}", e.getMessage());
        
        // Assuming ErrorType has a FILE_TOO_LARGE enum constant
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            ErrorType.FILE_TOO_LARGE, 
            Map.of("details", "File size exceeds maximum limit")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 유효성 검사 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation failed: {}", e.getMessage());
        
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors = new ArrayList<>();
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            ErrorType.GENERAL_ERROR, // Or a more specific ErrorType.VALIDATION_ERROR if available
            Map.of("details", "Validation failed", "errors", errors)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException occurred: {}", e.getMessage(), e);
        
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            ErrorType.INVALID_REQUEST, 
            Map.of("details", e.getMessage())
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 일반적인 Runtime 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected RuntimeException occurred: {}", e.getMessage(), e);
        
        // Assuming ErrorType has an INTERNAL_SERVER_ERROR enum constant
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            ErrorType.INTERNAL_SERVER_ERROR, 
            Map.of("details", "An unexpected error occurred")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 모든 예외의 최종 처리자
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred: {}", e.getMessage(), e);
        
        // Assuming ErrorType has an INTERNAL_SERVER_ERROR enum constant
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            ErrorType.INTERNAL_SERVER_ERROR, 
            Map.of("details", "An unexpected error occurred")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
