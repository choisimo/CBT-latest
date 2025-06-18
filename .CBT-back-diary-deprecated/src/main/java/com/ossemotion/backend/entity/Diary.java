package com.ossemotion.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Diary") // Matches table name from mariadb-emotion.sql
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false) // Matches BIGINT AUTO_INCREMENT from SQL
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Matches user_id BIGINT from SQL
    private User user;

    // Assuming DIARY_DATE is a logical date for the entry, separate from created_at
    // If this column is not in the DB, it needs to be added or mapped to created_at's date part.
    // For now, including as per prompt's entity structure.
    // If it's not in mariadb-emotion.sql, schema update (ddl-auto=update) will add it.
    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(name = "title", length = 255) // SQL allows NULL, DTO implies non-blank for creation
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") // Matches content TEXT from SQL
    private String content;

    @Lob
    @Column(name = "alternative_thought", columnDefinition = "TEXT") // Matches alternative_thought TEXT from SQL
    private String aiAlternativeThoughts;

    @Column(name = "is_negative") // Matches is_negative BOOLEAN from SQL
    private Boolean isNegative; // Using Boolean to allow null if DB column allows, or map from default

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
             createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // If diaryDate is meant to default to today when not provided
        // if (diaryDate == null) {
        //     diaryDate = LocalDate.now();
        // }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter for DTO mapping to String ID
    public String getStringId() {
        return this.id != null ? this.id.toString() : null;
    }
}
