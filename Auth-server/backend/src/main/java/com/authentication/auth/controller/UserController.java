package com.authentication.auth.controller;

import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.users.UserResponseDto;
import com.authentication.auth.service.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(Principal principal) {
        String userId = principal.getName();
        UserResponseDto userResponseDto = userService.findByLoginId(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponseDto, "사용자 정보 조회 성공"));
    }
}
