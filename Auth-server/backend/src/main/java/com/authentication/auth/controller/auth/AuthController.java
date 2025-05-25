package com.authentication.auth.controller.auth;

import com.authentication.auth.api.docs.AuthApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    @Override
    public ResponseEntity<String> authCheck() {
        return ResponseEntity.ok("Authorized");
    }
}