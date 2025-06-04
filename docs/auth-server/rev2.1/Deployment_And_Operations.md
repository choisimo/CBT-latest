# Chapter 8: 배포 및 운영

## 소개

이 문서는 Auth-Server 애플리케이션의 배포, 관리, 모니터링 방법을 안내합니다. 배포 전략, CI/CD, 헬스 체크, 로깅, 모니터링, 알림 설정 등을 다룹니다.

## 배포 전략

### Docker Compose (로컬 개발/간단한 환경)

표준 프로젝트 위치에 `docker-compose.yml` 파일이 없지만, Docker Compose는 다중 컨테이너 애플리케이션을 오케스트레이션하는 일반적이고 권장되는 방법입니다. 주로 로컬 개발, 테스트, 간단한 프로덕션 환경에 적합합니다.

이 프로젝트에 대한 `docker-compose.yml` 예시를 만든다면, 보통 다음과 같은 서비스를 정의합니다:

*   **`auth-backend`**: Spring Boot 애플리케이션 자체 (Docker 이미지로 빌드)
*   **`database`**: MariaDB 인스턴스
*   **`redis`**: 캐싱/세션 관리를 위한 Redis 인스턴스
*   **`kafka`**: Apache Kafka 브로커 (KRaft 기반 Kafka가 아니라면 Zookeeper도 필요할 수 있음)
*   **`mongo`**: MongoDB 인스턴스

