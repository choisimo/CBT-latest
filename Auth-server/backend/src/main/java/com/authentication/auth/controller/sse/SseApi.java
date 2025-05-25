package com.authentication.auth.controller.sse;

import com.authentication.auth.dto.token.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "SSE API", description = "API for Server-Sent Events")
@RequestMapping("/api") // Common base path from SseController
public interface SseApi {

    @Operation(summary = "Subscribe to SSE events", description = "Subscribes to the server's real-time event stream.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SSE subscription successful",
                         content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Authentication failed",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.authentication.auth.dto.response.ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.authentication.auth.dto.response.ErrorResponse.class)))
    })
    @GetMapping(value = "/protected/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> subscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Parameter(name = "Last-Event-ID", description = "Last received event ID for resuming connection",
                       in = ParameterIn.HEADER, required = false)
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    );

    @Operation(summary = "Send dummy SSE data (test)", description = "Sends a dummy SSE event to a specified user. For testing purposes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dummy data sent successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request (e.g., missing message in payload)",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.authentication.auth.dto.response.ErrorResponse.class)))
    })
    @PostMapping("/public/dummyData/{userId}")
    ResponseEntity<Void> sendDummyData(
            @Parameter(description = "User ID to send the dummy event to", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "Payload containing the message", required = true,
                       schema = @Schema(type = "object", example = "{\"message\": \"Hello from server!\"}"))
            @RequestBody Map<String, String> payload
    );
}
