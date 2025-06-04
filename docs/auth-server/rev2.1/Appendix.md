# Chapter 9: 부록

## 소개

이 부록은 Auth-Server 프로젝트를 이해하고 활용하는 데 도움이 되는 보조 정보를 제공합니다. 관련 용어 해설, 자주 묻는 질문(FAQ), 그리고 일반적인 문제 해결 팁이 포함되어 있습니다.

---

## 용어 해설

*   **API (Application Programming Interface):** 소프트웨어 컴포넌트 간의 상호작용을 위한 규칙, 프로토콜, 도구의 집합. 기능 및 데이터 접근을 위한 요청/응답 방식이 포함됨.
*   **Authentication (인증):** 사용자, 시스템, 애플리케이션의 신원을 검증하는 과정.
*   **Authorization (인가):** 인증된 주체가 특정 리소스에 접근하거나 작업을 수행할 권한이 있는지 결정하는 과정.
*   **CI/CD (지속적 통합/지속적 배포):** 소프트웨어 빌드, 테스트, 배포를 자동화하여 더 빠르고 신뢰성 있게 릴리즈하는 개발 관행.
*   **CORS (Cross-Origin Resource Sharing):** 웹 브라우저에서 한 도메인의 웹 페이지가 다른 도메인의 리소스에 접근할 수 있도록 제어하는 보안 기능.
*   **Database Migration:** 관계형 데이터베이스 스키마의 점진적, 가역적 변경을 관리하는 것.
*   **Docker:** 컨테이너 내에서 애플리케이션과 모든 의존성을 패키징하여 환경에 상관없이 빠르고 안정적으로 실행할 수 있게 해주는 플랫폼.
*   **Docker Compose:** 여러 컨테이너 기반 애플리케이션을 정의하고 실행하는 도구. YAML 파일로 서비스 구성을 관리함.
*   **DTO (Data Transfer Object):** 계층 간 데이터 전달을 위한 단순 객체. API 요청/응답에서 데이터 구조를 정의하고 서비스 계층과 영속 계층(API 계약) 분리를 돕는다.
*   **Endpoint:** API에 접근할 수 있는 특정 URL.
*   **Entity (JPA):** 관계형 데이터베이스의 테이블에 매핑되는 자바 클래스. 인스턴스는 테이블의 행(row)에 해당.
*   **Filter (Servlet/Spring Security):** 요청/응답을 가로채 인증, 인가, 로깅, 데이터 변환 등 전/후처리를 수행하는 컴포넌트.
*   **GitHub Actions:** GitHub에 내장된 자동화 플랫폼. 빌드, 테스트, 배포 파이프라인을 자동화할 수 있음.
*   **Gradle:** 주로 자바 프로젝트에서 사용되는 빌드 자동화 도구.
*   **HttpOnly Cookie:** 클라이언트 스크립트에서 접근할 수 없는 쿠키. XSS 공격에 더 안전하며, 주로 Refresh Token 저장에 사용.
*   **JPA (Jakarta Persistence API):** 자바 객체와 관계형 데이터베이스 간 데이터 접근, 영속성, 관리를 위한 표준 명세. Hibernate가 대표적 구현체.
*   **JWT (JSON Web Token):** 여러 클레임을 담아 인증 및 정보 교환에 사용되는 짧고 URL-safe한 토큰 표준.
    *   **Access Token:** 보호된 API 접근을 위한 단기 JWT.
    *   **Refresh Token:** Access Token 재발급을 위한 장기 토큰.
*   **Kafka:** 고성능, 내결함성, 실시간 이벤트 스트림 처리를 위한 분산 이벤트 스트리밍 플랫폼.
    *   **Producer (Kafka):** 하나 이상의 Kafka 토픽에 레코드 스트림을 발행(쓰기)하는 애플리케이션.
    *   **Consumer (Kafka):** 하나 이상의 Kafka 토픽에서 레코드 스트림을 구독(읽고 처리)하는 애플리케이션.
    *   **Topic (Kafka):** 레코드가 발행되는 카테고리 또는 피드 이름.
    *   **Consumer Lag (Kafka):** 토픽 파티션의 마지막 메시지와 컨슈머 그룹이 마지막으로 소비한 메시지의 오프셋 차이. 컨슈머가 얼마나 뒤처졌는지 나타냄.
