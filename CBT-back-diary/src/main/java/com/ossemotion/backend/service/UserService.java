package com.ossemotion.backend.service;

import com.ossemotion.backend.dto.UserDto;
import com.ossemotion.backend.entity.User;
import com.ossemotion.backend.entity.UserAuthentication;
import com.ossemotion.backend.repository.UserAuthenticationRepository;
import com.ossemotion.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
// import org.springframework.security.core.userdetails.UsernameNotFoundException; // Using a generic exception for now
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthenticationRepository userAuthenticationRepository;

    // Temporary mock user ID for `/api/users/me` before full auth integration
    // This should match an ID in your Users table if you want to test DB retrieval
    private static final Long MOCK_USER_ID_LONG = 1L; // Assuming ID 1 exists or will be created for testing

    @Transactional(readOnly = true)
    public UserDto getCurrentUserDetails() {
        // In a real scenario, we'd get the userId from Spring Security Context's Principal
        // For now, using MOCK_USER_ID_LONG.

        Optional<User> userOpt = userRepository.findById(MOCK_USER_ID_LONG);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Fetch the first UserAuthentication record to determine providerType
            // A user might have multiple linked accounts, here we simplify by taking the first one.
            Optional<UserAuthentication> userAuthOpt = userAuthenticationRepository.findFirstByUserId(user.getId());
            String providerType = userAuthOpt.map(ua -> ua.getAuthProvider().getProviderName()).orElse("NORMAL");
            
            boolean emailVerified = true; // Placeholder, actual status might come from User.isActive or a dedicated field/service

            return UserDto.builder()
                    .userId(user.getId().toString()) // Convert Long to String for DTO
                    .nickname(user.getUserName()) // or user.getNickname()
                    .email(user.getEmail())
                    .emailVerified(emailVerified) 
                    .providerType(providerType)
                    .role(user.getUserRole()) // or user.getRole()
                    .build();
        } else {
            // Fallback to a completely hardcoded user if MOCK_USER_ID_LONG is not found in DB
            // This helps test the endpoint even with an empty database or no mock user seeded.
            return UserDto.builder()
                    .userId(MOCK_USER_ID_LONG.toString())
                    .nickname("Mock User")
                    .email("mock@example.com")
                    .emailVerified(true)
                    .providerType("NORMAL")
                    .role("USER")
                    .build();
        }
    }
}
