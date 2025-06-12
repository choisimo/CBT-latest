# Chapter 9: Appendix

## Introduction

This appendix provides supplementary information to aid in understanding and working with the Auth-Server project. It includes a glossary of relevant terms, answers to frequently asked questions (FAQ), and tips for troubleshooting common issues.

---

## Glossary of Terms

*   **API (Application Programming Interface):** A set of rules, protocols, and tools for building software applications. It specifies how software components should interact, often involving requests and responses for accessing features and data.
*   **Authentication:** The process of verifying the identity of a user, system, or application.
*   **Authorization:** The process of determining whether an authenticated user, system, or application has the necessary permissions to access a specific resource or perform an action.
*   **CI/CD (Continuous Integration/Continuous Deployment):** Practices that automate the building, testing, and deployment of software, enabling faster and more reliable releases.
*   **CORS (Cross-Origin Resource Sharing):** A security feature implemented by web browsers that controls how web pages in one domain can request resources from another domain.
*   **Database Migration:** The management of incremental, reversible changes to relational database schemas.
*   **Docker:** A platform for developing, shipping, and running applications in containers. Containers package up code and all its dependencies so the application runs quickly and reliably from one computing environment to another.
*   **Docker Compose:** A tool for defining and running multi-container Docker applications. It uses a YAML file to configure the application's services.
*   **DTO (Data Transfer Object):** A simple object used to transfer data between layers of an application, often used in API request/response cycles to shape data and decouple service layers from persistence layers or API contracts.
*   **Endpoint:** A specific URL where an API can be accessed.
*   **Entity (JPA):** A Java class that is mapped to a table in a relational database. Instances of an entity correspond to rows in the table.
*   **Filter (Servlet/Spring Security):** A component that intercepts requests and responses to perform pre-processing or post-processing tasks, such as authentication, authorization, logging, or data transformation.
*   **GitHub Actions:** An automation platform integrated into GitHub that allows you to automate your build, test, and deployment pipeline.
*   **Gradle:** A build automation tool used for software development, particularly popular for Java projects.
*   **HttpOnly Cookie:** A type of cookie that cannot be accessed by client-side scripts, making it more secure against cross-site scripting (XSS) attacks. Often used for storing refresh tokens.
*   **JPA (Jakarta Persistence API):** A Java specification for accessing, persisting, and managing data between Java objects/classes and a relational database. Hibernate is a common implementation.
*   **JWT (JSON Web Token):** A compact, URL-safe standard for creating access tokens that assert some number of claims. Widely used for authentication and information exchange in web applications.
    *   **Access Token:** A short-lived JWT used to authorize access to protected API resources.
    *   **Refresh Token:** A longer-lived token used to obtain new Access Tokens without requiring the user to re-enter credentials.
*   **Kafka:** A distributed event streaming platform designed for high-throughput, fault-tolerant, real-time processing of event streams.
    *   **Producer (Kafka):** An application that publishes (writes) streams of records to one or more Kafka topics.
    *   **Consumer (Kafka):** An application that subscribes to (reads and processes) streams of records from one or more Kafka topics.
    *   **Topic (Kafka):** A category or feed name to which records are published.
    *   **Consumer Lag (Kafka):** The difference in offsets between the last message produced to a topic partition and the last message consumed by a consumer group for that partition. Indicates how far behind a consumer is.
*   **Log4j2:** A popular Java-based logging framework, successor to Log4j.
*   **MariaDB:** An open-source relational database management system, a community-developed fork of MySQL.
*   **Microservices:** An architectural style that structures an application as a collection of small, autonomous services, modeled around a business domain.
*   **MongoDB:** A NoSQL document database that stores data in flexible, JSON-like documents.
*   **Monolithic Application:** An application built as a single, unified unit.
*   **OAuth2 (Open Authorization 2.0):** An authorization framework that enables third-party applications to access user resources on a web service without exposing user credentials.
*   **RBAC (Role-Based Access Control):** A security approach that restricts network access based on the roles of individual users within an enterprise.
*   **Redis:** An in-memory data structure store, often used as a database, cache, and message broker.
*   **REST (Representational State Transfer):** An architectural style for designing networked applications, relying on a stateless, client-server, cacheable communications protocol — usually HTTP.
*   **Spring Boot:** An open-source Java-based framework used to create microservices and stand-alone, production-grade Spring applications with minimal setup.
*   **Spring Security:** A powerful and highly customizable authentication and access-control framework for Java applications, especially those built with Spring.
*   **SSE (Server-Sent Events):** A technology where a web server can push events to a client over a single, long-lived HTTP connection. Useful for real-time updates.
*   **Transaction Management:** The process of managing a sequence of operations (e.g., database reads/writes) as a single atomic unit of work to ensure data consistency.

---

## Frequently Asked Questions (FAQ)

*   **Q: How do I set up the development environment?**
    *   A: Refer to the main `README.md` for initial project setup instructions. Key requirements include Java (version 21 as per `build.gradle`), Gradle, and access to running instances of MariaDB, Redis, Kafka, and MongoDB. For local dependency setup, consider creating a `docker-compose.yml` file as outlined in `Deployment_And_Operations.md`. Ensure necessary environment variables for database connections, JWT secrets, etc., are configured.

*   **Q: Where can I find the API documentation?**
    *   A: Detailed API specifications, including endpoints, request/response formats, and authentication requirements, are available in `API_Documentation.md`.

