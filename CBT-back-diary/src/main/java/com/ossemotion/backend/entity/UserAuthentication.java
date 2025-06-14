package com.ossemotion.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "User_Authentication")
@IdClass(UserAuthenticationId.class) // Using IdClass for composite key
public class UserAuthentication {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_provider_id", referencedColumnName = "id", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "social_id", nullable = false) // Provider's unique ID for the user
    private String socialId;

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
}

// Separate IdClass for the composite key
class UserAuthenticationId implements Serializable {
    private Long user; // Matches the type of User.id
    private Integer authProvider; // Matches the type of AuthProvider.id

    // equals and hashCode methods are essential for composite keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAuthenticationId that = (UserAuthenticationId) o;
        if (!user.equals(that.user)) return false;
        return authProvider.equals(that.authProvider);
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + authProvider.hashCode();
        return result;
    }
}
