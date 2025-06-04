# Chapter 8: Deployment and Operations

This chapter outlines the deployment architecture, configuration management, logging, monitoring, and operational considerations for the Emotion-based AI Diary Application.

## 8.1. Deployment Architecture and Process

### 8.1.1. Docker Image Build Script (`Dockerfile`)

A `Dockerfile` specific to the `backend` module was not found in the repository. However, a typical `Dockerfile` for a Spring Boot application would generally include the following steps:

1.  **Base Image:** Start from a suitable OpenJDK base image that matches the Java version used by the project (Java 21, though JDK 17 is used in CI/CD).
    ```dockerfile
    FROM openjdk:17-jdk-slim
    ```
2.  **Arguments:** Define an argument for the JAR file name/path.
    ```dockerfile
    ARG JAR_FILE=build/libs/*.jar
    ```
3.  **Copy JAR:** Copy the built Spring Boot application JAR file into the Docker image.
    ```dockerfile
    COPY ${JAR_FILE} app.jar
    ```
4.  **Expose Port:** Expose the port the Spring Boot application listens on (e.g., 8080).
    ```dockerfile
    EXPOSE 8080
    ```
5.  **Entrypoint/CMD:** Define the command to run the application when the container starts.
    ```dockerfile
    ENTRYPOINT ["java", "-jar", "/app.jar"]
    ```
    Or, to allow for passing JVM options:
    ```dockerfile
    CMD ["java", "-jar", "/app.jar"]
    ```

**Note:** An actual `Dockerfile` needs to be created and added to the `backend` directory, tailored to the specific build output and any runtime requirements (e.g., profiles, JVM arguments).

### 8.1.2. CI/CD Pipeline Configuration (GitHub Actions)

The CI/CD pipeline is configured using GitHub Actions, as defined in `.github/workflows/deploy.yml`.

*   **Trigger:** The workflow is triggered on every `push` to the `main` branch.
*   **Jobs:**
    *   A single job named `build` runs on an `ubuntu-latest` runner.
*   **Steps within the `build` job:**
    1.  **`actions/checkout@v2`:** Checks out the repository code.
    2.  **`actions/setup-java@v3`:** Sets up JDK 17 (Adopt distribution).
    3.  **`webfactory/ssh-agent@v0.5.3` (`Add Github SSH Key`):** Adds an SSH private key (`secrets.SSH_PRIVATE_KEY_GITHUB`) to the SSH agent. This key is likely a GitHub Deploy Key or an account's SSH key granting access to repositories if needed during the build/deployment phase (e.g., for pulling private dependencies, though not explicitly shown here).
    4.  **`appleboy/ssh-action@v0.1.3` (`Deploy to Server`):** Connects to the deployment server via SSH and executes a script.
        *   **Secrets Used:**
            *   `secrets.SERVER_HOST`: The hostname or IP address of the deployment server.
            *   `secrets.SERVER_USER`: The username for SSH login to the server.
            *   `secrets.SSH_PRIVATE_KEY`: The SSH private key for authenticating to the deployment server.
            *   `secrets.SSH_PORT`: The SSH port on the deployment server (defaults to 22 if not specified).
        *   **Script Executed:**
            ```bash
            chmod +x /server/deploy.sh
            /server/deploy.sh
            ```
            This indicates that a shell script named `deploy.sh` located in the `/server/` directory on the target server is responsible for the actual deployment steps.
*   **`/server/deploy.sh` (External Script):** The contents of this script are not part of this repository. It would typically perform actions such as:
    *   Logging into a Docker registry (if applicable).
    *   Pulling the latest Docker image of the application.
    *   Stopping the currently running application container.
    *   Starting a new container with the updated image.
    *   Performing health checks.
    *   Cleaning up old images/containers.

### 8.1.3. Deployment Environments

Typical deployment environments for such an application would include:

*   **Development (Dev):**
    *   Used by developers for daily work, coding, and unit/integration testing.
    *   Configuration: Local databases (MariaDB, MongoDB, Redis often run via Docker Compose), Kafka instance (local or shared dev), relaxed security settings, verbose logging.
    *   Deployment: Often direct from IDE or using local build scripts.
*   **Staging (Test/QA):**
    *   A pre-production environment that mirrors production as closely as possible.
    *   Used for thorough testing, QA, User Acceptance Testing (UAT).
    *   Configuration: Uses dedicated databases and services, near-production-level configurations, Jasypt encryption active with staging master keys.
    *   Deployment: Automated via CI/CD pipeline from a specific branch (e.g., `develop` or `release` branch).
