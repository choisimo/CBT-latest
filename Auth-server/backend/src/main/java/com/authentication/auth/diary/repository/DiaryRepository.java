package main.java.com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByIdAndUser(Long diaryId, User user);

    Page<Diary> findAllByUser(User user, Pageable pageable);
}
