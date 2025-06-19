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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("email : " + email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // The orElseThrow above will throw if user is not found, so this check is technically redundant
        // but kept for explicit clarity if needed in future debugging or logic extension.
        if (user == null) { 
            log.error("User lookup by email returned null, which should not happen if orElseThrow is working as expected: " + email);
            // Defensive coding: ensure UsernameNotFoundException is thrown if somehow orElseThrow didn't.
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return new PrincipalDetails(user);
    }
}
