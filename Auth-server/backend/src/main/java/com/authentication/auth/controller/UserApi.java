package com.authentication.auth.controller;


import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.UserNameCheckRequestDto;
import com.authentication.auth.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Tag(name = "3. User Management API", description = "사용자 가입, 프로필, 중복체크, 토큰 정리 관련 API")
public interface UserApi {

    @Operation(summary = "회원 가입", description = "새로운 유저를 등록합니다. 이메일 인증 코드가 사전에 검증되어야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 가입 성공", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = Map.class),
                                            examples = @ExampleObject(name = "회원가입 성공 응답", value = "{\"message\": \"join successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "이메일 미인증 또는 잘못된 요청", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @ExampleObject(name = "잘못된 요청 또는 이메일 미인증", value = "{\"timestamp\": \"2023-10-27T10:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Email validation is not complete or request is malformed.\", \"path\": \"/api/public/join\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 아이디", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @ExampleObject(name = "아이디 중복 오류", value = "{\"timestamp\": \"2023-10-27T10:01:00Z\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"User ID 'existingUser123' already exists.\", \"path\": \"/api/public/join\"}"))),
            @ApiResponse(responseCode = "500", description = "회원 가입 실패 (서버 오류)", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @ExampleObject(name = "서버 오류 응답", value = "{\"timestamp\": \"2023-10-27T10:02:00Z\", \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Failed to save user information due to a server error.\", \"path\": \"/api/public/join\"}")))
    })
    ResponseEntity<com.authentication.auth.dto.response.ApiResponse<String>> join(@RequestBody(description = "회원 가입 정보", required = true,
            content = @Content(mediaType = "application/json",
                               schema = @Schema(implementation = JoinRequest.class),
                               examples = {
                                   @ExampleObject(
                                       name = "일반 사용자 가입 예시",
                                       summary = "모든 필수 및 선택 정보를 포함한 일반적인 회원가입 요청입니다.",
                                       value = "{\"userId\": \"newUser123\", \"userPw\": \"Password123!\", \"userName\": \"홍길동\", \"nickname\": \"쾌활한 다람쥐\", \"phone\": \"010-1234-5678\", \"email\": \"user@example.com\", \"role\": \"USER\", \"birthDate\": \"1990-01-01\", \"gender\": \"MALE\", \"isPrivate\": false, \"profile\": \"https://example.com/profile.jpg\", \"code\": \"A1B2C3\"}"
                                   ),
                                   @ExampleObject(
                                       name = "필수 항목 누락 예시 (비밀번호 누락)",
                                       summary = "필수 항목인 비밀번호(userPw)가 누락된 경우의 요청입니다. (400 Bad Request 예상)",
                                       value = "{\"userId\": \"incompleteUser\", \"userName\": \"김미영\", \"nickname\": \"조용한 고양이\", \"phone\": \"010-5555-4444\", \"email\": \"incomplete@example.com\", \"code\": \"D4E5F6\"}"
                                   ),
                                   @ExampleObject(
                                       name = "형식 오류 예시 (이메일 형식 오류)",
                                       summary = "이메일 형식이 올바르지 않은 경우의 요청입니다. (400 Bad Request 예상)",
                                       value = "{\"userId\": \"badEmailUser\", \"userPw\": \"Password123!\", \"userName\": \"박형식\", \"nickname\": \"똑똑한 부엉이\", \"phone\": \"010-7777-8888\", \"email\": \"bademailformat\", \"code\": \"G7H8I9\"}"
                                   )
                               }
                              )
            ) JoinRequest request) throws Exception;

    @Operation(summary = "프로필 이미지 업로드", description = "유저 프로필 이미지를 업로드하고 이미지 URL을 반환받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 업로드 성공", 
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class, example = "{\"fileName\": \"https://your-file-server.com/attach/profile/xxxx_profile.jpg\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 이름, 확장자 또는 내용", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류 (파일 업로드 실패)", content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<com.authentication.auth.dto.response.ApiResponse<Map<String, String>>> fileUpload(
            @Parameter(description = "업로드할 프로필 이미지 파일 (멀티파트)", required = true,
                       content = @Content(mediaType = "multipart/form-data"))
            MultipartFile[] files);

    @Operation(summary = "사용자명 중복 체크", description = "제공된 사용자명(로그인 ID)이 이미 사용 중인지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 체크 결과 (true: 중복됨, false: 사용 가능)",
                         content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean")))
    })
    ResponseEntity<com.authentication.auth.dto.response.ApiResponse<Boolean>> checkUserNameIsDuplicate(
            @RequestBody(description = "확인할 사용자명 정보", required = true,
                         content = @Content(schema = @Schema(implementation = UserNameCheckRequestDto.class)))
            UserNameCheckRequestDto requestDto);

    @Operation(summary = "사용자 ID 중복 체크", description = "제공된 사용자 ID가 이미 사용 중인지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 체크 결과", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = Boolean.class),
                                            examples = {
                                                @ExampleObject(name = "ID 중복됨", summary = "User ID Duplicate", value = "true"),
                                                @ExampleObject(name = "ID 사용 가능", summary = "User ID Available", value = "false")
                                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = @ExampleObject(name = "잘못된 요청 응답", value = "{\"timestamp\": \"2023-10-27T10:25:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Required request body is missing or userId is not provided.\", \"path\": \"/api/public/check/userId/IsDuplicate\"}")))
    })
    ResponseEntity<com.authentication.auth.dto.response.ApiResponse<Boolean>> checkUserIdIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto);

    @Operation(summary = "사용자 토큰 쿠키 정리 (로그아웃)", description = "클라이언트의 refreshToken 쿠키를 만료시켜 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠키 정리 성공", 
                         content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"message\": \"refreshToken deleted\"}")))
    })
    ResponseEntity<com.authentication.auth.dto.response.ApiResponse<String>> cleanUserTokenCookie(HttpServletRequest request, HttpServletResponse response);
}
