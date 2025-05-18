package com.career_block.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.DTO.users.joinRequest;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.redis.redisService;
import com.authentication.auth.service.smtp.emailService;
import com.authentication.auth.service.users.userService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "User Management", description = "유저 관리 API")
public class usersController {

    @Value("${file.profile-path}")
    private String profilePath;
    @Value("${file.server}")
    private String fileServer;
    @Value("${site.domain}")
    private String domain;
    private final userService userService;
    private final emailService emailService;
    private final redisService redisService;
    private final jwtUtility jwtUtility;

    @Operation(summary = "회원 가입", description = "새로운 유저를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 가입 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "회원 가입 실패", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/join")
    public ResponseEntity<?> join(@RequestBody joinRequest request) throws Exception {
        if (!redisService.checkEmailCode(request.getEmail(), request.getCode()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email validation is not accessed");
        ResponseEntity<?> save = userService.join(request);
        if (save.getStatusCode() == HttpStatus.CONFLICT)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("already exist userId or nickname");
        if (save.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.status(HttpStatus.OK).body("join successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("join save failed");
        }
    }

    @Operation(summary = "프로필 업로드", description = "유저 프로필 이미지를 업로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 업로드 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 이름 또는 확장자", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/profileUpload")
    public ResponseEntity<?> fileUpload(@RequestParam("profile") MultipartFile[] files) {
        Map<String, String> response = new HashMap<>();
        for (MultipartFile file : files) {
            try {
                String originName = file.getOriginalFilename();
                if (originName == null || originName.contains("..")) {
                    log.error("invalid file name : " + originName);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid file name : " + originName);
                }

                String originNameOnly = FilenameUtils.getBaseName(originName);
                String extension = FilenameUtils.getExtension(originName);
                if (!isValidExtension(extension)) {
                    log.error("invalid file extension : " + extension);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid file extension : " + extension);
                }

                String fileName = UUID.randomUUID().toString() + "_" + originNameOnly + "." + extension;

                File directory = new File(profilePath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File targetFile = new File(profilePath, fileName);
                file.transferTo(targetFile);

                log.info("Saving file to {}", targetFile.getAbsolutePath());

                if (!isValidFileContent(targetFile, extension)) {
                    targetFile.delete();
                    log.error("invalid file content");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid file content");
                }

                log.info("saving file dir to {}", targetFile.getAbsolutePath());

                response.put("fileName", fileServer + "/attach/profile/" + fileName);

            } catch (IOException e) {
                log.error("프로필 파일 업로드 중 오류 발생!", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("profile upload error");
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    private boolean isValidExtension(String extension) {
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff");
        return allowedExtensions.contains(extension);
    }

    private boolean isValidFileContent(File file, String extension) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            List<String> allowedMimeTypes = Arrays.asList(
                    "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml", "image/tiff"
            );
            return allowedMimeTypes.contains(mimeType);
        } catch (IOException e) {
            log.error("파일 내용 검증 중 오류 발생", e);
            return false;
        }
    }

    @Operation(summary = "아이디 중복 체크", description = "아이디의 중복 여부를 체크합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 체크 성공", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/check/userId/IsDuplicate")
    public boolean checkUserIdIsDuplicate(@RequestBody HashMap<String, String> user) {
        log.info("/check/userId/IsDuplicate : {}", user.get("userId"));
        return userService.checkUserIdIsDuplicate(user.get("userId"));
    }

    @Operation(summary = "닉네임 중복 체크", description = "닉네임의 중복 여부를 체크합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 체크 성공", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/check/nickname/IsDuplicate")
    public boolean checkNickNameDuplicate(@RequestBody HashMap<String, String> user) {
        log.info("/check/nickname/IsDuplicate : {}", user.get("nickname"));
        return userService.checkNickNameIsDuplicate(user.get("nickname"));
    }

    @PostMapping("/public/clean/userTokenCookie")
    public ResponseEntity<?> cleanUserTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = "refreshToken";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }
            }
        }
        return ResponseEntity.ok().body("refreshToken deleted");
    }

    private void sendFrontNewCookie(HttpServletResponse response, int status, tokenDto tokendto) {
        response.setStatus(status);
        response.addHeader(SecurityConstants.TOKEN_HEADER, SecurityConstants.TOKEN_PREFIX + tokendto.getAccessToken());
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokendto.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setDomain(domain);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        // cookie 배열 가지고 오기
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        log.error("[getRefreshTokenFromCookie] cookie 에서 refreshToken 찾기 실패");
        return null;
    }

    private boolean RedisMatchRToken(String userId, String RToken) {
        return redisService.findRToken(userId, "server", RToken);
    }
}