package com.authentication.auth.repository;

import com.authentication.auth.domain.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QUser user = QUser.user;

    @Override
    @Transactional
    public long updatePassword(String userId, String newPassword) {
        return queryFactory
                .update(user)
                .set(user.password, newPassword)
                .where(user.loginId.eq(userId))
                .execute();
    }

    @Override
    public List<String> findAllEmail() {
        return queryFactory
                .select(user.email)
                .from(user)
                .fetch();
    }
}
