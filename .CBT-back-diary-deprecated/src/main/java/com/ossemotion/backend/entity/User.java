package com.ossemotion.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users") // Matches table name from mariadb-emotion.sql
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id; // Matches BIGINT AUTO_INCREMENT

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_name", unique = true, nullable = false, length = 50) // Maps to nickname in DTO
    private String userName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole; // Maps to role in DTO

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(name = "is_active", nullable = false, length = 20)
    @Builder.Default
    private String isActive = "ACTIVE";

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Builder default for new objects, DB default for existing

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(); // Builder default for new objects, DB default for existing

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) { // Should be handled by @Builder.Default or DB if column allows null
            createdAt = now;
        }
        if (updatedAt == null) { // Should be handled by @Builder.Default or DB if column allows null
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter for DTO mapping if nickname is preferred over userName
    public String getNickname() {
        return this.userName;
    }
    // Getter for DTO mapping if role is preferred over userRole
    public String getRole() {
        return this.userRole;
    }
}
