package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 *  idx_email : email 컬럼에 대한 인덱스 -> email 검색 시 성능 향상 (unique constraint)
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_email", columnList = "email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /**
     * 사용자 로그인 ID 및 대표 닉네임 (DB의 user_name 컬럼)
     */
    @Column(name = "login_id", nullable = false, length = 50, unique = true)
    private String loginId;

    @Column(name = "nickname", nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Builder.Default
    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole = "USER";

    @Builder.Default
    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false, length = 20)
    private String isActive = "ACTIVE";

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Diary> diaries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuthentication> authentications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCustomSetting> customSettings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Method to activate user
    public void activate() {
        this.isActive = "ACTIVE";
    }
    
    // UserService에서 사용하는 메서드들 추가
    public String getUserNickname() {
        return this.nickname;
    }
}
