# Chapter 9: Appendices

## 9.1. 용어 정의 (Glossary)

This glossary defines key technical terms and acronyms used throughout this design document.

*   **AI (Artificial Intelligence):** The simulation of human intelligence processes by machines, especially computer systems, as applied in this document to emotion analysis and thought suggestion.
*   **API (Application Programming Interface):** A set of rules and protocols that allows different software applications to communicate with each other.
*   **BCrypt:** A password-hashing function designed by Niels Provos and David Mazières, used for securely storing passwords.
*   **CI/CD (Continuous Integration/Continuous Deployment or Delivery):** Practices that automate the building, testing, and deployment of software.
*   **CRUD (Create, Read, Update, Delete):** The four basic functions of persistent storage.
*   **CSRF (Cross-Site Request Forgery):** An attack that forces an end user to execute unwanted actions on a web application in which they're currently authenticated.
*   **DDL (Data Definition Language):** A syntax for creating and modifying database objects such as tables, indexes, and users.
*   **DLQ (Dead-Letter Queue):** A service for storing messages that could not be processed successfully by a message queue consumer, allowing for later inspection and handling.
*   **Docker:** An open platform for developing, shipping, and running applications in containers.
*   **DTO (Data Transfer Object):** An object that carries data between processes, often used to transfer data between layers of an application (e.g., service layer to controller, or for API request/response bodies).
*   **ELK Stack (Elasticsearch, Logstash, Kibana):** A suite of open-source tools for searching, analyzing, and visualizing log data in real time.
*   **ERD (Entity-Relationship Diagram):** A type of flowchart that illustrates how "entities" such as people, objects, or concepts relate to each other within a system.
*   **Eventual Consistency:** A consistency model used in distributed computing that guarantees that, if no new updates are made to a given data item, eventually all accesses to that item will return the last updated value.
*   **Grafana:** An open-source platform for monitoring and observability, often used for visualizing time-series data from sources like Prometheus.
*   **HMAC (Hash-based Message Authentication Code):** A specific type of message authentication code involving a cryptographic hash function and a secret cryptographic key.
*   **HTTP (Hypertext Transfer Protocol):** The foundation of data communication for the World Wide Web.
*   **HTTPS (Hypertext Transfer Protocol Secure):** An extension of HTTP for secure communication over a computer network, widely used on the Internet.
*   **IDE (Integrated Development Environment):** A software application that provides comprehensive facilities to computer programmers for software development.
*   **Jasypt (Java Simplified Encryption):** A Java library which allows the developer to add basic encryption capabilities to his/her projects with minimum effort, and without the need of having deep knowledge on how cryptography works.
*   **JDBC (Java Database Connectivity):** An API for the Java programming language that defines how a client may access a database.
*   **JDK (Java Development Kit):** A software development environment used for developing Java applications.
*   **JPA (Jakarta Persistence API, formerly Java Persistence API):** A Java application programming interface specification that describes the management of relational data in applications using Java Platform, Standard Edition and Jakarta EE/Java EE.
*   **JSON (JavaScript Object Notation):** A lightweight data-interchange format that is easy for humans to read and write and easy for machines to parse and generate.
*   **JWT (JSON Web Token):** A compact, URL-safe means of representing claims to be transferred between two parties, commonly used for authentication and information exchange in web applications.
*   **Kafka (Apache Kafka):** A distributed event streaming platform used for building real-time data pipelines and streaming apps.
*   **KPI (Key Performance Indicator):** A measurable value that demonstrates how effectively a company is achieving key business objectives.
*   **Load Balancer:** A device or software that distributes network or application traffic across a number of servers to improve responsiveness and availability.
*   **Log4j2 (Apache Log4j 2):** An open-source logging framework for Java applications.
*   **Lombok:** A Java library that automatically plugs into your editor and build tools to spice up your java. Never write another getter or equals method again.
*   **MariaDB:** A community-developed, commercially supported fork of the MySQL relational database management system.
*   **Microservices:** An architectural style that structures an application as a collection of small, autonomous services, modeled around a business domain.
*   **MongoDB:** A source-available cross-platform document-oriented database program, classified as a NoSQL database program.
*   **Monolith (Monolithic Architecture):** An architectural style where an application is built as a single, unified unit.
*   **Nginx:** A web server that can also be used as a reverse proxy, load balancer, mail proxy, and HTTP cache.
*   **NLP (Natural Language Processing):** A subfield of linguistics, computer science, and artificial intelligence concerned with the interactions between computers and human language.
*   **NoSQL:** A database that provides a mechanism for storage and retrieval of data that is modeled in means other than the tabular relations used in relational databases.
*   **OAuth 2.0 (Open Authorization 2.0):** An open standard for access delegation, commonly used as a way for Internet users to grant websites or applications access to their information on other websites but without giving them the passwords.
*   **ORM (Object-Relational Mapping):** A programming technique for converting data between incompatible type systems using object-oriented programming languages. Spring Data JPA is an example.
*   **Outbox Pattern:** A design pattern used in microservices architecture to ensure reliable message publishing without relying on distributed transactions.
*   **PaaS (Platform as a Service):** A cloud computing model where a third-party provider delivers hardware and software tools to users over the internet.
*   **Prometheus:** An open-source systems monitoring and alerting toolkit.
*   **QueryDSL:** A framework for type-safe SQL-like querying in Java.
*   **RBAC (Role-Based Access Control):** A method of restricting network access based on the roles of individual users within an enterprise.
*   **RDB (Relational Database):** A database structured to recognize relations among stored items of information.
*   **RDBMS (Relational Database Management System):** A program that allows you to create, update, and administer a relational database.
*   **Redis (Remote Dictionary Server):** An in-memory data structure store, used as a distributed, in-memory key–value database, cache and message broker, with optional durability.
*   **REST (Representational State Transfer):** An architectural style for designing networked applications, relying on a stateless, client-server, cacheable communications protocol — and in virtually all cases, the HTTP protocol.
*   **Reverse Proxy:** A server that sits in front of web servers and forwards client (e.g. web browser) requests to those web servers.
*   **RPO (Recovery Point Objective):** The maximum acceptable amount of data loss after an unplanned incident, expressed as a duration.
*   **RTO (Recovery Time Objective):** The targeted duration of time within which a business process must be restored after a disaster or disruption.
*   **Saga Pattern:** A design pattern for managing data consistency across microservices in distributed transaction scenarios.
*   **Scalability:** The capability of a system, network, or process to handle a growing amount of work, or its potential to be enlarged to accommodate that growth. (Includes Scale Up: increasing resources of existing nodes; Scale Out: adding more nodes).
*   **Schema (Database):** The structure of a database described in a formal language supported by the database management system (DBMS).
*   **Socket.IO:** A JavaScript library for real-time web applications. It enables real-time, bi-directional communication between web clients and servers.
*   **Spring Boot:** An open-source Java-based framework used to create microservices and stand-alone, production-grade Spring applications with minimal setup.
*   **Spring Data JPA:** Part of the larger Spring Data family, makes it easy to implement JPA based repositories.
*   **Spring Security:** A powerful and highly customizable authentication and access-control framework for Java applications, especially those built with Spring.
*   **SQL (Structured Query Language):** A domain-specific language used in programming and designed for managing data held in a relational database management system.
*   **SSE (Server-Sent Events):** A server push technology enabling a client to receive automatic updates from a server via an HTTP connection.
*   **SSL/TLS (Secure Sockets Layer/Transport Layer Security):** Cryptographic protocols designed to provide communications security over a computer network.
*   **Stateless (Architecture):** A design principle where the server does not store any client context between requests. Each request from the client contains all information needed by the server.
*   **Swagger (OpenAPI Initiative):** A set of open-source tools built around the OpenAPI Specification that can help you design, build, document, and consume REST APIs. Springdoc OpenAPI is used in this project.
*   **TTL (Time-To-Live):** A mechanism that limits the lifespan or lifetime of data in a computer or network.
*   **UI (User Interface):** The means by which the user and a computer system interact, in particular the use of input devices and software.
*   **URI (Uniform Resource Identifier):** A unique sequence of characters that identifies a logical or physical resource used by web technologies.
*   **URL (Uniform Resource Locator):** A specific type of URI that not only identifies a resource but also specifies how it can be accessed (e.g., HTTP, FTP).
*   **UUID (Universally Unique Identifier):** A 128-bit number used to identify information in computer systems.
*   **XSS (Cross-Site Scripting):** A type of security vulnerability typically found in web applications that enables attackers to inject client-side scripts into web pages viewed by other users.

