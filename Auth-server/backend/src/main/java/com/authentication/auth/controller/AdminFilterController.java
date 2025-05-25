package com.authentication.auth.controller;

import com.authentication.auth.filter.FilterCondition;
import com.authentication.auth.filter.FilterRegistry;
import com.authentication.auth.filter.PathPatternFilterCondition;
import com.authentication.auth.filter.PluggableFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/filters")
@Tag(name = "Admin Filter API", description = "관리자용 필터 관리 API")
public class AdminFilterController {

    private final FilterRegistry filterRegistry;

    @Operation(
            summary = "등록된 모든 필터 조회",
            description = "현재 시스템에 등록된 모든 플러그형 필터와 각 필터에 적용된 조건 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "필터 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FilterListResponse.class),
                            examples = @ExampleObject(name = "필터 목록 예시", value = "{\"filters\":[{\"filterId\":\"jwtVerificationFilter\",\"conditions\":[{\"id\":\"uuid1\",\"description\":\"public paths\",\"patterns\":[\"/public/**\"],\"methods\":[]}]}]}"))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "인증 실패 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/admin/filters\"}"))
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "권한 없음 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access Denied\",\"path\":\"/admin/filters\"}"))
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<FilterListResponse> getAllFilters() {
        Map<String, PluggableFilter> filters = filterRegistry.getFilters();
        List<FilterInfo> filterInfos = filters.values().stream()
                .map(filter -> {
                    List<ConditionInfo> conditions = filterRegistry.getConditionsForFilter(filter.getFilterId()).stream()
                            .map(this::mapConditionToInfo)
                            .collect(Collectors.toList());
                    return new FilterInfo(filter.getFilterId(), filter.getClass().getSimpleName(), conditions);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(new FilterListResponse(filterInfos));
    }

    @Operation(
            summary = "특정 필터에 조건 추가",
            description = "지정된 필터에 새로운 PathPatternFilterCondition을 추가합니다. \n" +
                    "요청 본문에 경로 패턴과 HTTP 메서드를 포함해야 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조건 추가 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "조건 추가 성공 예시", value = "{\"message\":\"Condition added successfully to filter jwtVerificationFilter\"}"))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid filter ID or request body\",\"path\":\"/admin/filters/jwtVerificationFilter/conditions\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "필터를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "필터 없음 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Filter with ID jwtVerificationFilter not found\",\"path\":\"/admin/filters/jwtVerificationFilter/conditions\"}"))
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{filterId}/conditions")
    public ResponseEntity<MessageResponse> addConditionToFilter(
            @Parameter(description = "조건을 추가할 필터의 고유 ID", example = "jwtVerificationFilter")
            @PathVariable String filterId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "추가할 조건 정보 (PathPatternFilterCondition)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddConditionRequest.class),
                            examples = @ExampleObject(name = "조건 추가 요청 예시", value = "{\"description\":\"Allow public access\",\"patterns\":[\"/public/**\"],\"methods\":[\"GET\",\"POST\"]}")
                    )
            )
            @RequestBody AddConditionRequest request) {

        if (!filterRegistry.getFilters().containsKey(filterId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Filter with ID " + filterId + " not found"));
        }

        PathPatternFilterCondition condition = new PathPatternFilterCondition(
                request.description(),
                request.methods().toArray(new HttpMethod[0]),
                request.patterns().toArray(new String[0])
        );
        filterRegistry.addCondition(filterId, condition);
        return ResponseEntity.ok(new MessageResponse("Condition added successfully to filter " + filterId));
    }

    @Operation(
            summary = "특정 필터에서 조건 제거",
            description = "지정된 필터에서 특정 조건 ID를 가진 조건을 제거합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조건 제거 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "조건 제거 성공 예시", value = "{\"message\":\"Condition uuid1 removed successfully from filter jwtVerificationFilter\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "필터 또는 조건을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "필터/조건 없음 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Condition with ID uuid1 not found for filter jwtVerificationFilter\",\"path\":\"/admin/filters/jwtVerificationFilter/conditions/uuid1\"}"))
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{filterId}/conditions/{conditionId}")
    public ResponseEntity<MessageResponse> removeConditionFromFilter(
            @Parameter(description = "조건을 제거할 필터의 고유 ID", example = "jwtVerificationFilter")
            @PathVariable String filterId,
            @Parameter(description = "제거할 조건의 고유 ID", example = "uuid1")
            @PathVariable String conditionId) {

        if (!filterRegistry.getFilters().containsKey(filterId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Filter with ID " + filterId + " not found"));
        }

        boolean removed = filterRegistry.removeCondition(filterId, conditionId);
        if (removed) {
            return ResponseEntity.ok(new MessageResponse("Condition " + conditionId + " removed successfully from filter " + filterId));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Condition with ID " + conditionId + " not found for filter " + filterId));
        }
    }

    @Operation(
            summary = "필터 활성화/비활성화",
            description = "특정 필터의 활성화 상태를 변경합니다. \n" +
                    "활성화 시 모든 요청에 대해 필터가 적용되도록 하고, 비활성화 시 모든 요청에 대해 필터가 적용되지 않도록 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "필터 상태 변경 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "필터 활성화 성공 예시", value = "{\"message\":\"Filter jwtVerificationFilter enabled\"}"))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid action: unknown\",\"path\":\"/admin/filters/jwtVerificationFilter/status\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "필터를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "필터 없음 예시", value = "{\"timestamp\":\"2023-01-01T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Filter with ID jwtVerificationFilter not found\",\"path\":\"/admin/filters/jwtVerificationFilter/status\"}"))
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{filterId}/status")
    public ResponseEntity<MessageResponse> setFilterStatus(
            @Parameter(description = "상태를 변경할 필터의 고유 ID", example = "jwtVerificationFilter")
            @PathVariable String filterId,
            @Parameter(description = "변경할 상태 (enable 또는 disable)", example = "enable")
            @RequestParam String action,
            HttpServletRequest httpRequest) {

        if (!filterRegistry.getFilters().containsKey(filterId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Filter with ID " + filterId + " not found"));
        }

        String enableConditionId = filterId + "-enable-condition";
        String disableConditionId = filterId + "-disable-condition";

        switch (action.toLowerCase()) {
            case "enable":
                // Remove any existing disable condition
                filterRegistry.removeCondition(filterId, disableConditionId);
                // Add a condition that always returns false for shouldNotFilter (i.e., always apply filter)
                filterRegistry.addCondition(filterId, new FilterCondition() {
                    @Override
                    public String getId() { return enableConditionId; }
                    @Override
                    public String getDescription() { return "Always apply filter (enabled by admin)"; }
                    @Override
                    public boolean shouldNotFilter(HttpServletRequest request) { return false; }
                });
                return ResponseEntity.ok(new MessageResponse("Filter " + filterId + " enabled"));
            case "disable":
                // Remove any existing enable condition
                filterRegistry.removeCondition(filterId, enableConditionId);
                // Add a condition that always returns true for shouldNotFilter (i.e., never apply filter)
                filterRegistry.addCondition(filterId, new FilterCondition() {
                    @Override
                    public String getId() { return disableConditionId; }
                    @Override
                    public String getDescription() { return "Never apply filter (disabled by admin)"; }
                    @Override
                    public boolean shouldNotFilter(HttpServletRequest request) { return true; }
                });
                return ResponseEntity.ok(new MessageResponse("Filter " + filterId + " disabled"));
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Invalid action: " + action));
        }
    }

    // --- Helper classes for API responses and requests ---

    @Schema(description = "필터 목록 응답")
    public record FilterListResponse(
            @Schema(description = "등록된 필터 정보 목록")
            List<FilterInfo> filters
    ) {}

    @Schema(description = "필터 정보")
    public record FilterInfo(
            @Schema(description = "필터 고유 ID", example = "jwtVerificationFilter")
            String filterId,
            @Schema(description = "필터 클래스 이름", example = "JwtVerificationFilter")
            String filterClassName,
            @Schema(description = "필터에 적용된 조건 목록")
            List<ConditionInfo> conditions
    ) {}

    @Schema(description = "조건 정보")
    public record ConditionInfo(
            @Schema(description = "조건 고유 ID", example = "uuid1")
            String id,
            @Schema(description = "조건 설명", example = "Public API paths")
            String description,
            @Schema(description = "적용된 경로 패턴 목록", example = "[\"/api/public/**\"]")
            Set<String> patterns,
            @Schema(description = "적용된 HTTP 메소드 목록", example = "[\"GET\", \"POST\"]")
            Set<String> methods
    ) {}

    @Schema(description = "조건 추가 요청")
    public record AddConditionRequest(
            @Schema(description = "조건 설명", example = "Allow unauthenticated access to login")
            String description,
            @Schema(description = "적용할 경로 패턴 목록", example = "[\"/auth/login\", \"/auth/join\"]")
            Set<String> patterns,
            @Schema(description = "적용할 HTTP 메소드 목록", example = "[\"POST\"]")
            Set<HttpMethod> methods
    ) {}

    @Schema(description = "메시지 응답")
    public record MessageResponse(
            @Schema(description = "응답 메시지", example = "Operation successful")
            String message
    ) {}

    @Schema(description = "오류 응답")
    public record ErrorResponse(
            @Schema(description = "타임스탬프", example = "2023-01-01T12:00:00Z")
            String timestamp,
            @Schema(description = "HTTP 상태 코드", example = "400")
            int status,
            @Schema(description = "오류 유형", example = "Bad Request")
            String error,
            @Schema(description = "오류 메시지", example = "Invalid input")
            String message,
            @Schema(description = "요청 경로", example = "/api/resource")
            String path
    ) {}

    private ConditionInfo mapConditionToInfo(FilterCondition condition) {
        if (condition instanceof PathPatternFilterCondition pathCondition) {
            return new ConditionInfo(
                    pathCondition.getId(),
                    pathCondition.description(),
                    pathCondition.patterns(),
                    pathCondition.methods().stream().map(HttpMethod::name).collect(Collectors.toSet())
            );
        } else {
            return new ConditionInfo(
                    condition.getId(),
                    condition.getDescription(),
                    Set.of(), // No patterns for generic condition
                    Set.of()  // No methods for generic condition
            );
        }
    }
}
