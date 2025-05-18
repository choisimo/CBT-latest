package com.authentication.auth.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/auth_check")
    public ResponseEntity<String> authCheck() {
        return ResponseEntity.ok("Authorized");
    }
}