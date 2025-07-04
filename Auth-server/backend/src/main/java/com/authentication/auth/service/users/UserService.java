package com.authentication.auth.service.users;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.users.UserResponseDto;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.redis.RedisService; // RedisService import
import com.authentication.auth.configuration.token.JwtUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisService redisService; // RedisService 주입
    private final JwtUtility jwtUtility; // JwtUtility 주입

    @Transactional
    public User join(JoinRequest request) {
        // 아이디(userId) 기반 중복 체크 제거: 이메일만 단일 식별자로 사용
        if (repository.existsByEmail(request.email())) {
            log.error("이미 존재하는 이메일 입니다: {}", request.email());
            throw new CustomException(ErrorType.EMAIL_ALREADY_EXISTS, "이미 존재하는 이메일입니다: " + request.email());
        }

        // 이메일 인증 코드 검증은 Controller에서 수행 후 호출되는 것으로 가정.
        // 여기서는 단순히 WAITING 상태로 사용자를 저장.

        try {
            User newUser = User.builder()
                    .nickname(request.nickname())
                    .password(passwordEncoder.encode(request.userPw()))
                    .email(request.email())
                    .loginId(request.loginId())
                    .isPremium(false)
                    .isActive("WAITING") // 초기 상태는 WAITING
                    .build();
            repository.save(newUser);
            log.info("회원가입 요청 성공 (상태: WAITING): email={} loginId={}", newUser.getEmail(), newUser.getLoginId());
            return newUser;
        } catch (Exception e) {
            log.error("회원 가입 처리 중 오류 발생", e);
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR, "회원 가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier();
        User user = repository.findByEmail(identifier)
                .orElseGet(() -> repository.findByLoginId(identifier)
                        .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + identifier)));
        
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorType.INVALID_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
        
        // 토큰 생성
        TokenDto tokenDto = jwtUtility.buildToken(
            user.getNickname(), 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserRole()))
        );
        
        // 사용자 정보 생성
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(),
            user.getNickname(),
            user.getEmail()
        );
        
        return new LoginResponse(tokenDto.accessToken(), userInfo);
    }

    @Transactional
    public void activateUser(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + email));
        
        user.activate(); // User 엔티티에 activate 메소드 추가 가정 (isActive = "ACTIVE")
        repository.save(user);
        log.info("사용자 활성화 성공: {}", email);

        // 인증 코드 삭제
        boolean codeDeleted = redisService.deleteEmailCode(email);
        if (codeDeleted) {
            log.info("Redis에서 이메일 인증 코드 삭제 성공: {}", email);
        } else {
            log.warn("Redis에서 이메일 인증 코드 삭제 실패 또는 해당 코드 없음: {}", email);
        }
    }

    @Transactional(readOnly = true) // getEmailByUserId는 읽기 전용이므로 readOnly = true 추가
    public String getEmailByUserId(String email) {
        // 메서드 이름을 유지하되, 실제로는 email을 받아 처리하도록 변경
        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("이메일 찾기 실패: '{}'와 일치하는 사용자를 찾을 수 없습니다.", email);
                    return new CustomException(ErrorType.USER_NOT_FOUND, "해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email);
                });
        return user.getEmail();
    }

    @Transactional
    public void UpdateUserPassword(String email, String temporalPassword) {
        User user = repository.findByEmail(email)
                .orElseThrow(() ->
                    new CustomException(ErrorType.USER_NOT_FOUND,
                                        "비밀번호 변경 대상 사용자를 찾을 수 없습니다: " + email));
    
        user.setPassword(passwordEncoder.encode(temporalPassword));
        repository.save(user);
        log.info("사용자 비밀번호 변경 성공: {}", email);
    }
    

    @Transactional(readOnly = true)
    public boolean checkUserIdIsDuplicate(Integer userId) {
        return repository.existsById(userId.longValue());
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameIsDuplicate(String nickname) {
        return repository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true) // checkLoginIdIsDuplicate는 읽기 전용
    public boolean checkLoginIdIsDuplicate(String loginId) {
        return repository.existsByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public UserResponseDto findByLoginId(String username) {
        log.info("Finding user for /api/users/me by username: {}", username);
        User user = repository.findByLoginId(username)
                .orElseGet(() -> repository.findByEmail(username)
                        .orElseThrow(() -> {
                            log.error("User not found by loginId or email: {}", username);
                            return new CustomException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + username);
                        }));
        log.info("User found for /api/users/me: {}", user.getLoginId());
        return new UserResponseDto(user.getId(), user.getNickname(), user.getEmail(), user.getLoginId(), user.getIsPremium(), user.getUserRole());
    }

}
