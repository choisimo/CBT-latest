package com.career_block.auth.repository;

import com.authentication.auth.domain.User;
import com.authentication.auth.domain.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface usersRepository extends JpaRepository<users, Long> {

    User findByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByUserIdOrNickname(String userId, String nickname);
    boolean existsByNickname(String nickname);

    boolean existsByUserId(String userId);

    @Modifying
    @Transactional
    @Query("UPDATE users u SET u.userPw = :password WHERE u.userId = :userId")
    int updatePassword(String userId, String password);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
