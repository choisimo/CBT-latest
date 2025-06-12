package com.authentication.auth.repository;

import com.authentication.auth.domain.UserAuthentication;
import com.authentication.auth.domain.UserAuthenticationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthenticationRepository extends JpaRepository<UserAuthentication, UserAuthenticationId> {
    Optional<UserAuthentication> findByAuthProvider_ProviderNameAndSocialId(String providerName, String socialId);
}
