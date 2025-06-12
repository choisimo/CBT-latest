# Chapter 1: Project Overview

## 1.1. Project Background and Objective

### 1.1.1. Problem Definition and Solution
The project aims to address the common need for individuals to have a dedicated and private space to record their thoughts, feelings, and daily experiences. In today's fast-paced world, understanding and managing one's emotional state is crucial for mental well-being. Many individuals lack the tools or awareness to effectively track and reflect on their emotions.

This application, the "Emotion-based AI Diary Application," provides a solution by offering a secure digital diary platform. Beyond simple note-taking, it integrates AI-powered emotion analysis to help users gain deeper insights into their entries. The AI can identify emotional patterns, detect potentially negative thought cycles, and suggest alternative perspectives, thereby empowering users to understand their emotional landscape better.

### 1.1.2. Project Final Goal and Expected Effects
The ultimate goal of this project is to help users improve their emotional awareness and overall psychological well-being.

Expected effects include:
*   **Enhanced Emotional Intelligence:** Users will develop a better understanding of their own emotions and the triggers behind them.
*   **Improved Stress Management:** By identifying patterns and negative thoughts, users can learn to manage stress more effectively.
*   **Personal Growth and Self-Reflection:** The diary serves as a tool for self-reflection, promoting personal insights and growth over time.
*   **Better Mental Health Outcomes:** While not a replacement for professional help, the application can be a valuable tool for early identification of concerning thought patterns and for fostering positive mental health habits.

## 1.2. Key Feature Definition

### 1.2.1. Core Feature List and Priority
Based on the available documentation and API specifications, the key features are:

*   **User Registration and Authentication (Local & OAuth2)** - High (Essential for personal diary)
    *   Local registration with email verification.
    *   OAuth2 integration (Google, Kakao, Naver).
    *   JWT-based authentication for secure API access.
*   **Diary Entry Management (CRUD operations)** - High (Core functionality of a diary)
    *   Create, Read, Update, and Delete diary entries.
    *   Title and content for entries.
*   **AI-based Emotion Analysis of diary entries** - High (Key differentiator)
    *   Analysis of diary content to detect emotions.
    *   (Details of specific emotions or intensity to be confirmed from AI Worker module)
*   **Automatic Thought Detection & Alternative Thought Suggestion** - High (Value-add for well-being)
    *   Identification of potentially negative or recurring thought patterns from entries.
    *   AI-powered suggestions for alternative, more constructive thoughts.
*   **User Profile Management** - Medium
    *   View and update user profile information (nickname, profile image, etc.).
    *   Password change and management.
*   **Settings Management** - Medium
    *   Allow users to customize application settings (e.g., notifications, theme).
*   **Real-time Notifications (SSE)** - Medium
    *   Server-Sent Events for real-time updates or notifications to the user (e.g., new insights, reminders - specific use cases to be defined).

### 1.2.2. User Stories or Use Case Diagrams
(To be added if specific user stories are provided/required).

For now, a textual description of user interaction with core features:
*   **New User:** A new user can register using their email and password or via an OAuth2 provider (Google, Kakao, Naver). They will need to verify their email if using local registration.
*   **Existing User:** An existing user can log in using their credentials or OAuth2.
*   **Diary Writing:** Once logged in, a user can create a new diary entry, providing a title (optional) and content. After saving, the AI will process the entry.
*   **Viewing Diary:** Users can view a list of their past entries, potentially with a summary of the detected emotion. They can select an entry to view its full content and the associated AI analysis, including detected emotions, automatic thoughts, and suggested alternative thoughts.
*   **Editing/Deleting Diary:** Users can edit the content or title of an existing entry or delete it entirely.
*   **Managing Profile:** Users can navigate to their profile page to update their nickname, profile picture, or change their password.
*   **Customizing Settings:** Users can access a settings page to adjust preferences like notification settings.

## 1.3. Project Scope

