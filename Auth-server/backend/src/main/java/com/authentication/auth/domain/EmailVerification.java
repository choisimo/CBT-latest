// package com.authentication.auth.domain;

// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// import java.time.LocalDateTime;

// @Entity
// @Table(name = "Email_Verification")
// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class EmailVerification {

// @Id
// @GeneratedValue(strategy = GenerationType.IDENTITY)
// private Long id;

// @Column(nullable = false, unique = true, length = 255)
// private String email;

// @Column(name = "verification_code", nullable = false, length = 10)
// private String verificationCode;

// @Column(name = "expires_at", nullable = false)
// private LocalDateTime expiresAt;

// @Column(name = "created_at", nullable = false, updatable = false)
// private LocalDateTime createdAt;

// @Builder.Default
// @Column(name = "is_verified", nullable = false)
// private Boolean isVerified = false;

// @PrePersist
// protected void onCreate() {
// createdAt = LocalDateTime.now();
// }
// }
