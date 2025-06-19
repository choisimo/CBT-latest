package com.authentication.auth.service.users;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.redis.RedisService; // RedisService import
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
    private final RedisService redisService; // RedisService 주입

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

        // 이메일 인증 코드 검증은 Controller에서 수행 후 호출되는 것으로 가정.
        // 여기서는 단순히 WAITING 상태로 사용자를 저장.

        try {
            User newUser = User.builder()
                    .userName(request.userId())
                    .password(passwordEncoder.encode(request.userPw()))
                    .email(request.email())
                    .userRole(request.role() != null ? request.role() : "USER")
                    .isPremium(false)
                    .isActive("WAITING") // 초기 상태는 WAITING
                    .build();
            repository.save(newUser);
            log.info("회원가입 요청 성공 (상태: WAITING): {}", newUser.getUserName());
            return newUser;
        } catch (Exception e) {
            log.error("회원 가입 처리 중 오류 발생", e);
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR, "회원 가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public void activateUser(String userId, String email) {
        User user = repository.findByUserName(userId)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));
        
        user.activate(); // User 엔티티에 activate 메소드 추가 가정 (isActive = "ACTIVE")
        repository.save(user);
        log.info("사용자 활성화 성공: {}", userId);

        // 인증 코드 삭제
        boolean codeDeleted = redisService.deleteEmailCode(email);
        if (codeDeleted) {
            log.info("Redis에서 이메일 인증 코드 삭제 성공: {}", email);
        } else {
            log.warn("Redis에서 이메일 인증 코드 삭제 실패 또는 해당 코드 없음: {}", email);
            // 이 경우, 이미 코드가 만료되었거나 다른 이유로 삭제되었을 수 있음.
            // 추가적인 오류 처리가 필요하다면 여기에 구현.
        }
    }

    @Transactional(readOnly = true) // getEmailByUserId는 읽기 전용이므로 readOnly = true 추가
    public String getEmailByUserId(String userId) {
        User user = repository.findByUserName(userId)
                .orElseThrow(() -> {
                    log.error("이메일 찾기 실패: 사용자 ID '{}'에 해당하는 사용자를 찾을 수 없습니다.", userId);
                    // null 대신 예외를 던지도록 수정 (NPE 방지 계획과 일치)
                    return new CustomException(ErrorType.USER_NOT_FOUND, "사용자 ID '" + userId + "'을(를) 찾을 수 없습니다.");
                });
        return user.getEmail();
    }

    @Transactional
    public void UpdateUserPassword(String userId, String temporalPassword) {
        User user = repository.findByUserName(userId)
                .orElseThrow(() -> {
                    log.error("비밀번호 변경 실패: 사용자 ID '{}'에 해당하는 사용자를 찾을 수 없습니다.", userId);
                    return new CustomException(ErrorType.USER_NOT_FOUND, "비밀번호 변경 대상 사용자를 찾을 수 없습니다: " + userId);
                });
        
        user.setPassword(passwordEncoder.encode(temporalPassword)); // User 엔티티에 setPassword 가정
        repository.save(user);
        log.info("임시 비밀번호로 package com.authentication.auth.service.users;\n" + //
                        "\n" + //
                        "import com.authentication.auth.domain.User;\n" + //
                        "import com.authentication.auth.dto.users.JoinRequest;\n" + //
                        "import com.authentication.auth.exception.CustomException;\n" + //
                        "import com.authentication.auth.exception.ErrorType;\n" + //
                        "import com.authentication.auth.repository.UserRepository;\n" + //
                        "import lombok.RequiredArgsConstructor;\n" + //
                        "import lombok.extern.slf4j.Slf4j;\n" + //
                        "import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;\n" + //
                        "import org.springframework.stereotype.Service;\n" + //
                        "import org.springframework.transaction.annotation.Transactional;\n" + //
                        "\n" + //
                        "@Slf4j\n" + //
                        "@Service\n" + //
                        "@RequiredArgsConstructor\n" + //
                        "public class UserService {\n" + //
                        "\n" + //
                        "    private final UserRepository repository;\n" + //
                        "    private final BCryptPasswordEncoder passwordEncoder;\n" + //
                        "\n" + //
                        "    @Transactional\n" + //
                        "    public User join(JoinRequest request) {\n" + //
                        "        if (repository.existsByUserName(request.userId())) {\n" + //
                        "            log.error(\"이미 존재하는 아이디 입니다: {}\", request.userId());\n" + //
                        "            throw new CustomException(ErrorType.USERNAME_ALREADY_EXISTS, \"이미 존재하는 아이디입니다: \" + request.userId());\n" + //
                        "        }\n" + //
                        "\n" + //
                        "        if (repository.existsByEmail(request.email())) {\n" + //
                        "            log.error(\"이미 존재하는 이메일 입니다: {}\", request.email());\n" + //
                        "            throw new CustomException(ErrorType.EMAIL_ALREADY_EXISTS, \"이미 존재하는 이메일입니다: \" + request.email());\n" + //
                        "        }\n" + //
                        "\n" + //
                        "        try {\n" + //
                        "            User newUser = User.builder()\n" + //
                        "                    .userName(request.userId())\n" + //
                        "                    .password(passwordEncoder.encode(request.userPw()))\n" + //
                        "                    .email(request.email())\n" + //
                        "                    .userRole(request.role() != null ? request.role() : \"USER\")\n" + //
                        "                    .isPremium(false)\n" + //
                        "                    .isActive(\"WAITING\")\n" + //
                        "                    .build();\n" + //
                        "            repository.save(newUser);\n" + //
                        "            log.info(\"회원가입 성공: {}\", newUser.getUserName());\n" + //
                        "            return newUser;\n" + //
                        "        } catch (Exception e) {\n" + //
                        "            log.error(\"회원 가입 실패\", e);\n" + //
                        "            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR, \"회원 가입 처리 중 오류가 발생했습니다.\", e);\n" + //
                        "        }\n" + //
                        "    }\n" + //
                        "\n" + //
                        "    @Transactional\n" + //
                        "    public String getEmailByUserId(String userId) {\n" + //
                        "        return repository.findByUserName(userId)\n" + //
                        "                .map(User::getEmail)\n" + //
                        "                .orElseGet(() -> {\n" + //
                        "                    log.error(\"이메일 찾기 실패: 사용자 ID '{}'에 해당하는 사용자를 찾을 수 없습니다.\", userId);\n" + //
                        "                    return null;\n" + //
                        "                });\n" + //
                        "    }\n" + //
                        "\n" + //
                        "    @Transactional\n" + //
                        "    public void UpdateUserPassword(String userId, String temporalPassword) {\n" + //
                        "        try {\n" + //
                        "            long updateCount = repository.updatePassword(userId, passwordEncoder.encode(temporalPassword));\n" + //
                        "            if (updateCount == 0)\n" + //
                        "                throw new Exception(\"비밀번호 변경 실패! 사용자를 찾을 수 없음\");\n" + //
                        "        } catch (Exception e) {\n" + //
                        "            log.error(\"비밀번호 변경 실패\", e);\n" + //
                        "        }\n" + //
                        "    }\n" + //
                        "\n" + //
                        "    @Transactional\n" + //
                        "    public boolean checkUserNameIsDuplicate(String userName) {\n" + //
                        "        return repository.existsByUserName(userName);\n" + //
                        "    }\n" + //
                        "\n" + //
                        "}\n" + //
                        "사용자 비밀번호 변경 성공: {}", userId);
        // try-catch 제거, updatePassword JPQL 대신 엔티티 수정 방식으로 변경하여 명확성 증대
    }

    @Transactional(readOnly = true) // checkUserNameIsDuplicate는 읽기 전용
    public boolean checkUserNameIsDuplicate(String userName) {
        return repository.existsByUserName(userName);
    }

}
