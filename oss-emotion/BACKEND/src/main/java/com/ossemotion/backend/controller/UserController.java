package com.ossemotion.backend.controller;

import com.ossemotion.backend.dto.UserDto;
import com.ossemotion.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        // In a real app, Spring Security would provide the principal (current user).
        // The principal's identifier (e.g., username or ID) would then be passed to userService.
        // For now, userService.getCurrentUserDetails() uses a mock ID or hardcoded details.
        UserDto userDto = userService.getCurrentUserDetails();
        return ResponseEntity.ok(userDto);
    }
}

