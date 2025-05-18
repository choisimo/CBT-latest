# 인증(Authentication) 및 인가(Authorization) 필터 시스템

## 개요
이 시스템은 Jakarta EE와 Spring Security를 기반으로 하며, 플러그형 필터 아키텍처를 사용하여 인증 및 인가 로직을 분리하고 확장 가능하게 구현합니다.

## 플러그형 필터 아키텍처

### 구조적 특징
- `PluggableFilter` 인터페이스: 모든 필터의 기본 인터페이스로, 필터 순서와 의존성을 정의
- `filterRegistry`: 필터 등록 및 순서 관리를 위한 클래스
- 위상 정렬(Topological Sort): 필터 간 의존성을 고려한 실행 순서 결정

### 주요 인증/인가 필터

#### 1. 인증(Authentication) 필터
- **AuthenticationFilter**: 사용자 로그인 및 자격 증명 검증 담당
- **JwtVerificationFilter**: JWT 토큰 유효성 검증 담당
- **SnsRequestFilter**: 소셜 로그인 요청 처리 담당

#### 2. 인가(Authorization) 필터
- **AuthorizationFilter**: 사용자 권한 확인 및 접근 제어 담당
- **RoleBasedAccessFilter**: 역할 기반 리소스 접근 제어 담당

## 필터 적용 흐름
1. 요청 접수
2. 인증(Authentication) 필터를 통한 사용자 신원 확인
   - JWT 토큰 검증 또는 소셜 로그인 인증
   - 인증 성공 시 `SecurityContext`에 인증 정보 설정
3. 인가(Authorization) 필터를 통한 권한 확인
   - 사용자 역할 및 권한 확인
   - 접근 가능한 리소스인지 검증
4. 적절한 권한이 있는 경우 요청 처리, 없는 경우 접근 거부

## 필터 등록 방법

필터를 등록하려면 다음 단계를 따르세요:

1. `PluggableFilter` 인터페이스를 구현한 필터 클래스 작성
2. 필터의 우선순위(`getOrder()`) 및 의존 관계(`getBeforeFilter()`, `getAfterFilter()`) 정의
3. Spring Bean으로 등록하여 자동으로 `filterRegistry`에 등록되도록 함

## 확장 및 커스터마이징

새로운 인증/인가 방식을 추가하려면:

1. `PluggableFilter` 인터페이스를 구현한 새 필터 클래스 생성
2. 적절한 실행 순서와 의존성 정의
3. Spring Bean으로 등록

## 주의사항
- 필터 간 순환 의존성이 없도록 주의해야 함
- 성능을 위해 불필요한 필터 중복 적용 방지
- 보안에 민감한 경로는 항상 적절한 인증/인가 필터를 통과하도록 설정
