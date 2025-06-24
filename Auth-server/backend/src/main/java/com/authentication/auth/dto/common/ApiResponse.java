// package com.authentication.auth.dto.common;

// import com.fasterxml.jackson.annotation.JsonInclude;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// /**
//  * 공통 API 응답 DTO
//  * 모든 API 응답에서 일관된 형식을 제공합니다.
//  * 
//  * @param <T> 응답 데이터의 타입
//  */
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @JsonInclude(JsonInclude.Include.NON_NULL)
// public class ApiResponse<T> {
    
//     /**
//      * 응답 성공 여부
//      */
//     private boolean success;
    
//     /**
//      * 응답 메시지
//      */
//     private String message;
    
//     /**
//      * 응답 데이터
//      */
//     private T data;
    
//     /**
//      * 에러 코드 (실패 시에만 포함)
//      */
//     private String errorCode;
    
//     /**
//      * 성공 응답 생성 (데이터 포함)
//      */
//     public static <T> ApiResponse<T> success(T data) {
//         return new ApiResponse<>(true, "Success", data, null);
//     }
    
//     /**
//      * 성공 응답 생성 (메시지와 데이터 포함)
//      */
//     public static <T> ApiResponse<T> success(String message, T data) {
//         return new ApiResponse<>(true, message, data, null);
//     }
    
//     /**
//      * 성공 응답 생성 (메시지만)
//      */
//     public static <T> ApiResponse<T> success(String message) {
//         return new ApiResponse<>(true, message, null, null);
//     }
    
//     /**
//      * 실패 응답 생성
//      */
//     public static <T> ApiResponse<T> failure(String message) {
//         return new ApiResponse<>(false, message, null, null);
//     }
    
//     /**
//      * 실패 응답 생성 (에러 코드 포함)
//      */
//     public static <T> ApiResponse<T> failure(String message, String errorCode) {
//         return new ApiResponse<>(false, message, null, errorCode);
//     }
    
//     /**
//      * 실패 응답 생성 (데이터 포함) 
//      *  --> 중복되므로 삭제됨
//      */
//     public static <T> ApiResponse<T> failure(String message, T data) {
//         return new ApiResponse<>(false, message, data, null);
//     }
// }