### 1.3.1. In-Scope Features
*   All core features listed in section 1.2.1.
*   Backend API development using Java Spring Boot.
*   Database design and implementation for user data, diary entries, AI analysis results (using MariaDB, MongoDB, Redis as indicated).
*   Integration with a Python-based AI worker module via Kafka for emotion analysis and thought suggestion.
*   Secure JWT-based authentication and authorization.
*   OAuth2 integration for user login.
*   Real-time notifications using SSE.
*   API documentation (Swagger).
*   Configuration encryption.

### 1.3.2. Out-of-Scope Features
*   **Advanced Sentiment Analysis:** Beyond the core emotion detection and thought suggestion (e.g., nuanced sentiment scores, trend analysis over long periods, if not explicitly planned for the AI worker).
*   **Direct Integration with Mental Health Professionals:** No features for connecting users with therapists or counselors.
*   **Mobile Application UI Development:** This document focuses on the backend and core application logic. A separate frontend (e.g., mobile or web UI) is assumed but its development is not covered here.
*   **Complex Admin Panel:** While admin-level actions like sending custom emails are present, a full-fledged administrative interface for managing all aspects of the application is not explicitly detailed as a primary user-facing feature.
*   **Gamification or Social Features:** No elements of social sharing, community, or gamified progress tracking.

## 1.4. Technology Stack and Development Environment

### 1.4.1. Frontend, Backend, Database, AI/ML, Infrastructure, Monitoring
*   **Backend:**
    *   Java 21
    *   Spring Boot 3.2.4 (modules: Spring Web, Spring Security, Spring Data JPA, Spring Boot OAuth2 Client, Spring Data Redis, Spring Data Elasticsearch, Spring Data MongoDB)
*   **Database:**
    *   MariaDB (for relational data, likely user accounts, core diary entries)
    *   MongoDB (for document-based data, potentially AI analysis results, flexible diary content)
    *   Redis (for caching, session management, refresh tokens)
*   **AI/ML:**
    *   Python (assumption for the AI Worker module, as per issue description)
    *   Kafka (for asynchronous communication between the backend and the AI Worker module)
    *   Specific Python libraries for ML/NLP: To be detailed from the AI Worker module's own documentation/code.
*   **Infrastructure:**
    *   Docker (for containerization and deployment)
    *   Nginx (as a reverse proxy, load balancer, as per issue description)
*   **Monitoring:**
    *   Prometheus (for metrics collection)
    *   Grafana (for metrics visualization and dashboards)
    *   ELK Stack (Elasticsearch, Logstash, Kibana) for log aggregation and analysis (as per issue description)
*   **Frontend:**
    *   Not detailed in the current backend-focused analysis. Assumed to be a separate component (e.g., web application or mobile app).

### 1.4.2. Key Libraries and Framework Versions
*   **QueryDSL:** io.github.openfeign.querydsl version 6.1 (as specified in `build.gradle`)
*   **JJWT:** 0.11.5 (for JWT creation and validation)
*   **Springdoc OpenAPI (Swagger):** 2.2.0 (for API documentation)
*   **Jasypt:** 3.0.5 (for configuration encryption)
*   **Netty Socket.IO:** 2.0.3 (for real-time communication, though SSE is primarily mentioned for notifications in API docs)
*   **Apache Kafka Client:** Version managed by Spring Boot/Spring Cloud, or explicitly defined in `kafka-module/build.gradle` (to be verified if specific version needed here).
*   **MariaDB Java Client:** Runtime dependency, version managed by Spring Boot.
*   **Lombok:** Compile-time utility for reducing boilerplate code.
*   **Spring Boot Starter Validation:** For request validation.
*   **Jackson Databind:** For JSON processing.
*   **Apache Commons Lang3, Commons IO, Commons Text:** Utility libraries.
*   **Jakarta Mail:** For email functionalities.
*   **Project Modules:**
    *   `common-domain`: For shared domain objects.
    *   `kafka-module`: For Kafka integration.
```
