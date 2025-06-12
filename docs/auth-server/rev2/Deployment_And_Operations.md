# Chapter 8: Deployment and Operations

## Introduction

This document provides an overview of how to deploy, manage, and monitor the Auth-Server application. It covers deployment strategies, CI/CD processes, health checks, logging, monitoring, and alerting.

## Deployment Strategy

### Docker Compose (for Local Development / Simple Setups)

While a `docker-compose.yml` file was not found in the standard project locations, Docker Compose is a common and recommended method for orchestrating multi-container applications, especially for local development and testing, or even simple production setups.

If you were to create a `docker-compose.yml` for this project, it would typically define services like:

*   **`auth-backend`**: The Spring Boot application itself, built as a Docker image.
*   **`database`**: A MariaDB instance.
*   **`redis`**: A Redis instance for caching/session management.
*   **`kafka`**: An Apache Kafka broker (and possibly Zookeeper if not using a KRaft-based Kafka).
*   **`mongo`**: A MongoDB instance.

**Key Environment Variables for Backend Container:**
The backend application container would need various environment variables to override defaults in `application.properties`. These include:
*   `SERVER_PORT`
*   `APP_SITE_DOMAIN`, `APP_COOKIE_DOMAIN`
*   `APP_JWT_SECRET_KEY`
*   Database connection: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
*   Redis connection: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`
*   MongoDB connection: `SPRING_DATA_MONGODB_URI`
*   Kafka connection: `SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS`
*   Email: `APP_EMAIL_SENDER`
*   OAuth credentials (these should be managed securely, e.g., via Docker secrets or environment variables from a secure source).

**Basic Docker Compose Commands:**
*   `docker-compose up -d`: Start all services in detached mode.
*   `docker-compose down`: Stop and remove all services.
*   `docker-compose logs -f <service_name>`: Follow logs for a specific service (e.g., `auth-backend`).
*   `docker-compose build <service_name>`: Build or rebuild the image for a service.

### CI/CD with GitHub Actions

The project utilizes GitHub Actions for Continuous Integration and Continuous Deployment, as defined in `.github/workflows/deploy.yml`.

*   **Workflow Triggers:**
    *   The workflow is triggered on every `push` to the `main` branch.

*   **Key Workflow Steps:**
    1.  **Checkout Code:** The latest code from the branch is checked out.
    2.  **Set up JDK 17:** The Java environment is prepared using JDK 17.
    3.  **Add GitHub SSH Key:** An SSH key (`secrets.SSH_PRIVATE_KEY_GITHUB`) is added to the agent, likely for the workflow to access other private repositories or resources if needed.
    4.  **Deploy to Server:** This step uses the `appleboy/ssh-action` to:
        *   Connect to a remote server defined by `secrets.SERVER_HOST`, `secrets.SERVER_USER`, `secrets.SSH_PRIVATE_KEY`, and `secrets.SSH_PORT`.
        *   Execute a script `/server/deploy.sh` located on the target server.
        *   **Note:** The contents of `/server/deploy.sh` are not part of the repository. This script is responsible for the actual deployment mechanics on the server. It could involve pulling a pre-built Docker image (if the image building is done in a separate workflow or manually), restarting a service, or deploying a JAR file directly. The GitHub Actions workflow itself does **not** include steps for building or pushing Docker images.

*   **Secrets Management:**
    *   The workflow relies heavily on GitHub Secrets for sensitive information such as SSH keys (`SSH_PRIVATE_KEY_GITHUB`, `SSH_PRIVATE_KEY`) and server connection details (`SERVER_HOST`, `SERVER_USER`, `SSH_PORT`). This is a good practice to avoid hardcoding credentials in the workflow file.

## Health Checks

Effective health checks are vital for ensuring the application is running correctly and for enabling automated recovery in orchestrated environments.

*   **Spring Boot Actuator (Not Explicitly Used):** The project does not explicitly include the `spring-boot-starter-actuator` dependency in its `build.gradle`. Spring Boot Actuator provides several production-ready features, including comprehensive health check endpoints (e.g., `/actuator/health`).
    *   **Recommendation:** It is highly recommended to add `spring-boot-starter-actuator`. This would provide:
        *   A default `/actuator/health` endpoint that checks basic health (e.g., disk space) and can be configured to include checks for database connectivity (MariaDB, MongoDB), Redis, and Kafka.
        *   Detailed health indicators that can be customized.
*   **Current Status:** Without Actuator, any health check mechanism would need to be custom-built (e.g., a dedicated controller endpoint that performs basic checks). Details of such custom checks are not available from the current codebase analysis.
*   **Importance:** A health check endpoint should be exposed so that deployment environments (like Docker Swarm, Kubernetes, or even a simple reverse proxy) can automatically determine if application instances are alive and ready to serve traffic.

## Logging

Logging is essential for debugging, monitoring, and auditing application behavior.

*   **Framework:** The project uses **Log4j2**, as configured in `backend/src/main/resources/log4j2.xml`.
*   **Key Log Locations:**
    *   The provided `log4j2.xml` snippet defines a `FileAppender` that writes logs to a file named `app.log`.
    *   **Console Logging (for Docker):** For containerized deployments, it's best practice to log to `stdout` and `stderr` so Docker can manage the logs. The `log4j2.xml` would need to be configured with a `ConsoleAppender` for this. The current snippet does not show one, but it might exist in the full configuration.
*   **Log Format:** The defined pattern for the file appender is `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n` with UTF-8 encoding.
*   **Adjusting Log Levels:** Log levels (e.g., INFO, DEBUG, ERROR) for different packages and classes are configured within the `<Loggers>` section of the `log4j2.xml` file. For example, to change the root log level or the level for `com.authentication.auth`, modifications would be made there.
    ```xml
    <!-- Example within log4j2.xml -->
    <Loggers>
        <Root level="info">
            <AppenderRef ref="File"/>
            <!-- Add <AppenderRef ref="Console"/> if a ConsoleAppender is defined -->
        </Root>
        <Logger name="com.authentication.auth" level="debug" additivity="false">
            <AppenderRef ref="File"/>
            <!-- <AppenderRef ref="Console"/> -->
        </Logger>
    </Loggers>
    ```
*   **Centralized Logging (Recommendation):** For production environments, especially with multiple service instances, forwarding logs to a centralized logging solution (e.g., ELK Stack - Elasticsearch, Logstash, Kibana; Splunk; Grafana Loki) is highly recommended. This allows for easier searching, analysis, and alerting based on log data.

## Monitoring

Monitoring provides insights into the application's performance, resource usage, and overall health.

*   **Spring Boot Actuator Endpoints (If Added):** If `spring-boot-starter-actuator` were added, it would expose several useful endpoints for monitoring:
    *   `/actuator/metrics`: Provides detailed metrics (e.g., JVM memory usage, CPU usage, HTTP request latencies, system uptime).
    *   `/actuator/info`: Displays application information.
    *   Other endpoints for caches, thread dumps, etc.
*   **Application Performance Monitoring (APM):**
    *   **Recommendation:** For comprehensive monitoring, consider integrating an APM tool. Popular choices include:
        *   **Prometheus & Grafana:** Prometheus for metrics collection and Grafana for visualization and dashboards. Spring Boot applications can expose metrics in Prometheus format using the Micrometer library (often included with Actuator).
        *   **Datadog, Dynatrace, New Relic:** Commercial APM solutions offering more extensive features.
*   **Key Metrics to Monitor:**
    *   **System Resources:** CPU utilization, memory usage (heap and non-heap), disk I/O, network I/O.
    *   **Application Performance:** Request latency (average, percentiles), request throughput, error rates (HTTP 4xx/5xx).
    *   **JVM Health:** Garbage collection frequency and duration, thread pool usage.
    *   **Database Performance:** Connection pool usage, query latency, number of slow queries.
    *   **Kafka Integration:** Producer send rates, consumer lag, message processing times (if applicable for consumers within this service).
    *   **Redis Performance:** Hit/miss rates, memory usage, command latency.

## Alerting

Alerting proactively notifies the operations team about critical issues or potential problems.

*   **Importance:** Alerts help in quickly identifying and addressing issues before they significantly impact users.
*   **Setup (Recommendation):**
    *   Alerting is typically set up based on the metrics collected by a monitoring system.
    *   If using Prometheus/Grafana, Grafana Alerting or Alertmanager can be used to define alert rules (e.g., high error rate, high CPU usage, service down).
    *   Alerts can be configured to send notifications via various channels like:
        *   Email
        *   Slack
        *   PagerDuty
        *   Other incident management tools.
    *   **Example Alert Conditions:**
        *   HTTP 5xx error rate exceeds X% over Y minutes.
        *   Request latency p95 exceeds Z milliseconds.
        *   Application instance is unresponsive (based on health check failures).
        *   Kafka consumer lag exceeds a critical threshold.
        *   Low disk space on the server.
*   **Current Status:** Specific alerting mechanisms are not defined within the project's codebase. Implementation would depend on the chosen monitoring tools.

## Operational Checklist (Optional but Recommended)

A routine checklist can help maintain application health and stability:

*   **Daily/Weekly Checks:**
    *   Review application logs (`app.log` or centralized logging system) for recurring errors or unusual patterns.
    *   Monitor key performance metrics (CPU, memory, error rates, latency).
    *   Check server disk space.
    *   Verify database health and backup status.
    *   Monitor Kafka cluster health and consumer group lag (if applicable).
*   **As Needed:**
    *   Apply security patches and updates to the OS, JVM, and dependencies.
    *   Review and adjust resource allocations based on performance trends.
    *   Perform capacity planning exercises.

---