*   **Log4j2:** 자바 기반의 인기 있는 로깅 프레임워크. Log4j의 후속 버전.
*   **MariaDB:** 오픈소스 관계형 데이터베이스 관리 시스템. MySQL의 커뮤니티 포크.
*   **Microservices (마이크로서비스):** 애플리케이션을 비즈니스 도메인 중심의 작고 독립적인 서비스 집합으로 구성하는 아키텍처 스타일.
*   **MongoDB:** JSON과 유사한 유연한 문서로 데이터를 저장하는 NoSQL 데이터베이스.
*   **Monolithic Application:** 하나의 통합된 단일 유닛으로 구축된 애플리케이션.
*   **OAuth2 (Open Authorization 2.0):** 제3자 애플리케이션이 사용자 자격 증명을 노출하지 않고 웹 서비스의 리소스에 접근할 수 있게 해주는 인가 프레임워크.
*   **RBAC (Role-Based Access Control):** 사용자 역할에 따라 네트워크 접근을 제한하는 보안 방식.
*   **Redis:** 데이터베이스, 캐시, 메시지 브로커로 자주 사용되는 인메모리 데이터 구조 저장소.
*   **REST (Representational State Transfer):** HTTP 등 무상태, 클라이언트-서버, 캐시 가능한 통신 프로토콜을 기반으로 네트워크 애플리케이션을 설계하는 아키텍처 스타일.
*   **Spring Boot:** 최소 설정으로 독립 실행형, 프로덕션급 Spring 애플리케이션 및 마이크로서비스를 만들 수 있는 오픈소스 자바 프레임워크.
*   **Spring Security:** 주로 Spring 기반 자바 애플리케이션을 위한 강력하고 커스터마이즈 가능한 인증/인가 프레임워크.
*   **SSE (Server-Sent Events):** 웹 서버가 단일, 장기 HTTP 연결을 통해 클라이언트에 실시간 이벤트를 푸시할 수 있게 해주는 기술.
*   **Transaction Management:** 데이터 일관성을 위해 일련의 작업(예: DB 읽기/쓰기)을 하나의 원자적 단위로 관리하는 것.

---

## 자주 묻는 질문 (FAQ)

*   **Q: 개발 환경은 어떻게 설정하나요?**
    *   A: 초기 프로젝트 설정은 메인 `README.md`를 참고하세요. 주요 요구사항은 Java(21버전, `build.gradle` 참고), Gradle, 그리고 MariaDB, Redis, Kafka, MongoDB 인스턴스 접근입니다. 로컬 의존성 세팅은 `Deployment_And_Operations.md`의 `docker-compose.yml` 예시를 참고하세요. DB 연결, JWT 시크릿 등 환경변수도 반드시 설정해야 합니다.

*   **Q: API 문서는 어디서 볼 수 있나요?**
    *   A: 엔드포인트, 요청/응답 형식, 인증 요구사항 등 상세 API 명세는 `API_Documentation.md`에서 확인할 수 있습니다.

*   **Q: 이 프로젝트의 인증 방식은 어떻게 되나요?**
    *   A: 기본적으로 JWT 기반 인증입니다. 사용자는 자격 증명으로 로그인하면 Access Token과 Refresh Token을 받습니다. 이후 API 요청은 Access Token으로 인증합니다. OAuth2를 통한 외부 인증도 지원합니다. 전체 개요는 `Security_Overview.md`를 참고하세요.

*   **Q: 주요 모듈은 무엇인가요?**
    *   A: 프로젝트는 `backend`(핵심 로직), `common-domain`(공유 데이터 모델/예외), `kafka-module`(Kafka 연동) 등으로 구성됩니다. 자세한 내용은 `System_Architecture.md` 참고.

*   **Q: 프로젝트/문서에 기여하려면 어떻게 하나요?**
    *   A: 기여, 이슈 제보, 개선 제안 가이드는 메인 `README.md`의 "Contributing" 섹션과 `backend/docs/README.md`에 있습니다.

*   **Q: 트랜잭션 관리는 어디서 하나요?**
    *   A: 주로 서비스 계층에서 Spring의 `@Transactional` 어노테이션으로 관리합니다. 자세한 내용은 `Transaction_Management.md` 참고.

*   **Q: 실시간 클라이언트 업데이트는 어떻게 처리하나요?**
    *   A: Server-Sent Events(SSE)를 사용해 실시간 알림(예: 푸시 알림)을 제공합니다. 관련 API는 `API_Documentation.md`, 플로우는 `Flows_And_Diagrams.md` 참고.

---

## 일반적인 문제 해결

*   **문제: 데이터베이스 연결 오류로 애플리케이션이 시작되지 않음 (MariaDB, MongoDB)**
    *   **해결:**
        1.  `application.properties` 또는 환경별 설정에서 DB 자격 증명(아이디, 비밀번호)과 연결 URL(호스트, 포트, DB명) 확인
        2.  DB 서버(MariaDB, MongoDB)가 실행 중이고 애플리케이션에서 접근 가능한지 확인
        3.  Docker로 DB를 실행 중이라면 컨테이너 로그(`docker logs <db_container_name>`)에서 에러 확인
        4.  JDBC/MongoDB 드라이버가 올바르게 추가되어 있는지 확인
        5.  방화벽, DNS 등 네트워크 연결 문제 확인

