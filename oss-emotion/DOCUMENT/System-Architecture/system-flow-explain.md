1. 사용자가 웹/모바일 클라이언트를 통해 로그인
2. Nginx API Gateway를 통해 요청이 Spring Security Auth 서비스로 전달
3. Auth 서비스가 MariaDB에서 사용자 자격 증명 확인
4. **인증 세션/토큰 생성 후 저장** - 이 부분이 다이어그램에서 명확하지 않음
5. 클라이언트에게 인증 토큰 또는 세션 ID 반환

## Redis를 활용한 인증 정보 저장 필요성

### 인증 정보를 Redis에 저장해야 하는 이유

Redis는 인증 토큰 및 세션 저장소로 매우 적합합니다:

1. **빠른 액세스 속도**: Redis는 인메모리 데이터 스토어로, 인증 토큰 검증 시 빠른 응답을 제공합니다[12]
2. **세션 만료 관리**: Redis의 TTL(Time-To-Live) 기능을 사용하여 토큰/세션의 만료 시간을 효과적으로 관리할 수 있습니다[9][10]
3. **분산 환경 지원**: 여러 서버에서 동일한 인증 상태를 공유할 수 있어 로드 밸런싱 환경에서 필수적입니다[14]
4. **확장성**: 사용자 증가에 따라 쉽게 확장 가능합니다[12]

### Redis에 저장되는 인증 정보의 종류

1. **세션 데이터**: Spring Session을 사용할 경우 HttpSession 정보[2][6][11]
2. **인증 토큰**: OAuth2 토큰이나 JWT와 같은 인증 토큰[4][8][12]
3. **SecurityContext**: Spring Security의 인증 컨텍스트[14]

## 개선된 인증 정보 저장 아키텍처 제안

현재 다이어그램을 개선하여 명확한 인증 정보 저장 흐름을 추가할 필요가 있습니다:

### 인증 정보 저장 시점

1. **사용자 로그인 성공 직후**: 인증 서비스가 사용자 검증 후 세션/토큰 생성 시[14]
2. **토큰 갱신 시**: OAuth2 등의 방식에서 액세스 토큰 갱신 시[4]
3. **로그아웃 시**: 토큰/세션 무효화를 위해 해당 정보 삭제 또는 블랙리스트에 추가[8]

### 구현 방식

```java
// Spring Security와 Redis Session 구성 예시
@Configuration
@EnableRedisHttpSession
public class RedisSessionConfig {
    @Value("${spring.redis.host}")
    private String host;
    
    @Value("${spring.redis.port}")
    private int port;
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }
}
```

이 구성을 통해 Spring Security의 인증 정보가 자동으로 Redis에 저장됩니다[2][11][14].

### 보안 고려사항

Redis에 인증 정보를 저장할 때 다음 보안 사항을 고려해야 합니다:

1. **암호화**: Redis 연결 및 저장 데이터 암호화[13]
2. **패스워드 보호**: Redis 서버 자체에 인증 설정[3][13]
3. **네트워크 격리**: Redis 서버를 내부 네트워크에만 노출[3][13]
4. **적절한 TTL 설정**: 세션/토큰의 유효 기간 적절히 설정[10]
