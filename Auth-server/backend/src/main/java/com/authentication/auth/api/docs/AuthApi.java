package com.authentication.auth.api.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Authentication Status", description = "인증 상태 확인 API")
public interface AuthApi {

    @Operation(summary = "인증 상태 확인 (테스트용)", 
               description = "현재 요청이 유효한 인증 토큰을 가지고 있는지 확인합니다. Swagger에서 테스트 시에는 'Authorize' 버튼을 통해 Bearer Token을 먼저 설정해야 합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증됨 (Authorized)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않음 (Unauthorized)")
    })
    @GetMapping("/auth_check")
    ResponseEntity<String> authCheck();
}