*   **문제: Kafka 컨슈머가 메시지를 처리하지 않거나 프로듀서가 연결되지 않음**
    *   **해결:**
        1.  `application.properties`의 Kafka 브로커 연결 정보(`spring.kafka.producer.bootstrap-servers`, 컨슈머 속성) 확인
        2.  Kafka 브로커가 실행 중이고 접근 가능한지 확인
        3.  프로듀서/컨슈머 설정의 토픽 이름 오타 확인
        4.  컨슈머 로그에서 역직렬화, 처리 오류 등 에러 확인 (`kafka-module` 로그 참고)
        5.  컨슈머 그룹 설정 및 올바른 토픽 구독 여부 확인
        6.  Kafka 컨슈머 지연(consumer lag) 모니터링

*   **문제: 보호된 API 접근 시 401 Unauthorized 에러**
    *   **해결:**
        1.  요청의 `Authorization: Bearer <token>` 헤더에 유효한 JWT Access Token이 포함되어 있는지 확인
        2.  토큰 만료 여부 확인 (Access Token은 일반적으로 단기)
        3.  올바른 시크릿 키와 알고리즘으로 생성된 토큰인지 확인
        4.  `AuthorizationFilter` 설정 및 접근하려는 경로가 실제로 보호된 경로인지, 토큰의 역할이 요구 역할과 일치하는지 확인 (`Security_Overview.md` 참고)

*   **문제: 보호된 API 접근 시 403 Forbidden 에러**
    *   **해결:**
        1.  인증(유효한 토큰)은 되었으나 해당 리소스에 필요한 역할/권한이 없음
        2.  JWT 클레임에 할당된 사용자 역할 확인
        3.  엔드포인트의 보안 설정에서 요구 역할 확인 (예: `/api/admin/**` 경로는 `ROLE_ADMIN` 필요)

*   **문제: 빌드 실패 (`./gradlew build` 또는 CI/CD 파이프라인)**
    *   **해결:**
        1.  빌드 출력 로그에서 구체적인 에러 메시지 확인
        2.  올바른 Java 버전(JDK 21) 설치 여부 확인
        3.  빌드 캐시 정리: `./gradlew clean build` 실행
        4.  `build.gradle`의 의존성 누락/버전 충돌 확인
        5.  의존성 다운로드 중 네트워크 오류라면 인터넷 연결 또는 저장소 설정 확인

*   **문제: 이메일 발송 실패**
    *   **해결:**
        1.  SMTP 서버 설정(`app.email-sender`, Spring Boot의 `spring.mail.*` 등) 확인
        2.  SMTP 서버 로그에서 연결/인증 실패 확인
        3.  수신자 이메일 주소 유효성 확인
        4.  애플리케이션 로그에서 `MessagingException` 등 관련 에러 확인

*   **문제: OAuth2 로그인 실패 (예: Google, Kakao, Naver)**
    *   **해결:**
        1.  `application.properties`(또는 보안 설정)에서 클라이언트 ID, 시크릿, 리다이렉트 URI가 OAuth 제공자 설정과 일치하는지 확인
        2.  리다이렉트 URI가 OAuth 제공자에 올바르게 등록되어 있는지 확인
        3.  리다이렉트 플로우 중 애플리케이션 로그/브라우저 콘솔에서 상세 에러 메시지 확인

---

## 유용한 코드 스니펫 (선택)

이 섹션은 일반적인 코드 패턴 예시를 위한 공간입니다. API, 트랜잭션 등 구체적 예시는 각 문서에서 확인하세요.

**기본 Spring Boot 컨트롤러 구조 예시:**
```java
package com.authentication.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/example") // 이 컨트롤러의 기본 경로
public class ExampleController {

    // GET 예시 엔드포인트
    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello, World!");
    }

    // POST 예시 엔드포인트
    @PostMapping("/data")
    public ResponseEntity<MyDataResponse> processData(@RequestBody MyDataRequest request) {
        // ... 요청 데이터 처리 ...
        MyDataResponse response = new MyDataResponse("Processed: " + request.getInput());
        return ResponseEntity.ok(response);
    }

    // 예시용 더미 DTO
    static class MyDataRequest {
        private String input;
        // getters and setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }

    static class MyDataResponse {
        private String result;
        public MyDataResponse(String result) { this.result = result; }
        // getters and setters
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
```
---
