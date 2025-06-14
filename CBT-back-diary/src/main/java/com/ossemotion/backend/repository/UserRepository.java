package com.ossemotion.backend.repository;

import com.ossemotion.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { // ID type is Long
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName); // For finding by nickname/user_name
}
