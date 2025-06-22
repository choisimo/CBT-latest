package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auth_provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "provider_name", nullable = false, length = 50, unique = true)
    private String providerName;

    @Column(nullable = true)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "authProvider", cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserAuthentication> userAuthentications = new ArrayList<>();
}
