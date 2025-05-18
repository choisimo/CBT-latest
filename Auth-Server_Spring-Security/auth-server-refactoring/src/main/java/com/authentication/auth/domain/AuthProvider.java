package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Auth_Provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;
    
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "authProvider", cascade = CascadeType.ALL)
    private List<UserAuthentication> userAuthentications = new ArrayList<>();
    
    /**
     * 인증 제공자 유형을 나타내는 열거형
     */
    public enum ProviderType {
        SERVER("server", "내부 서버 인증"),
        GOOGLE("google", "구글 OAuth2"),
        KAKAO("kakao", "카카오 OAuth2"),
        NAVER("naver", "네이버 OAuth2"),
        FACEBOOK("facebook", "페이스북 OAuth2"),
        GITHUB("github", "깃허브 OAuth2");
        
        private final String code;
        private final String description;
        
        ProviderType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ProviderType fromCode(String code) {
            for (ProviderType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return SERVER; // 기본값
        }
    }
}
