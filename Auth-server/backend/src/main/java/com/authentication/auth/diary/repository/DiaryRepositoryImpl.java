package main.java.com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.QDiary;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DiaryRepositoryImpl implements DiaryRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QDiary diary = QDiary.diary;

    @Override
    public Optional<Diary> findByIdAndUserId(Long diaryId, Long userId) {
        Diary result = queryFactory
                .selectFrom(diary)
                .where(
                        diary.id.eq(diaryId),
                        diary.user.id.eq(userId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Diary> findAllByUserId(Long userId, Pageable pageable) {
        // 1. 데이터 조회 쿼리 (내용)
        List<Diary> content = queryFactory
                .selectFrom(diary)
                .where(diary.user.id.eq(userId))
                .orderBy(diary.createdAt.desc()) // 최신순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 전체 카운트 조회 쿼리 (페이징을 위해 필요)
        Long total = queryFactory
                .select(diary.count())
                .from(diary)
                .where(diary.user.id.eq(userId))
                .fetchOne();

        long count = (total == null) ? 0L : total;

        // 3. Page 구현체로 반환
        return new PageImpl<>(content, pageable, count);
    }
}
