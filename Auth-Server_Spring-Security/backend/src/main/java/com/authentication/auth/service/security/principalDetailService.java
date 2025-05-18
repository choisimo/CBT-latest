package com.career_block.auth.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.authentication.auth.DTO.token.principalDetails;
import com.authentication.auth.domain.users;
import com.authentication.auth.repository.usersRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class principalDetailService implements UserDetailsService {

    private final usersRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        users user = repository.findByUserId(username);
        return new principalDetails(user);
    }
}
