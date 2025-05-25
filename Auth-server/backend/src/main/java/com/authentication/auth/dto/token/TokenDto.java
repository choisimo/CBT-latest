package com.authentication.auth.dto.token;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @Author : choisimo
 * @Date : 2025.05.08
 * @Description : Token Data Transfer Object
 * @Detail : accessToken, refreshToken
 * @Refactor : change class type to record type
 */

public record TokenDto(
    @Schema(description = "발급된 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJyb2xlIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxNjE4NzY3NH0.verylongaccesstokenexample")
    String accessToken,
    @Schema(description = "발급된 리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJpYXQiOjE3MTYxODU4NzQsImV4cCI6MTcxODc3Nzg3NH0.verylongrefreshtokenexample")
    String refreshToken
) {
}

// same as
/**
 * class TokenDto {
 * String accessToken;
 * String refreshToken;
 *
 * public TokenDto(String accessToken, String refreshToken) {
 * this.accessToken = accessToken;
 * this.refreshToken = refreshToken;
 * }
 *
 * public String getAccessToken() {
 * return accessToken;
 * }
 *
 * public void setAccessToken(String accessToken) {
 * this.accessToken = accessToken;
 * }
 *
 * public String getRefreshToken() {
 * return refreshToken;
 * }
 *
 * public void setRefreshToken(String refreshToken) {
 * this.refreshToken = refreshToken;
 * }
 *
 * public String toString() {
 * return "TokenDto [accessToken=" + accessToken + ", refreshToken=" +
 * refreshToken + "]";
 * }
 *
 * public int hashCode() {
 *
 * }
 * }
 *
 */
