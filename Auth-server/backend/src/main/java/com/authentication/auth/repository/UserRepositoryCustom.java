package com.authentication.auth.repository;

import java.util.List;

public interface UserRepositoryCustom {
    long updatePassword(String userId, String newPassword);
    List<String> findAllEmail();
}
