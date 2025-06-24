package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@Profile({"dev", "test"})
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Ensure it runs after tracing filters, but before most other filters.
@Slf4j
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final List<String> SENSITIVE_HEADERS = Arrays.asList("authorization", "cookie", "set-cookie");
    private static final List<String> SENSITIVE_JSON_FIELDS = Arrays.asList("password", "accessToken", "refreshToken", "token", "secret", "credentials");
    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile(
        "\"(" + String.join("|", SENSITIVE_JSON_FIELDS) + ")\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );
    private static final String MASKED_VALUE = "\"******\"";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for H2 console and Swagger UI to reduce noise
        String path = request.getRequestURI();
        if (path.startsWith("/h2-console") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8); // Short unique ID for this request

        logRequest(requestWrapper, requestId);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(responseWrapper, requestId, duration);
            responseWrapper.copyBodyToResponse(); // IMPORTANT: Ensure response body is written back
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        StringBuilder reqLog = new StringBuilder();
        reqLog.append("\n[REQUEST START - ").append(requestId).append("]\n");
        reqLog.append("URI         : ").append(request.getMethod()).append(" ").append(request.getRequestURI());
        if (request.getQueryString() != null) {
            reqLog.append("?").append(request.getQueryString());
        }
        reqLog.append("\n");
        reqLog.append("Headers     : ").append(maskSensitiveHeaders(Collections.list(request.getHeaderNames()), request::getHeader)).append("\n");
        
        String requestBody = getBody(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (!requestBody.isEmpty()) {
            reqLog.append("Request Body: ").append(maskSensitiveJsonFields(requestBody)).append("\n");
        }
        reqLog.append("[REQUEST END - ").append(requestId).append("]");
        log.info(reqLog.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, String requestId, long duration) {
        StringBuilder resLog = new StringBuilder();
        resLog.append("\n[RESPONSE START - ").append(requestId).append("]\n");
        resLog.append("Status      : ").append(response.getStatus()).append("\n");
        resLog.append("Headers     : ").append(maskSensitiveHeaders(response.getHeaderNames(), response::getHeader)).append("\n");
        
        String responseBody = getBody(response.getContentAsByteArray(), response.getCharacterEncoding());
        if (!responseBody.isEmpty()) {
            resLog.append("Response Body: ").append(maskSensitiveJsonFields(responseBody)).append("\n");
        }
        resLog.append("Duration    : ").append(duration).append(" ms\n");
        resLog.append("[RESPONSE END - ").append(requestId).append("]");
        log.info(resLog.toString());
    }

    private String getBody(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            return new String(content, encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to read body due to unsupported encoding: {}", encoding, e);
            return "[Could not read body: Unsupported encoding]";
        }
    }

    private String maskSensitiveHeaders(Iterable<String> headerNames, java.util.function.Function<String, String> headerValueResolver) {
        StringBuilder headersString = new StringBuilder("{");
        headerNames.forEach(headerName -> {
            if (headersString.length() > 1) {
                headersString.append(", ");
            }
            String value = headerValueResolver.apply(headerName);
            if (SENSITIVE_HEADERS.contains(headerName.toLowerCase())) {
                value = "***MASKED***";
            }
            headersString.append(headerName).append("=").append(value);
        });
        headersString.append("}");
        return headersString.toString();
    }

    private String maskSensitiveJsonFields(String body) {
        if (body == null || body.isEmpty() || !body.trim().startsWith("{") && !body.trim().startsWith("[")) {
            // Not JSON or empty, return as is
            return body;
        }
        Matcher matcher = JSON_FIELD_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // Group 1 is the field name, Group 2 is the original value
            matcher.appendReplacement(sb, "\"" + matcher.group(1) + "\":" + MASKED_VALUE);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false; // Log async dispatches as well
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false; // Log error dispatches as well
    }
}
