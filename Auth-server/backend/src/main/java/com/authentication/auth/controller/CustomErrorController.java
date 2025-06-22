package com.authentication.auth.controller;

import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("javax.servlet.error.status_code");
        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        
        // API 요청인지 확인 (JSON 응답 필요)
        boolean isApiRequest = requestURI.startsWith("/api/") || 
                              (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));
        
        int statusCode = 500; // 기본값
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }
        
        if (isApiRequest) {
            // API 요청인 경우 JSON 응답 반환
            return handleApiError(statusCode);
        } else {
            // 웹 페이지 요청인 경우 기존 HTML 템플릿 반환
            return handleWebError(statusCode, model);
        }
    }
    
    private ResponseEntity<ApiResponse<?>> handleApiError(int statusCode) {
        ErrorType errorType;
        HttpStatus httpStatus;
        
        switch (statusCode) {
            case 404:
                errorType = ErrorType.RESOURCE_NOT_FOUND;
                httpStatus = HttpStatus.NOT_FOUND;
                break;
            case 401:
                errorType = ErrorType.AUTHENTICATION_FAILED;
                httpStatus = HttpStatus.UNAUTHORIZED;
                break;
            case 403:
                errorType = ErrorType.FORBIDDEN_ACTION;
                httpStatus = HttpStatus.FORBIDDEN;
                break;
            case 500:
            default:
                errorType = ErrorType.INTERNAL_SERVER_ERROR;
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }
        
        ApiResponse<Map<String, Object>> response = ApiResponse.error(
            errorType,
            Map.of("details", "HTTP " + statusCode + " Error")
        );
        
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    private String handleWebError(int statusCode, Model model) {
        model.addAttribute("statusCode", statusCode);
        
        switch (statusCode) {
            case 404:
                return "notExist";  // src/main/resources/templates/notExist.html 템플릿 반환
            case 401:
                return "unauthorized"; // src/main/resources/templates/unauthorized.html 템플릿 반환
            default:
                return "error"; // 다른 오류에는 기본 에러 페이지 반환
        }
    }

    @RequestMapping("/errorPage")
    public String handleErrorPage(HttpServletRequest request, Model model) {
        return handleWebError(500, model); // 기본 웹 에러 페이지 처리
    }
}

