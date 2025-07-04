package com.authentication.auth.dto.token;

import com.authentication.auth.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class PrincipalDetails implements UserDetails, OAuth2User {

    private final User user;
    private Map<String, Object> attributes;

    public PrincipalDetails(User user) {
        this.user = user;
    }

    public PrincipalDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }

    public String getNickname() {
        return user.getNickname();
    }

    /**
     * @deprecated use getEmail() via getUsername() instead. This method now returns the user's email for backward compatibility.
     */
    @Deprecated
    public String getUserId() {
        return user.getEmail();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return getUsername();
    }

    /**
     * Returns the authentication provider name (e.g., google, kakao).
     * If the user logged in with normal credentials, returns "server".
     */
    public String getProviderType() {
        // Lazy guard – authentications may be uninitialized or empty
        if (user.getAuthentications() == null || user.getAuthentications().isEmpty()) {
            return "server";
        }

        try {
            return user.getAuthentications().get(0).getAuthProvider().getProviderName();
        } catch (Exception e) {
            // Fallback for any unexpected null pointers
            return "server";
        }
    }
}
