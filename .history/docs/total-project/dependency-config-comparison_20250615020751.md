# Dependency and Configuration Comparison Report

This report consolidates and compares build configurations and application properties for the `Auth-Server` and `CBT-back-diary` projects. Information for Auth-Server is based on details provided in the subtask description, while information for CBT-back-diary is based on previously read project files (`CBT-back-diary/src/main/java/com/ossemotion/backend/build.gradle` and `CBT-back-diary/src/main/resources/application.properties`).

## I. Build & Dependency Comparison

| Feature         | Auth-Server                                                               | CBT-back-diary (`CBT-back-diary/src/main/java/com/ossemotion/backend/build.gradle`) | Notes                                                                                                 |
| :-------------- | :------------------------------------------------------------------------ | :---------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------- |
| Java Version    | 21                                                                        | 17                                                                                  | Difference in Java versions.                                                                          |
| Spring Boot     | 3.2.4                                                                     | 3.2.0                                                                               | Minor version difference. Both are Spring Boot 3.x.                                                   |
| Database Driver | `org.mariadb.jdbc:mariadb-java-client` (assumed, consistent with dialect) | `org.mariadb.jdbc:mariadb-java-client`                                              | Both use MariaDB. Version for CBT-back-diary likely managed by Spring Boot.                           |
| Spring Data JPA | Yes (`spring-boot-starter-data-jpa`)                                      | Yes (`spring-boot-starter-data-jpa`)                                                | Consistent. Versions tied to respective Spring Boot versions.                                         |
| Spring Security | Yes (`spring-boot-starter-security`)                                      | Yes (`spring-boot-starter-security`)                                                | Consistent. Versions tied to respective Spring Boot versions.                                         |
| QueryDSL        | Yes (e.g., `com.querydsl:querydsl-jpa`)                                   | No (Not explicitly listed in its `build.gradle`)                                    | Auth-Server utilizes QueryDSL for data access; CBT-back-diary does not (based on its `build.gradle`). |
| Redis Client    | Yes (`spring-boot-starter-data-redis`)                                    | No (Not explicitly listed in its `build.gradle`)                                    | Auth-Server uses Redis; CBT-back-diary does not appear to (based on its `build.gradle`).              |
| JWT Libraries   | Yes (e.g., `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`)       | No (Not explicitly listed in its `build.gradle`)                                    | Auth-Server uses JWT for token-based authentication; CBT-back-diary does not appear to.               |
| Lombok          | Yes (`org.projectlombok:lombok`)                                          | Yes (`org.projectlombok:lombok`)                                                    | Consistent.                                                                                           |

## II. Configuration Properties Comparison

| Property               | Auth-Server (`application.properties` & implied `application-database.properties`)                                              | CBT-back-diary (`src/main/resources/application.properties`)             | Notes                                                                                                     |
| :--------------------- | :------------------------------------------------------------------------------------------------------------------------------ | :----------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------- |
| Server Port            | `7078`                                                                                                                          | `8080`                                                                   | Different ports.                                                                                          |
| DB URL                 | Externalized (in `application-database.properties`)                                                                             | `jdbc:mariadb://localhost:3306/emotion_db?createDatabaseIfNotExist=true` | Auth-Server uses externalized config. CBT-back-diary uses `emotion_db`.                                   |
| DB Username            | Externalized                                                                                                                    | `root`                                                                   | Auth-Server externalizes. CBT-back-diary uses `root`.                                                     |
| DB Password            | Externalized                                                                                                                    | `password` (hardcoded)                                                   | Auth-Server externalizes. CBT-back-diary has hardcoded password.                                          |
| DB Dialect             | `org.hibernate.dialect.MariaDBDialect`                                                                                          | `org.hibernate.dialect.MariaDBDialect`                                   | Consistent.                                                                                               |
| JPA `ddl-auto`         | `none`                                                                                                                          | `update`                                                                 | Different strategies. Auth-Server expects schema to be managed externally. CBT-back-diary allows updates. |
| JPA `show-sql`         | Not specified (default `false` unless overridden in profiles)                                                                   | `true`                                                                   | CBT-back-diary logs SQL.                                                                                  |
| Redis Config           | Assumed (as `spring-boot-starter-data-redis` is present; likely default or externalized)                                        | N/A (No Redis dependency listed in its `build.gradle`)                   | Auth-Server is configured for Redis.                                                                      |
| `spring.config.import` | Likely used for `application-database.properties` (e.g., `optional:file:./config/application-database.properties` or classpath) | Not present                                                              | Auth-Server uses externalized properties for database.                                                    |

## Summary of Findings:

This consolidated comparison uses provided information for Auth-Server and data from previously read files for CBT-back-diary.

- **Key Differences in Dependencies & Technologies:**

  - **Java Version:** Auth-Server (Java 21) uses a newer Java version than CBT-back-diary (Java 17).
  - **Spring Boot Version:** Minor difference (Auth-Server: 3.2.4, CBT-back-diary: 3.2.0). Both are on the Spring Boot 3.x line.
  - **Data Access & Utility Libraries:** Auth-Server utilizes QueryDSL for database querying, Redis for caching/session management, and JWT for token-based authentication. CBT-back-diary's specified `build.gradle` does not include these dependencies.

- **Configuration Practices:**

  - **Database Configuration:** Auth-Server externalizes its database configuration, which is suitable for production. CBT-back-diary's `application.properties` has hardcoded database credentials and URL.
  - **JPA `ddl-auto`:** Auth-Server (`none`) relies on external schema management, while CBT-back-diary (`update`) allows Hibernate to modify the schema.

- **Infrastructure Dependencies:**

  - Both projects use MariaDB.
  - Auth-Server additionally depends on Redis.
  - No Kafka usage is evident in the build files for either project.

- **Impact for Integration:**
  - The Java version difference needs consideration.
  - The absence of Redis and JWT in CBT-back-diary's specified build script is a major architectural difference if it's intended to integrate with Auth-Server's security model or leverage similar caching/session strategies. If CBT-back-diary requires these, its dependencies would need to be updated.
  - Database schema management strategies (`ddl-auto`) differ, which is important if they were to share a database or if a unified deployment strategy is considered.
  - Configuration externalization in Auth-Server is more robust for varied environments.

This comparison highlights significant differences in technology choices and configuration practices that would need to be addressed in any integration scenario.
