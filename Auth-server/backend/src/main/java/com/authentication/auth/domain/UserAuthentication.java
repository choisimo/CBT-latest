package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "User_Authentication", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"auth_provider_id", "social_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthentication {
    @EmbeddedId
    private UserAuthenticationId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("authProviderId")
    @JoinColumn(name = "auth_provider_id")
    private AuthProvider authProvider;
    
    @Column(name = "social_id", nullable = false, length = 255)
    private String socialId;
    
    @Column(name = "created_at", nullable = false)
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