*   **Production (Prod):**
    *   The live environment accessed by end-users.
    *   Configuration: Highly available and resilient databases and services, strict security settings, Jasypt encryption active with production master keys, performance-optimized logging.
    *   Deployment: Automated via CI/CD pipeline, typically from the `main` branch or tagged releases, often with approval steps and careful monitoring.

## 8.2. Environment Configuration Management

### 8.2.1. `application.properties` or `application.yml`

Spring Boot's externalized configuration mechanism is used.
*   **Profiles:** Configurations are managed per environment using Spring profiles (e.g., `application-dev.yml`, `application-staging.yml`, `application-prod.yml`). The active profile is typically set via an environment variable (`SPRING_PROFILES_ACTIVE`) or a command-line argument during application startup.
*   A base `application.yml` (or `.properties`) would contain common settings, with profile-specific files overriding or adding configurations.

### 8.2.2. Key Configuration Items

The following items require careful configuration per environment:

*   **Database Connection Details:** URLs, usernames, passwords for MariaDB, MongoDB, and Redis.
*   **JWT Secret Key:** The secret key used for signing JWTs (`appProperties.jwtSecretKey`). This must be unique and strong for production.
*   **Kafka Broker Addresses:** Connection string for the Kafka cluster.
*   **OAuth2 Client IDs/Secrets:** Credentials for each OAuth2 provider (Google, Kakao, Naver).
*   **Jasypt Encryption Master Password:** The password used by Jasypt to decrypt encrypted properties (see section 8.2.3).
*   **Email Service (SMTP) Configuration:** Host, port, username, password for the email server.
*   **Logging Levels and Appenders.**
*   **External API Keys:** Any other third-party API keys.
*   **CORS Allowed Origins.**

### 8.2.3. Sensitive Information Management (Jasypt & Master Password)

The application uses Jasypt (`com.github.ulisesbocchio:jasypt-spring-boot-starter`) for encrypting sensitive properties within configuration files (e.g., database passwords, JWT secret key, OAuth2 client secrets).

*   **Encrypted Properties:** Sensitive values in `application-{profile}.yml` would be stored in their encrypted form, typically prefixed with `ENC(...)`.
*   **Jasypt Master Password (Crucial for Security):**
    *   The Jasypt master password, used by the application at runtime to decrypt these properties, **must NOT be stored directly in source code or committed configuration files.**
    *   **Recommended Strategies for Managing the Jasypt Master Password:**
        1.  **Environment Variables:** Store the master password in an environment variable on the deployment server (e.g., `JASYPT_ENCRYPTOR_PASSWORD=yourSecretMasterPassword`). The application will automatically pick it up if Jasypt is configured to look for it. This is a common and recommended approach.
        2.  **Command-Line Argument:** Pass the master password as a command-line argument when starting the Spring Boot application (e.g., `--jasypt.encryptor.password=yourSecretMasterPassword`). This is less ideal for automated deployments but can be used.
        3.  **Secrets Management Service (Most Secure for Production):** Integrate with a dedicated secrets management service like HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, or Google Cloud Secret Manager. The application would fetch the Jasypt master password from this service at startup. This provides centralized management, auditing, and stronger security for secrets.

## 8.3. Logging and Monitoring

### 8.3.1. Logging Strategy

*   **Framework:** Log4j2 is used for logging, as configured in `backend/src/main/resources/log4j2.xml`.
*   **Key Events & Errors to Log:**
    *   API Requests: Method, path, parameters (masking sensitive data), response status, execution time.
    *   Authentication & Authorization: Login attempts (success/failure, username), token issuance, authorization failures.
    *   Service Layer: Key business logic steps, entry and exit of important methods.
    *   External API Calls: Requests and responses to services like OAuth providers, Kafka.
    *   Database Operations: Significant queries or slow query warnings (if enabled).
    *   Errors & Exceptions: Full stack traces for all exceptions, especially unhandled ones. Include relevant context.
    *   Kafka Operations: Message production success/failure.
*   **Log Levels:**
    *   `INFO`: Standard operational information, lifecycle events.
    *   `WARN`: Potential issues or unexpected situations that don't immediately cause errors.
    *   `ERROR`: Errors that prevent normal operation or functionality.
    *   `DEBUG`: Detailed information for development and troubleshooting (typically disabled in production or enabled dynamically).
    *   `TRACE`: Very fine-grained information (rarely used in production).
