package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Diary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // SQL: title VARCHAR(255) NULL -> nullable=true로 설정 (필수값 아님)
    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    // SQL: alternative_thought TEXT NULL -> 필드명 및 컬럼명 일치
    @Lob
    @Column(name = "alternative_thought", columnDefinition = "TEXT")
    private String alternativeThought;

    // SQL: is_negative BOOLEAN DEFAULT FALSE
    @Column(name = "is_negative")
    private Boolean isNegative;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