**백엔드 컨테이너 주요 환경 변수:**
백엔드 애플리케이션 컨테이너는 `application.properties`의 기본값을 오버라이드할 다양한 환경 변수가 필요합니다. 예시:
*   `SERVER_PORT`
*   `APP_SITE_DOMAIN`, `APP_COOKIE_DOMAIN`
*   `APP_JWT_SECRET_KEY`
*   데이터베이스 연결: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
*   Redis 연결: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`
*   MongoDB 연결: `SPRING_DATA_MONGODB_URI`
*   Kafka 연결: `SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS`
*   이메일: `APP_EMAIL_SENDER`
*   OAuth 자격증명(보안상 Docker secrets 또는 안전한 환경변수 관리 필요)

**기본 Docker Compose 명령어:**
*   `docker-compose up -d`: 모든 서비스를 백그라운드로 시작
*   `docker-compose down`: 모든 서비스 중지 및 제거
*   `docker-compose logs -f <service_name>`: 특정 서비스 로그 실시간 보기 (예: `auth-backend`)
*   `docker-compose build <service_name>`: 서비스 이미지 빌드/재빌드

### GitHub Actions를 활용한 CI/CD

프로젝트는 `.github/workflows/deploy.yml`에 정의된 GitHub Actions를 통해 CI/CD를 수행합니다.

*   **워크플로우 트리거:**
    *   `main` 브랜치에 `push`가 발생할 때마다 워크플로우가 실행됩니다.

*   **주요 워크플로우 단계:**
    1.  **코드 체크아웃:** 최신 브랜치 코드를 가져옴
    2.  **JDK 17 환경 준비:** JDK 17로 자바 환경 세팅
    3.  **GitHub SSH 키 추가:** 에이전트에 SSH 키(`secrets.SSH_PRIVATE_KEY_GITHUB`) 추가 (다른 비공개 저장소 접근 등 필요 시)
    4.  **서버 배포:** `appleboy/ssh-action`을 사용해
        *   `secrets.SERVER_HOST`, `secrets.SERVER_USER`, `secrets.SSH_PRIVATE_KEY`, `secrets.SSH_PORT`로 정의된 원격 서버에 접속
        *   대상 서버의 `/server/deploy.sh` 스크립트 실행
        *   **참고:** `/server/deploy.sh`의 내용은 저장소에 포함되어 있지 않음. 이 스크립트가 실제 배포(이미지 pull, 서비스 재시작, JAR 배포 등)를 담당. GitHub Actions 워크플로우 자체에는 이미지 빌드/푸시 단계가 없음.

*   **Secrets 관리:**
    *   SSH 키(`SSH_PRIVATE_KEY_GITHUB`, `SSH_PRIVATE_KEY`), 서버 접속 정보(`SERVER_HOST`, `SERVER_USER`, `SSH_PORT`) 등 민감 정보는 GitHub Secrets로 안전하게 관리합니다.

## 헬스 체크

애플리케이션이 정상적으로 동작하는지 확인하고, 오케스트레이션 환경에서 자동 복구를 가능하게 하려면 효과적인 헬스 체크가 필수입니다.

*   **Spring Boot Actuator (명시적 사용 X):** `build.gradle`에 `spring-boot-starter-actuator` 의존성이 명시적으로 포함되어 있지 않습니다. Actuator는 `/actuator/health` 등 다양한 프로덕션용 헬스 체크 엔드포인트를 제공합니다.
    *   **권장:** `spring-boot-starter-actuator` 추가를 권장합니다. 주요 효과:
        *   DB(MariaDB, MongoDB), Redis, Kafka 등 연결 상태까지 포함한 `/actuator/health` 제공
        *   커스터마이즈 가능한 상세 헬스 인디케이터
*   **현재 상태:** Actuator가 없다면, 별도의 커스텀 컨트롤러로 헬스 체크 엔드포인트를 구현해야 합니다(코드베이스에 해당 내용 없음).
*   **중요성:** 헬스 체크 엔드포인트는 Docker Swarm, Kubernetes, reverse proxy 등에서 인스턴스의 정상/준비 상태를 자동 감지하는 데 필수입니다.

## 로깅

로깅은 디버깅, 모니터링, 감사에 필수적입니다.

*   **프레임워크:** 프로젝트는 **Log4j2**를 사용하며, 설정은 `backend/src/main/resources/log4j2.xml`에 있습니다.
*   **주요 로그 위치:**
    *   `log4j2.xml`의 `FileAppender`는 로그를 `app.log` 파일에 기록합니다.
    *   **Docker용 콘솔 로깅:** 컨테이너 환경에서는 로그를 stdout/stderr로 출력하는 것이 권장됩니다. 이를 위해 `ConsoleAppender` 추가 필요(현재 설정 일부만 제공됨).
*   **로그 포맷:** 파일 appender의 패턴은 `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n` (UTF-8 인코딩)
*   **로그 레벨 조정:** 패키지/클래스별 로그 레벨은 `<Loggers>` 섹션에서 설정. 예시:
    ```xml
    <!-- log4j2.xml 내 예시 -->
    <Loggers>
        <Root level="info">
            <AppenderRef ref="File"/>
            <!-- <AppenderRef ref="Console"/> 추가 시 콘솔 로깅 -->
        </Root>
        <Logger name="com.authentication.auth" level="debug" additivity="false">
            <AppenderRef ref="File"/>
            <!-- <AppenderRef ref="Console"/> -->
        </Logger>
    </Loggers>
    ```
*   **중앙 집중식 로깅(권장):** 프로덕션 환경(여러 인스턴스)에서는 로그를 중앙 시스템(ELK, Splunk, Grafana Loki 등)으로 전송하는 것이 좋습니다. 검색, 분석, 알림에 용이합니다.

## 모니터링

모니터링은 애플리케이션의 성능, 자원 사용, 전체 상태를 파악하는 데 중요합니다.

*   **Spring Boot Actuator 엔드포인트(추가 시):** Actuator를 추가하면 다음과 같은 모니터링 엔드포인트가 노출됩니다:
    *   `/actuator/metrics`: JVM 메모리, CPU, HTTP 지연, 시스템 업타임 등 상세 메트릭
    *   `/actuator/info`: 애플리케이션 정보
    *   캐시, 스레드 덤프 등 기타 엔드포인트
*   **APM(Application Performance Monitoring):**
    *   **권장:** 종합 모니터링을 위해 APM 도구 연동을 고려하세요. 대표적 예시:
        *   **Prometheus & Grafana:** Prometheus로 메트릭 수집, Grafana로 시각화/대시보드. Micrometer(Actuator에 포함)로 Prometheus 포맷 노출 가능
        *   **Datadog, Dynatrace, New Relic:** 상용 APM 솔루션
*   **주요 모니터링 지표:**
    *   **시스템 자원:** CPU, 메모리(힙/비힙), 디스크/네트워크 I/O
    *   **애플리케이션 성능:** 요청 지연(평균, p95 등), 처리량, 에러율(HTTP 4xx/5xx)
    *   **JVM 상태:** GC 빈도/시간, 스레드풀 사용량
    *   **DB 성능:** 커넥션풀, 쿼리 지연, 슬로우 쿼리 수
    *   **Kafka:** 프로듀서 전송률, 컨슈머 지연, 메시지 처리 시간
    *   **Redis:** hit/miss, 메모리 사용, 명령 지연

## 알림(모니터링 경보)

알림은 운영팀이 심각한 문제나 잠재적 장애를 신속히 인지할 수 있게 해줍니다.

*   **중요성:** 알림은 사용자 영향이 커지기 전에 신속한 대응을 가능하게 합니다.
*   **설정(권장):**
    *   모니터링 시스템에서 수집한 메트릭 기반으로 알림 규칙을 설정합니다.
    *   Prometheus/Grafana 사용 시 Grafana Alerting 또는 Alertmanager로 알림 조건 정의(예: 에러율, CPU, 서비스 다운 등)
    *   이메일, Slack, PagerDuty 등 다양한 채널로 알림 전송 가능
    *   **예시 알림 조건:**
        *   HTTP 5xx 에러율 X% 초과
        *   p95 요청 지연 Z ms 초과
        *   인스턴스 헬스 체크 실패
        *   Kafka 컨슈머 지연 임계치 초과
        *   서버 디스크 공간 부족
*   **현재 상태:** 코드베이스에 구체적 알림 메커니즘은 정의되어 있지 않음. 실제 구현은 선택한 모니터링 도구에 따라 달라짐.

## 운영 체크리스트 (선택적이지만 권장)

정기적인 체크리스트는 애플리케이션의 건강과 안정성을 유지하는 데 도움이 됩니다.

*   **일일/주간 점검:**
    *   애플리케이션 로그(`app.log` 또는 중앙 로깅 시스템)에서 반복 에러/이상 패턴 확인
    *   주요 성능 지표(CPU, 메모리, 에러율, 지연 등) 모니터링
    *   서버 디스크 공간 확인
    *   DB 상태 및 백업 상태 점검
    *   Kafka 클러스터 및 컨슈머 그룹 지연 확인
*   **수시:**
    *   OS, JVM, 의존성 보안 패치 적용
    *   성능 추이에 따라 자원 할당 조정
    *   용량 계획 수립

---
