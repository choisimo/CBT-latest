package com.authentication.auth.repository;

import com.authentication.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // For backward compatibility with tests
    default Optional<User> findByUserName(String userName) {
        return findByLoginId(userName);
    }

}
