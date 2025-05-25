# Dynamic Filter Management

This section describes the dynamic filter management system implemented in the Auth-Server. This system allows administrators to dynamically add, remove, and configure security filter conditions without requiring code changes or application redeployments.

## Overview

The dynamic filter management is primarily handled by the `FilterRegistry` and `AdminFilterController` components. It enables the application to adapt its security behavior at runtime based on configurations provided via a REST API.

## Key Components

### `FilterRegistry`

*   **Purpose**: The central component for managing and registering dynamic filters. It holds a collection of `PluggableFilter` instances, each capable of having multiple `FilterCondition`s.
*   **Location**: `com.authentication.auth.filter.FilterRegistry.java`
*   **Functionality**:
    *   Registers `PluggableFilter` instances during application startup.
    *   Provides methods to retrieve `PluggableFilter`s by their unique ID.
    *   Manages the lifecycle of `FilterCondition`s associated with each `PluggableFilter`.

### `PluggableFilter`

*   **Purpose**: An interface representing a dynamically configurable security filter. Concrete implementations (e.g., `PathPatternFilter`) implement this interface.
*   **Location**: `com.authentication.auth.filter.PluggableFilter.java`
*   **Functionality**:
    *   Defines the common interface for dynamic filters.
    *   Manages a list of `FilterCondition`s that determine the filter's behavior.
    *   Provides methods to add, remove, and evaluate conditions.

### `FilterCondition`

*   **Purpose**: An interface representing a single condition that a `PluggableFilter` evaluates. Concrete implementations define specific criteria (e.g., path patterns, HTTP methods).
*   **Location**: `com.authentication.auth.filter.FilterCondition.java`
*   **Functionality**:
    *   Defines the `evaluate` method, which determines if a condition is met.
    *   Includes a `description` for human-readable context.

### `PathPatternFilterCondition`

*   **Purpose**: A concrete implementation of `FilterCondition` that evaluates requests based on URL path patterns and HTTP methods.
*   **Location**: `com.authentication.auth.filter.PathPatternFilterCondition.java`
*   **Functionality**:
    *   Matches incoming request paths against configured patterns.
    *   Checks if the request's HTTP method is among the allowed methods.

### `AdminFilterController`

*   **Purpose**: A REST controller that exposes endpoints for administrators to manage dynamic filters and their conditions.
*   **Location**: `com.authentication.auth.controller.AdminFilterController.java`
*   **Endpoints**:
    *   `GET /api/admin/filters`: Lists all registered dynamic filters.
    *   `POST /api/admin/filters/{filterId}/conditions`: Adds a new condition to a specified dynamic filter.
    *   `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}`: Removes a condition from a specified dynamic filter.
    *   `POST /api/admin/filters/{filterId}/status`: Enables or disables a specific dynamic filter. This allows for runtime control over whether a filter actively processes requests.

## How it Works

1.  **Initialization**: During application startup, `PluggableFilter` instances are registered with the `FilterRegistry`.
2.  **API Management**: Administrators use the `AdminFilterController`'s REST endpoints to:
    *   View existing dynamic filters.
    *   Add new `FilterCondition`s to a `PluggableFilter`. For example, adding a `PathPatternFilterCondition` to allow public access to certain paths.
    *   Remove existing `FilterCondition`s.
3.  **Runtime Evaluation**: When an HTTP request comes in, the relevant `PluggableFilter` (e.g., `AuthorizationFilter` might use `PathPatternFilter`) retrieves its associated `FilterCondition`s from the `FilterRegistry` and evaluates them. Based on the evaluation results, the filter decides whether to allow or deny the request, or apply specific logic.

## Example Use Case

An administrator can use the API to add a new `PathPatternFilterCondition` to the `jwtVerificationFilter` that allows unauthenticated access to a newly added public endpoint (`/api/public/new-feature`). This can be done without redeploying the application, providing great flexibility for managing access control.
