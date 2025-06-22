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
        log.info("login identifier : {}", identifier);

        // 먼저 이메일로 조회하고, 없으면 로그인 ID로 조회한다.
        User user = repository.findByEmail(identifier)
                .orElseGet(() -> repository.findByLoginId(identifier)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with email or loginId: " + identifier)));

        return new PrincipalDetails(user);
    }
}
