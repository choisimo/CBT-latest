package com.ossemotion.backend.repository;

import com.ossemotion.backend.entity.UserAuthentication;
import com.ossemotion.backend.entity.UserAuthenticationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAuthenticationRepository extends JpaRepository<UserAuthentication, UserAuthenticationId> {
    // Find by User's ID (Long type)
    // JpaRepository's findById expects UserAuthenticationId. We need a custom query or derived method.
    // Option 1: Find all auth methods for a given user id
    List<UserAuthentication> findByUserId(Long userId);

    // Option 2: More specific - find by User's ID and provider name (if needed)
    @Query("SELECT ua FROM UserAuthentication ua WHERE ua.user.id = :userId AND ua.authProvider.providerName = :providerName")
    Optional<UserAuthentication> findByUserIdAndProviderName(@Param("userId") Long userId, @Param("providerName") String providerName);
    
    // To get the provider type for a user, we'd typically fetch UserAuthentication records by user.id
    // and then get the providerName from the associated AuthProvider entity.
    // This method specifically gets the first (or primary) UserAuthentication record for a user.
    Optional<UserAuthentication> findFirstByUserId(Long userId);
}