## 9.2. 참고 자료 (References)

This section lists key frameworks, libraries, tools, and standards referenced or used in the design of the Emotion-based AI Diary Application.

*   **Frameworks & Libraries:**
    *   Spring Boot 3.2.4
    *   Spring Security
    *   Spring Data JPA, Spring Data MongoDB, Spring Data Redis
    *   Spring Web
    *   Spring Boot OAuth2 Client
    *   Apache Kafka (spring-kafka)
    *   QueryDSL
    *   JJWT (Java JWT library by Okta) 0.11.5
    *   Jasypt (Java Simplified Encryption) 3.0.5
    *   Log4j2 (Logging framework)
    *   Lombok (Java boilerplate reduction library)
    *   Netty Socket.IO (com.corundumstudio.socketio:netty-socketio 2.0.3)
    *   Jackson Databind (JSON processing)
    *   MariaDB Java Client
    *   Springdoc OpenAPI (Swagger for API documentation) 2.2.0
    *   (Assumed for AI Worker) Python, NLTK, spaCy, Transformers (Hugging Face), scikit-learn, TensorFlow/PyTorch, kafka-python.
*   **Databases & In-Memory Stores:**
    *   MariaDB
    *   MongoDB
    *   Redis
*   **Tools & Platforms:**
    *   Docker
    *   Nginx
    *   Git & GitHub (including GitHub Actions for CI/CD)
    *   Prometheus (Monitoring)
    *   Grafana (Visualization)
    *   ELK Stack (Elasticsearch, Logstash, Kibana - Logging)
    *   IntelliJ IDEA / VS Code (Assumed IDEs)
    *   Postman / cURL (Assumed API testing tools)
*   **Standards & Protocols:**
    *   HTTP/HTTPS
    *   REST (Representational State Transfer)
    *   JSON (JavaScript Object Notation)
    *   JWT (JSON Web Token) - RFC 7519
    *   OAuth 2.0 - RFC 6749 (The OAuth 2.0 Authorization Framework)
    *   OpenID Connect (built on OAuth 2.0)
    *   Server-Sent Events (SSE) HTML5 Standard
    *   SQL (Structured Query Language)
    *   BCrypt (Password Hashing Algorithm)

## 9.3. 개정 이력 (Revision History)

| Version | Date       | Description of Changes                                     | Author(s)        |
|---------|------------|------------------------------------------------------------|------------------|
| 1.0     | 2024-05-28 | Initial comprehensive draft of the design document (Chapters 1-9). | Jules (AI Agent) |
|         |            |                                                            |                  |

*(Note: Today's date 2024-05-28 is used as an example for the initial draft.)*