*   **Log Format (from `log4j2.xml`):**
    *   Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n`
    *   Includes: Timestamp, thread name, log level, logger name (abbreviated), and the log message.
    *   Encoding: UTF-8.
*   **Log File Management:**
    *   The current `log4j2.xml` configures a `File` appender to `app.log` with `append="true"`.
    *   **Recommendation:** Implement log rotation (e.g., daily rotation, size-based rotation) and retention policies to manage disk space. Log4j2 supports these via `RollingFileAppender`. Logs should be shipped to a centralized logging system (ELK).

### 8.3.2. Monitoring System Integration (Prometheus, Grafana, ELK Stack)

The issue description mentions Prometheus, Grafana, and ELK Stack for monitoring.

*   **Prometheus:**
    *   Collects metrics from the Spring Boot application. This is typically done by including the `spring-boot-starter-actuator` and `micrometer-registry-prometheus` dependencies. Actuator exposes a `/actuator/prometheus` endpoint that Prometheus scrapes.
    *   Metrics include: JVM metrics (memory, GC, threads), system metrics (CPU, disk), HTTP request metrics (count, latency, error rates), data source metrics (active connections, pool usage), Kafka client metrics.
    *   Custom business metrics can also be exposed using Micrometer.
*   **Grafana:**
    *   Connects to Prometheus as a data source.
    *   Used to create dashboards for visualizing key metrics, providing insights into application performance, health, and resource utilization.
*   **ELK Stack (Elasticsearch, Logstash, Kibana):**
    *   **Logstash:** Configured to collect logs from various sources, including `app.log` from the Spring Boot application instances, Nginx access/error logs, AI Worker logs, and potentially database logs. It parses, filters, and transforms these logs before forwarding them.
    *   **Elasticsearch:** Stores the processed logs in a scalable and searchable manner.
    *   **Kibana:** Provides a web interface for searching, analyzing, and visualizing logs stored in Elasticsearch. Dashboards can be created for error tracking, request tracing, and operational insights.

### 8.3.3. Alerting Configuration

*   **Prometheus Alertmanager:** Integrated with Prometheus to define alert rules based on metric thresholds or conditions (e.g., high error rates, high latency, low disk space, service unavailability). Alertmanager handles deduplication, grouping, and routing of alerts to various notification channels (e.g., email, Slack, PagerDuty).
*   **ELK Alerts:** Elasticsearch (via X-Pack alerting features or ElastAlert) can be configured to trigger alerts based on log patterns or anomalies (e.g., sudden increase in error logs, specific security-related log messages).

### 8.3.4. Real-time Communication: SSE vs. Socket.IO

The application includes dependencies for both SSE (via Spring MVC's `SseEmitter` used in `SseController.java`) and Socket.IO (via `com.corundumstudio.socketio:netty-socketio`).

*   **SSE (`SseController.java`):**
    *   **Current Use:** The `SseController` provides an endpoint for clients to subscribe to server-sent events. It sends an "INIT" event upon subscription and has a test endpoint to push dummy data.
    *   **Likely Use Cases:** Unidirectional server-to-client updates such as:
        *   Notifications to users (e.g., "Your diary analysis is complete," "New login detected from a different location").
        *   Broadcasting system-wide announcements or updates.
    *   **Advantages:** Lightweight, uses standard HTTP, efficient for server-push when bidirectional communication isn't strictly needed. Supported natively by browsers.

*   **Socket.IO (`netty-socketio` dependency):**
    *   **Current Use:** While the dependency is present, no specific controllers or services using `netty-socketio` for Socket.IO communication have been identified in the analyzed backend code.
    *   **Potential Use Cases:** If features requiring true bidirectional, low-latency communication are planned, Socket.IO would be suitable:
        *   Real-time chat features between users or with support.
        *   Collaborative editing (if ever considered for diaries, though unlikely for a personal diary).
        *   More interactive notifications that might require immediate client acknowledgments or follow-up client-server interactions within the same session.
        *   Real-time dashboards for admin users showing live application activity.
    *   **Advantages:** Provides robust bidirectional and real-time capabilities, fallback mechanisms for older browsers/proxies, room support, etc.

*   **Clarification and Recommendation:**
    *   **Distinct Roles:** Currently, only SSE seems to be actively implemented for server-push notifications. The Socket.IO dependency suggests that more interactive real-time features might be planned or were considered.
    *   **Documentation:** The design document should clearly state which specific features leverage SSE and which (if any) are planned to use Socket.IO.
    *   **Simplification:** If all current and near-future real-time needs can be met by SSE, the Socket.IO dependency might be an unnecessary overhead unless specific bidirectional features are on the roadmap. If Socket.IO is planned, its integration points (controllers, services) need to be defined. If both are truly needed, their use cases must be distinct to avoid confusion and ensure developers choose the right tool for the job. For instance, SSE for simple notifications, Socket.IO for interactive chat/collaboration.

## 8.4. Backup and Recovery

### 8.4.1. Database Backup

*   **MariaDB:**
    *   **Strategy:** Implement regular automated backups.
    *   **Methods:**
        *   Logical backups using `mysqldump` (daily full backups, with more frequent incremental backups or binary log replication if RPO is low).
        *   Physical backups (e.g., Percona XtraBackup) for faster restore times with large databases.
        *   If using a managed cloud database service, leverage its built-in automated backup and point-in-time recovery (PITR) features.
    *   **Retention:** Define a backup retention policy based on business requirements (e.g., keep daily backups for 7 days, weekly for a month, monthly for a year).
    *   **Testing:** Regularly test backup restoration procedures.
*   **MongoDB:**
    *   **Strategy:** Similar regular automated backups.
    *   **Methods:**
        *   Logical backups using `mongodump` (for full or partial backups).
        *   Filesystem snapshots (if MongoDB data files are on a separate logical volume).
        *   Managed cloud database service backups (e.g., MongoDB Atlas).
    *   **Retention & Testing:** Same principles as MariaDB.
*   **Redis:**
    *   **Persistence:** Determine if Redis data needs to be durable. Redis is often used as a cache where data loss is acceptable, but for storing refresh tokens or email verification codes, some persistence is desirable.
        *   **RDB Snapshots:** Point-in-time snapshots. Configure frequency based on RPO.
        *   **AOF (Append Only File):** Logs every write operation. Provides better durability than RDB but can result in larger files.
    *   **Backup:** If persistence is enabled, the RDB files or AOF files should be backed up regularly.
    *   **Data Repopulation:** For some cached data, it might be acceptable to repopulate from primary data sources (MariaDB, MongoDB) after a Redis failure rather than restoring from a backup. Refresh tokens, however, are critical and should be backed up if persisted.

### 8.4.2. System 장애 시 복구 절차 (RTO/RPO) - System Failure Recovery Procedure

*   **Recovery Time Objective (RTO):** The maximum acceptable downtime for the application after a disaster. (e.g., 2 hours).
*   **Recovery Point Objective (RPO):** The maximum acceptable amount of data loss measured in time. (e.g., 1 hour for critical data).
*   **General Recovery Steps:**
    1.  **Incident Assessment:** Identify the scope and nature of the failure.
    2.  **Environment Restoration:**
        *   If infrastructure is affected (e.g., server failure), provision new servers/VMs or use a standby environment.
        *   Deploy application containers/binaries from CI/CD or artifact repository.
    3.  **Database Restoration:**
        *   Restore MariaDB from the latest suitable backup, considering the RPO. Apply transaction logs if using PITR.
        *   Restore MongoDB from its latest backup.
        *   Restore Redis from its backup if persisted data is critical and not easily repopulatable.
    4.  **Configuration Verification:** Ensure all environment configurations, Jasypt master passwords (from secure storage), and network settings are correctly applied.
    5.  **Service Startup & Health Checks:** Start all application services (Backend, AI Worker), databases, Kafka, Nginx. Perform thorough health checks to ensure all components are operational.
    6.  **Data Consistency Checks:** If possible, perform checks to ensure data consistency between different data stores after restoration, especially if RPO meant some data divergence.
    7.  **Traffic Failover:** Switch DNS or load balancer configurations to the restored environment.
    8.  **Post-Mortem:** After recovery, conduct a post-mortem analysis to understand the root cause and improve recovery procedures.

## 8.5. Performance Tuning and Scalability

### 8.5.1. Key Performance Indicators (KPIs)

Monitor the following KPIs to assess application performance and identify bottlenecks:

*   **Response Time:** Average, median, 95th percentile, and 99th percentile response times for key API endpoints.
*   **Throughput:** Requests per second (RPS) or transactions per second (TPS) that the system can handle.
*   **Error Rate:** Percentage of API requests resulting in errors (e.g., HTTP 5xx, 4xx).
*   **Resource Utilization:**
    *   CPU Usage (Application servers, database servers, AI workers).
    *   Memory Usage (JVM heap, container memory, database buffers).
    *   Disk I/O and Space (Databases, log storage).
    *   Network Bandwidth.
*   **Database Performance:** Query latency, connection pool usage, index hit rates.
*   **Kafka Performance:** Message lag, throughput per topic/partition.
*   **AI Worker Performance:** Processing time per diary entry, queue length.

### 8.5.2. Scale Out/Scale Up Strategies

*   **API Gateway (Nginx):**
    *   **Scale Out:** Run multiple Nginx instances behind a load balancer (e.g., hardware LB, cloud LB).
*   **Application Server (Spring Boot Backend):**
    *   **Scale Out (Horizontal Scaling):** Deploy multiple instances of the Spring Boot application as Docker containers. Use a load balancer (managed by Nginx or a dedicated LB) to distribute traffic among instances. This is generally preferred for stateless applications.
    *   **Scale Up (Vertical Scaling):** Increase resources (CPU, memory) on the servers running the application instances.
*   **Databases:**
    *   **MariaDB:**
        *   Read Replicas: Offload read traffic to one or more read replicas.
        *   Connection Pooling: Properly configure connection pools on the application side.
        *   Vertical Scaling: Increase server resources.
        *   Sharding: For very high write loads, sharding can distribute data across multiple database servers (complex to implement).
    *   **MongoDB:**
        *   Replica Sets: For high availability and read scaling.
        *   Sharding: Distribute data across multiple shards for horizontal scalability of reads and writes.
*   **Apache Kafka:**
    *   Scale out by adding more brokers to the cluster.
    *   Increase the number of partitions for topics to improve parallelism for producers and consumers.
*   **Python AI Worker:**
    *   Scale out by running multiple instances (containers) of the AI worker. Each instance would be a consumer in the same consumer group for the relevant Kafka topic, allowing Kafka to distribute messages among them.

### 8.5.3. Caching Strategy (Redis)

*   **Purpose:** Reduce latency and database load by caching frequently accessed or computationally expensive data.
*   **Current Usage (from `RedisService.java`):**
    *   JWT Refresh Tokens.
    *   Email verification codes.
*   **Recommended Additional Caching Opportunities:**
    *   **User Profile Data:** Cache frequently accessed user profile information to avoid hitting MariaDB for every request that needs user details.
    *   **Settings:** Application-level settings (`SettingsOption`) can be cached as they change infrequently. User-specific settings (`UserCustomSetting`) could also be cached per user.
    *   **Read-Heavy API Responses:** For GET requests that return data that doesn't change very often, consider caching the entire response or parts of it.
    *   **Query Results:** Cache results of common, expensive database queries.
*   **Implementation:**
    *   Use Spring's caching abstraction (`@Cacheable`, `@CachePut`, `@CacheEvict`) with `spring-boot-starter-data-redis`.
    *   Define appropriate cache keys and Time-To-Live (TTL) values for cached data.
    *   Consider cache eviction strategies (LRU, LFU) and cache coherency if data is updated frequently.
I have successfully drafted Chapter 8: Deployment and Operations.

This chapter covers:
-   **Deployment Architecture and Process:**
    -   A typical `Dockerfile` structure for the Spring Boot application was described as one was not found.
    -   Analysis of the CI/CD pipeline from `.github/workflows/deploy.yml`, including triggers, jobs, steps, secrets, and the role of the external `/server/deploy.sh` script.
    -   Description of typical deployment environments (Dev, Staging, Prod).
-   **Environment Configuration Management:**
    -   Use of Spring Boot profiles (`application-dev.yml`, etc.).
    *   Key configuration items (DB, JWT, Kafka, OAuth2, Jasypt).
    *   **Jasypt Master Password Management:** Addressed user feedback by emphasizing that the master password must not be in source code and recommending secure strategies like environment variables or secrets management services.
-   **Logging and Monitoring:**
    *   Logging strategy using Log4j2 (from `log4j2.xml`), key events to log, levels, format, and management recommendations.
    *   Integration with Prometheus, Grafana, and ELK Stack.
    *   Alerting configuration.
    *   **SSE vs. Socket.IO Clarification:** Addressed user feedback by discussing the current use of SSE (from `SseController.java`) and the presence of the Socket.IO dependency (`netty-socketio` from `build.gradle`), outlining their distinct potential use cases and recommending clear documentation of their roles or simplification if possible.
-   **Backup and Recovery:**
    *   Database backup strategies for MariaDB, MongoDB, and Redis.
    *   General system recovery procedures with RTO/RPO considerations.
-   **Performance Tuning and Scalability:**
    *   Key Performance Indicators (KPIs).
    *   Scale Out/Scale Up strategies for different components (Nginx, Application Server, Databases, Kafka, AI Worker).
    *   Caching strategy using Redis, referencing current use and recommending further opportunities.

The content was based on the analysis of provided files, user feedback, and general best practices for deploying and operating such systems.

The output is a markdown formatted file named `Chapter8_Deployment_and_Operations.md`.

I am now ready to submit this subtask.
