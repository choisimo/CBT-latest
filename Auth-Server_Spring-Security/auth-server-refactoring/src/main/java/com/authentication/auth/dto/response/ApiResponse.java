package com.authentication.auth.dto.response;

import com.authentication.auth.constants.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * API 응답 형식을 표준화하는 레코드
 * @param status 상태 (success/error)
 * @param message 메시지
 * @param data 응답 데이터
 * @param timestamp 타임스탬프
 */
public record ApiResponse<T>(
    String status,
    String message,
    T data,
    LocalDateTime timestamp
) {
    /**
     * 성공 응답 생성
     * @param data 응답 데이터
     * @param message 메시지
     * @return API 응답
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", message, data, LocalDateTime.now());
    }
    
    /**
     * 성공 응답 생성 (기본 메시지)
     * @param data 응답 데이터
     * @return API 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다");
    }
    
    /**
     * 오류 응답 생성
     * @param errorType 오류 유형
     * @param details 추가 세부 정보
     * @return API 응답
     */
    public static ApiResponse<Map<String, Object>> error(ErrorType errorType, Map<String, Object> details) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", errorType.name());
        errorData.put("title", errorType.getTitle());
        if (details != null) {
            errorData.put("details", details);
        }
        
        return new ApiResponse<>("error", errorType.getMessage(), errorData, LocalDateTime.now());
    }
    
    /**
     * 오류 응답 생성 (세부 정보 없음)
     * @param errorType 오류 유형
     * @return API 응답
     */
    public static ApiResponse<Map<String, Object>> error(ErrorType errorType) {
        return error(errorType, null);
    }
    
    /**
     * ResponseEntity로 변환
     * @param httpStatus HTTP 상태
     * @return ResponseEntity
     */
    public ResponseEntity<ApiResponse<T>> toResponseEntity(HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(this);
    }
    
    /**
     * 성공 응답을 ResponseEntity로 변환 (HTTP 200)
     * @return ResponseEntity
     */
    public ResponseEntity<ApiResponse<T>> toSuccessResponseEntity() {
        return toResponseEntity(HttpStatus.OK);
    }
    
    /**
     * 오류 응답을 ResponseEntity로 변환
     * @param errorType 오류 유형
     * @return ResponseEntity
     */
    public static ResponseEntity<ApiResponse<Map<String, Object>>> toErrorResponseEntity(ErrorType errorType) {
        return error(errorType).toResponseEntity(errorType.getStatus());
    }
}
