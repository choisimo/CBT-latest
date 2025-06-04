# 6장: 트랜잭션 관리

## 소개

트랜잭션 관리는 엔터프라이즈 애플리케이션의 중요한 측면으로, 특히 데이터베이스 작업 중에 데이터 일관성과 무결성을 보장합니다. 이 문서는 Auth-Server 프로젝트에서 트랜잭션 관리가 구현되는 방식, 주로 Spring 프레임워크의 강력한 트랜잭션 관리 기능을 활용하는 방법을 설명합니다.

Spring Boot는 클래스 경로에서 Spring Data JPA와 `DataSource`를 감지하면 트랜잭션 관리를 자동 구성하며, 이는 이 프로젝트의 일반적인 설정입니다. 명시적인 `@EnableTransactionManagement` 어노테이션은 발견되지 않았지만 트랜잭션 지원은 암시적으로 활성화됩니다.

## 핵심 개념 (@Transactional)

이 프로젝트에서 선언적 트랜잭션 관리의 주요 메커니즘은 일반적으로 서비스 계층의 메서드에 적용되는 `@Transactional` 어노테이션입니다.

### 전파 수준

*   **기본 전파 (`Propagation.REQUIRED`):**
    코드베이스에서 관찰된 모든 `@Transactional` 어노테이션은 기본 전파 수준인 `Propagation.REQUIRED`를 사용합니다. 이는 다음을 의미합니다.
    *   `REQUIRED` 메서드가 호출될 때 이미 트랜잭션이 존재하는 경우 해당 메서드는 기존 트랜잭션 내에서 실행됩니다.
    *   트랜잭션이 존재하지 않으면 Spring은 메서드 실행을 위해 새 트랜잭션을 생성합니다.
    이는 서비스 계층의 대부분 사용 사례에 적합하며 서비스 작업이 일관된 트랜잭션 단위의 일부임을 보장합니다.

*   **기타 전파 수준:**
    현재 다른 전파 수준(예: `REQUIRES_NEW`, `NESTED`, `SUPPORTS`)이 명시적으로 사용된 증거는 없습니다. 기본 `REQUIRED` 수준이 기존 트랜잭션 요구 사항을 충족하는 것으로 보입니다.

### 롤백 규칙

*   **기본 롤백 동작:**
    `@Transactional`의 기본 롤백 동작은 활성 상태입니다.
    *   트랜잭션 메서드 내에서 `RuntimeException` 또는 `Error`가 발생하면 트랜잭션이 자동으로 롤백됩니다.
    *   검사된 `Exception`이 발생하면 트랜잭션이 자동으로 롤백되지 **않습니다**.

*   **명시적 롤백 구성:**
    *   조사된 서비스 클래스(예: `UserService`, `EmailService`, `TokenService`, `Oauth2Service`)의 `@Transactional` 어노테이션에서는 명시적인 `rollbackFor` 또는 `noRollbackFor` 속성이 관찰되지 않았습니다.
    *   **중요 참고:** `UserService.join()` 및 `UserService.UpdateUserPassword()`와 같은 일부 메서드에는 `try-catch (Exception e)` 블록이 포함됩니다. 이러한 블록 내에서 검사된 `Exception`(또는 `RuntimeException`이 아닌 모든 `Exception`)이 포착되고 `RuntimeException`으로 다시 던져지지 않으면 오류에도 불구하고 트랜잭션이 커밋될 수 있습니다. 이는 표준 Spring 동작이며 의도한 트랜잭션 결과를 보장하기 위해 개발 및 검토 중에 고려해야 합니다. 예를 들어, `UserService.join()`에서 `repository.save(newUser)`가 성공하지만 `try` 블록 내의 후속 작업에서 포착되는 비-RuntimeException이 발생하는 경우 사용자 생성이 커밋될 수 있습니다.

### 읽기 전용 트랜잭션

*   **목적 및 이점:**
    `@Transactional(readOnly = true)` 속성은 트랜잭션이 읽기 작업만 수행함을 나타내는 데 사용됩니다. 이는 런타임에 성능 최적화(예: 데이터베이스 드라이버 힌트, JPA 플러시 모드 설정)를 제공하고 우발적인 데이터 수정이 발생하지 않도록 보장할 수 있습니다.

*   **프로젝트에서의 사용:**
    현재 조사된 주요 서비스 클래스에서 `@Transactional(readOnly = true)`의 명시적인 사용은 관찰되지 않았습니다. 읽기 작업만 수행하는 메서드(예: `UserService.getEmailByUserId()`, `UserService.checkUserNameIsDuplicate()`, `EmailService.checkIsExistEmail()`)는 `readOnly = true` 없이 `@Transactional`로 어노테이트됩니다. 이러한 메서드에 `readOnly = true`를 적용하는 것은 잠재적인 성능 향상과 더 명확한 의도를 위해 권장되는 관행입니다.

