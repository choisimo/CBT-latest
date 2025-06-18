package com.authentication.auth.repository;

import com.authentication.auth.domain.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, Integer> {
    Optional<AuthProvider> findByProviderName(String providerName);
}   