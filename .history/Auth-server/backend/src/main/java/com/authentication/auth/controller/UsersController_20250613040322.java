package com.authentication.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.common.ApiResponse;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.UserNameCheckRequestDto;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.file.FileService;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.smtp.EmailService;
import com.authentication.auth.service.users.UserService;

import java.util.*;

/**
 * 사용자 관리 컨트롤러
 * 회원가입, 프로필 업로드, 중복 체크 등을 처리합니다.
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UsersController implements UserApi {

    @Value("${file.profile-path}")
    private String profilePath;
    @Value("${file.server}")
    private String fileServer;
    @Value("${site.domain}")
    private String domain;
    private final UserService userService;
    private final EmailService emailService;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;

    @Override
    @PostMapping("/public/join")
    public ResponseEntity<?> join(@RequestBody JoinRequest request) throws Exception {
        if (!redisService.checkEmailCode(request.email(), request.code()))
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

    @Override
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
                    "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml", "image/tiff");
            return allowedMimeTypes.contains(mimeType);
        } catch (IOException e) {
            log.error("파일 내용 검증 중 오류 발생", e);
            return false;
        }
    }

    @Override
    @PostMapping("/public/check/nickname/IsDuplicate")
    public boolean checkUserNameIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userName/IsDuplicate : {}", requestDto.userName());
        return userService.checkUserNameIsDuplicate(requestDto.userName());
    }

    @Override
    @PostMapping("/public/check/userId/IsDuplicate")
    public ResponseEntity<Boolean> checkUserIdIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userId/IsDuplicate : {}", requestDto.userName());
        // Assuming userId and userName are effectively the same for duplication checks
        return ResponseEntity.ok(userService.checkUserNameIsDuplicate(requestDto.userName()));
    }

    @Override
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

    private void sendFrontNewCookie(HttpServletResponse response, int status, TokenDto tokendto) {
        response.setStatus(status);
        response.addHeader(SecurityConstants.TOKEN_HEADER.getValue(), SecurityConstants.TOKEN_PREFIX.getValue() + tokendto.accessToken());
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokendto.refreshToken());
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