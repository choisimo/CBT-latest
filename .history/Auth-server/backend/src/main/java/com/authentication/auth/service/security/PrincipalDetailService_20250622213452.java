package com.authentication.auth.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.domain.User;
import com.authentication.auth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalDetailService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.info("사용자 인증 시도 - identifier: '{}'", identifier);

        // 유효하지 않은 식별자 값 검증
        if (identifier == null || identifier.isBlank() || 
            "NONE_PROVIDED".equalsIgnoreCase(identifier) ||
            "null".equalsIgnoreCase(identifier) ||
            "undefined".equalsIgnoreCase(identifier)) {
            log.warn("유효하지 않은 식별자로 인증 시도: '{}'", identifier);
            throw new UsernameNotFoundException("유효하지 않은 식별자입니다: " + identifier);
        }

        // 먼저 이메일로 조회하고, 없으면 로그인 ID로 조회한다.
        User user = repository.findByEmail(identifier)
                .orElseGet(() -> repository.findByLoginId(identifier)
                        .orElseThrow(() -> {
                            log.warn("사용자를 찾을 수 없음 - identifier: '{}'", identifier);
                            return new UsernameNotFoundException("User not found with email or loginId: " + identifier);
                        }));

        log.info("사용자 인증 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        return new PrincipalDetails(user);
    }
}