*   **Q: How is authentication handled in this project?**
    *   A: Authentication is primarily JWT-based. Users log in with credentials to receive an Access Token and a Refresh Token. Subsequent API requests are authenticated using the Access Token. OAuth2 is also supported for third-party authentication. For a comprehensive overview, see `Security_Overview.md`.

*   **Q: What are the main modules in the project?**
    *   A: The project is structured into several key modules: `backend` (core application logic), `common-domain` (shared data models and exceptions), and `kafka-module` (Kafka integration). More details can befound in `System_Architecture.md`.

*   **Q: How do I contribute to the project or its documentation?**
    *   A: Guidelines for contributing, reporting issues, or suggesting changes can be found in the "Contributing" section of the main `README.md` at the root of the project, and also in `backend/docs/README.md`.

*   **Q: Where are transactions managed?**
    *   A: Transactions are primarily managed at the service layer using Spring's `@Transactional` annotation. Details are in `Transaction_Management.md`.

*   **Q: How does the application handle real-time updates to clients?**
    *   A: The application uses Server-Sent Events (SSE) for pushing real-time updates (e.g., notifications) to connected clients. See the SSE API section in `API_Documentation.md` and relevant flows in `Flows_And_Diagrams.md`.

---

## Troubleshooting Common Issues

*   **Issue: Application fails to start due to database connection errors (MariaDB, MongoDB).**
    *   **Solution:**
        1.  Verify database credentials (username, password) and connection URLs (host, port, database name) in `application.properties` or your environment-specific configuration.
        2.  Ensure the database server (MariaDB, MongoDB) is running and accessible from the application environment.
        3.  If using Docker for databases, check container logs (`docker logs <db_container_name>`) for any startup errors.
        4.  Confirm that the correct JDBC/MongoDB driver is present and compatible.
        5.  Check for network connectivity issues (firewalls, DNS) between the application and the database servers.

*   **Issue: Kafka consumers are not processing messages or producers cannot connect.**
    *   **Solution:**
        1.  Verify Kafka broker connection details (`spring.kafka.producer.bootstrap-servers`, consumer properties) in `application.properties`.
        2.  Ensure the Kafka brokers are running and accessible.
        3.  Check topic names for typos in producer and consumer configurations.
        4.  Inspect consumer logs for errors (e.g., deserialization issues, processing errors). The `kafka-module` logs would be relevant here.
        5.  Verify consumer group configurations and ensure consumers are subscribed to the correct topics.
        6.  Monitor Kafka consumer lag to see if consumers are falling behind.

*   **Issue: 401 Unauthorized errors when accessing protected APIs.**
    *   **Solution:**
        1.  Ensure a valid JWT Access Token is included in the `Authorization: Bearer <token>` header of your request.
        2.  Check if the token has expired. Access Tokens are typically short-lived.
        3.  Verify that the token was generated with the correct secret key and algorithm.
        4.  Ensure the `AuthorizationFilter` is correctly configured and the path you are trying to access is indeed protected and requires the roles present in your token. Refer to `Security_Overview.md`.

*   **Issue: 403 Forbidden errors when accessing protected APIs.**
    *   **Solution:**
        1.  This means you are authenticated (valid token) but do not have the required roles/permissions for the specific resource.
        2.  Verify the roles assigned to your user account (present in the JWT claims).
        3.  Check the security configuration for the endpoint to understand which roles are required (e.g., `ROLE_ADMIN` for `/api/admin/**` paths).

*   **Issue: Build failures (`./gradlew build` or in CI/CD pipeline).**
    *   **Solution:**
        1.  Read the build output carefully for specific error messages.
        2.  Ensure you have the correct Java version installed (JDK 21).
        3.  Try cleaning the build cache: `./gradlew clean build`.
        4.  Check for missing dependencies or version conflicts in `build.gradle`.
        5.  If it's a network-related error during dependency download, check your internet connection or repository configurations.

*   **Issue: Email sending fails.**
    *   **Solution:**
        1.  Verify SMTP server settings (`app.email-sender` and underlying JavaMailSender configuration which might pick up Spring Boot auto-configs for `spring.mail.*` if not explicitly set).
        2.  Check SMTP server logs for connection issues or authentication failures.
        3.  Ensure the recipient email addresses are valid.
        4.  Look for `MessagingException` or similar errors in the application logs.

*   **Issue: OAuth2 login fails (e.g., with Google, Kakao, Naver).**
    *   **Solution:**
        1.  Double-check client ID, client secret, and redirect URI configurations in `application.properties` (or your secure configuration source) and ensure they match what's configured with the OAuth provider.
        2.  Ensure the redirect URIs are correctly registered with the OAuth provider.
        3.  Look for detailed error messages from the OAuth provider in the application logs or browser console during the redirect flow.

---

## Useful Code Snippets (Optional)

This section is intended for generic code patterns. Specific examples related to APIs, transactions, etc., are provided in their respective documents.

**Basic Spring Boot Controller Structure:**
```java
package com.authentication.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/example") // Base path for this controller
public class ExampleController {

    // Example GET endpoint
    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello, World!");
    }

    // Example POST endpoint
    @PostMapping("/data")
    public ResponseEntity<MyDataResponse> processData(@RequestBody MyDataRequest request) {
        // ... process request data ...
        MyDataResponse response = new MyDataResponse("Processed: " + request.getInput());
        return ResponseEntity.ok(response);
    }

    // Dummy DTOs for the example
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
