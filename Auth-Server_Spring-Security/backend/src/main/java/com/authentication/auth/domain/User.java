package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole = UserRole.USER;

    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", nullable = false)
    private UserStatus isActive = UserStatus.WAITING;

    @OneToMany(mappedBy = "user")
    private List<Diary> diaries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserRole {
        USER, ADMIN
    }

    /**
     * 사용자 상태를 나타내는 열거형
     * schema.sql의 enum('active','waiting','blocked','suspend','delete') 값과 일치
     */
    public enum UserStatus {
        // DB 스키마의 값과 일치시키기 위해 소문자로 정의
        // JPA에서는 UPPERCASE로 변환하므로 @Enumerated(EnumType.STRING) 사용 시 주의 필요
        ACTIVE("active"), 
        WAITING("waiting"), 
        BLOCKED("blocked"), 
        SUSPEND("suspend"), 
        DELETE("delete");
        
        private final String value;
        
        UserStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
}
