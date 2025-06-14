package com.authentication.auth.service.users;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public User join(JoinRequest request) {
        if (repository.existsByUserName(request.userId())) {
            log.error("이미 존재하는 아이디 입니다: {}", request.userId());
            throw new CustomException(ErrorType.USERNAME_ALREADY_EXISTS, "이미 존재하는 아이디입니다: " + request.userId());
        }

        if (repository.existsByEmail(request.email())) {
            log.error("이미 존재하는 이메일 입니다: {}", request.email());
            throw new CustomException(ErrorType.EMAIL_ALREADY_EXISTS, "이미 존재하는 이메일입니다: " + request.email());
        }

        try {
            User newUser = User.builder()
                    .userName(request.userId())
                    .password(passwordEncoder.encode(request.userPw()))
                    .email(request.email())
                    .userRole(request.role() != null ? request.role() : "USER")
                    .isPremium(false)
                    .isActive("WAITING")
                    .build();
            repository.save(newUser);
            log.info("회원가입 성공: {}", newUser.getUserName());
            return newUser;
        } catch (Exception e) {
            log.error("회원 가입 실패", e);
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR, "회원 가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public String getEmailByUserId(String userId) {
        return repository.findByUserName(userId)
                .map(User::getEmail)
                .orElseGet(() -> {
                    log.error("이메일 찾기 실패: 사용자 ID '{}'에 해당하는 사용자를 찾을 수 없습니다.", userId);
                    return null;
                });
    }

    @Transactional
    public void UpdateUserPassword(String userId, String temporalPassword) {
        try {
            long updateCount = repository.updatePassword(userId, passwordEncoder.encode(temporalPassword));
            if (updateCount == 0)
                throw new Exception("비밀번호 변경 실패! 사용자를 찾을 수 없음");
        } catch (Exception e) {
            log.error("비밀번호 변경 실패", e);
        }
    }

    @Transactional
    public boolean checkUserNameIsDuplicate(String userName) {
        return repository.existsByUserName(userName);
    }

}
