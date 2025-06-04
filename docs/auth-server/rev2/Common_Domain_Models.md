# Chapter 4.2: Common Domain Models (Entities, DTOs & Exceptions)

This document outlines key data structures defined within the `common-domain` module. These include JPA Entities that might serve as Data Transfer Objects (DTOs) in some contexts, as well as any custom exceptions defined for common error scenarios.

## Common Data Models / Entities

The `common-domain` module currently contains the following primary data model. While it is a JPA Entity, it defines data structures that are shared or fundamental to the domain.

### SettingsOption (`com.authentication.auth.domain.SettingsOption`)

This entity represents a configurable setting option within the application. It defines the properties of a setting, its default value, and whether it's user-editable. It is used to store and manage application-wide and user-specific settings.

| Field            | Type    | JPA Annotations                                       | Description                                      |
|------------------|---------|-------------------------------------------------------|--------------------------------------------------|
| `id`             | Integer | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | The unique identifier for the setting option.    |
| `settingKey`     | String  | `@Column(name = "setting_key", nullable = false, unique = true, length = 100)` | The unique key identifying the setting (e.g., "notifications.enabled"). |
| `defaultValue`   | String  | `@Column(name = "default_value", nullable = false, length = 255)`      | The default value for this setting.              |
| `dataType`       | String  | `@Column(name = "data_type", nullable = false, length = 20)`          | The data type of the setting (e.g., "BOOLEAN", "STRING", "INTEGER"). |
| `description`    | String  | `@Column(length = 255, nullable = true)`              | A brief description of what the setting controls. |
| `isUserEditable` | Boolean | `@Column(name = "is_user_editable", nullable = false)` | Indicates if users can modify this setting. Defaults to `true`. |

**Usage:**
*   Primarily used as a JPA entity to store setting definitions in the database.
*   Instances of this class (or projections from it) might be used in API responses when providing setting details to clients, or in requests when updating settings, thus acting in a DTO-like capacity.

## Common Custom Exceptions

As of the current analysis, the `common-domain` module does not appear to define custom exception classes. Exceptions used within the application are likely standard Java exceptions or custom exceptions defined in other modules (e.g., within the `backend` module itself). If common exceptions are added to this module later, they will be documented here.
