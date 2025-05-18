package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Diary_Answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "emotion_detection")
    private String emotionDetection;

    @Column(name = "automatic_thought")
    private String automaticThought;

    @Column(name = "prompt_for_change")
    private String promptForChange;

    @Column(name = "alternative_thought")
    private String alternativeThought;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmotionStatus status = EmotionStatus.NEUTRAL;

    public enum EmotionStatus {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
