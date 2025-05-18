package com.authentication.auth.DTO.token;


/**
 * @Author : choisimo
 * @Date : 2025.05.08
 * @Description : Token Data Transfer Object
 * @Detail : accessToken, refreshToken
 * @Refactor : change class type to record type
 * */

public record TokenDto(String accessToken, String refreshToken) {}

// same as
/**
 * class TokenDto {
 *     String accessToken;
 *     String refreshToken;
 *
 *     public TokenDto(String accessToken, String refreshToken) {
 *         this.accessToken = accessToken;
 *         this.refreshToken = refreshToken;
 *     }
 *
 *     public String getAccessToken() {
 *         return accessToken;
 *     }
 *
 *     public void setAccessToken(String accessToken) {
 *         this.accessToken = accessToken;
 *     }
 *
 *     public String getRefreshToken() {
 *         return refreshToken;
 *     }
 *
 *     public void setRefreshToken(String refreshToken) {
 *         this.refreshToken = refreshToken;
 *     }
 *
 *     public String toString() {
 *         return "TokenDto [accessToken=" + accessToken + ", refreshToken=" + refreshToken + "]";
 *     }
 *
 *     public int hashCode() {
 *
 *     }
 * }
 *
 * */
