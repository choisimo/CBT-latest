package com.authentication.auth.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Protect the entire controller
public class AdminPageController {

    @GetMapping("/dashboard") // main dashboard path
    @Hidden // Hide from Swagger UI as it's a UI page
    public ResponseEntity<?> getAdminDashboardPage() {
        // Model attributes can be added here if needed for the dashboard
        return ResponseEntity.ok(Map.of("message", "Admin dashboard data would be here."));
    }

    /**
     * Root mapping for /admin ("/admin" or "/admin/") to avoid NoResourceFoundException.
     * Redirects to the dashboard page so that users hitting the base admin URL see the dashboard.
     */
    @GetMapping({"", "/"})
    @Hidden
    public ResponseEntity<?> redirectAdminRoot() {
        return ResponseEntity.ok(Map.of(
            "message", "Admin root. Client should navigate to the admin dashboard.",
            "navigateTo", "/admin/dashboard"
        ));
    }
}
