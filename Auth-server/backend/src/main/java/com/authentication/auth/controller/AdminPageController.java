package com.authentication.auth.controller;

import com.authentication.auth.filter.FilterRegistry;
import com.authentication.auth.filter.PluggableFilter;
import com.authentication.auth.filter.FilterCondition;
import com.authentication.auth.filter.PathPatternFilterCondition; // Assuming this is the primary type
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Protect the entire controller
public class AdminPageController {

    private final FilterRegistry filterRegistry;

    // DTOs similar to AdminFilterController for consistency with template
    // These can be moved to a common DTO package if used elsewhere
    public record FilterListResponse(List<FilterInfo> filters) {}
    public record FilterInfo(String filterId, String filterClassName, List<ConditionInfo> conditions) {}
    public record ConditionInfo(String id, String description, Set<String> patterns, Set<String> methods) {}

    private ConditionInfo mapConditionToInfo(FilterCondition condition) {
        if (condition instanceof PathPatternFilterCondition pathCondition) {
            return new ConditionInfo(
                    pathCondition.getId(),
                    pathCondition.description(),
                    pathCondition.patterns(),
                    pathCondition.methods().stream().map(HttpMethod::name).collect(Collectors.toSet())
            );
        } else {
            // For generic or other types of conditions, provide basic info
            return new ConditionInfo(
                    condition.getId(),
                    condition.getDescription(),
                    Set.of(), // Or some other representation if applicable
                    Set.of()
            );
        }
    }

    @GetMapping("/filter-ui")
    @Hidden // Hide from Swagger UI as it's a UI page
    public String getFilterManagementPage(Model model) {
        Map<String, PluggableFilter> filters = filterRegistry.getFilters();
        List<FilterInfo> filterInfos = filters.values().stream()
                .map(filter -> {
                    List<ConditionInfo> conditions = filterRegistry.getConditionsForFilter(filter.getFilterId()).stream()
                            .map(this::mapConditionToInfo)
                            .collect(Collectors.toList());
                    return new FilterInfo(filter.getFilterId(), filter.getClass().getSimpleName(), conditions);
                })
                .collect(Collectors.toList());
        
        model.addAttribute("filterListResponse", new FilterListResponse(filterInfos));
        return "admin/filter-management"; // Path to the Thymeleaf template
    }

    @GetMapping("/dashboard") // main dashboard path
    @Hidden // Hide from Swagger UI as it's a UI page
    public String getAdminDashboardPage(Model model) {
        // Model attributes can be added here if needed for the dashboard
        return "admin/index"; // Path to the new Thymeleaf admin index template
    }

    /**
     * Root mapping for /admin ("/admin" or "/admin/") to avoid NoResourceFoundException.
     * Redirects to the dashboard page so that users hitting the base admin URL see the dashboard.
     */
    @GetMapping({"", "/"})
    @Hidden
    public String redirectAdminRoot() {
        return "redirect:/admin/dashboard";
    }


}