## 서비스 계층 전략

트랜잭션은 일반적으로 서비스 계층에서 구분되어 비즈니스 작업이 트랜잭션 컨텍스트 내에서 실행되도록 보장합니다. 데이터를 수정하거나 여러 리포지토리 호출에 걸쳐 일관성이 필요한 서비스 메서드는 `@Transactional`로 어노테이트됩니다.

**일반 코드 예제:**

```java
// 프로젝트의 일반적인 패턴을 기반으로 한 예제
package com.authentication.auth.service.users;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Spring의 Transactional

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    // ... 기타 종속성 ...

    @Transactional // 기본값: REQUIRED, RuntimeException/Error 발생 시 롤백
    public User createUser(JoinRequest joinRequest) {
        if (userRepository.existsByUserName(joinRequest.userId())) {
            throw new RuntimeException("사용자 ID가 이미 존재합니다"); // 롤백을 위한 RuntimeException 예제
        }
        User user = User.builder()
                .userName(joinRequest.userId())
                .password(passwordEncoder.encode(joinRequest.userPw()))
                .email(joinRequest.email())
                // ... 기타 필드 ...
                .build();
        return userRepository.save(user);
    }

    // 읽기 전용 메서드의 경우 (readOnly = true) 권장
    @Transactional // 이상적으로는 @Transactional(readOnly = true)여야 함
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUserName(username);
    }

    @Transactional
    public void updateUserPassword(String userId, String newPassword) {
        User user = userRepository.findByUserName(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다")); // 사용자를 찾을 수 없는 경우 롤백 보장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
```

## 비동기 작업 및 트랜잭션

현재 코드베이스 검사에 따르면 `@Async` 어노테이션은 `@Transactional` 메서드와 함께 사용되지 않습니다. 따라서 비동기 경계를 넘어 트랜잭션을 관리하기 위한 고려 사항(일반적으로 비동기 메서드에 대한 별도의 트랜잭션 컨텍스트 포함)은 현재 이 프로젝트에 적용되지 않습니다. 나중에 트랜잭션 동작이 필요한 `@Async` 메서드가 도입되면 신중한 설계가 필요합니다.

## 다중 데이터베이스 트랜잭션

이 프로젝트는 단일 기본 데이터 소스로 작동하는 것으로 보입니다. 코드베이스 검사에서는 여러 `DataSource` 빈에 대한 구성이나 일반적으로 여러 데이터베이스에 걸친 분산 트랜잭션에 사용되는 Atomikos 또는 Bitronix와 같은 JTA(Java Transaction API) 트랜잭션 관리자의 사용이 발견되지 않았습니다. 따라서 모든 트랜잭션 작업은 이 단일 데이터 소스에 대해 수행되는 것으로 가정합니다.

## 모범 사례/고려 사항

*   **트랜잭션 짧게 유지:** 잠금 경합을 최소화하고 데이터베이스 성능을 향상시키기 위해 트랜잭션은 가능한 한 짧게 유지해야 합니다.
*   **장기 실행 작업 방지:** 장기 실행 작업(예: 복잡한 계산, 트랜잭션 롤백을 지원하지 않는 외부 API 호출)을 데이터베이스 트랜잭션 내에 직접 포함하지 마십시오. 이러한 작업이 필요한 경우 기본 트랜잭션 외부에서 수행하거나 이벤트 기반 패턴을 사용하는 것을 고려하십시오.
*   **전파 이해:** 기존 트랜잭션 내에서 다른 트랜잭션 메서드를 호출할 때 `Propagation.REQUIRED`(기본값)가 어떻게 동작하는지 유의하십시오. 내부 메서드는 외부 트랜잭션에 참여합니다.
*   **롤백 동작:** 필요한 경우 롤백을 트리거하도록 예외가 적절하게 처리되는지 확인하십시오. 자동 롤백을 위해 `RuntimeException`에 의존하는 것이 일반적이지만 검사된 예외가 롤백을 트리거해야 하는 경우 `rollbackFor`를 사용하거나 `RuntimeException`으로 다시 던져 처리하십시오.
*   **`readOnly = true` 사용:** 데이터만 읽는 메서드의 경우 의도를 알리고 잠재적으로 성능 이점을 얻기 위해 `@Transactional(readOnly = true)`를 적용하십시오.
---
