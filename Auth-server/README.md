# Auth-Server_Spring-Security

This project implements an authentication and authorization server using Spring Security. It provides a robust and flexible framework for managing user access, authentication, and dynamic filter configurations.

## Features

*   User Registration and Login
*   JWT-based Authentication
*   Role-based Authorization (Admin/User)
*   Dynamic Filter Management (Add, Remove, Configure)
*   CORS Configuration
*   Email Verification
*   Password Reset
*   Swagger UI for API Documentation and Testing

## Getting Started

Detailed setup and running instructions can be found in the `backend/docs/Setup_and_Run.md` file.

## Documentation

This project provides comprehensive documentation covering various aspects of the Auth-Server. Below are the key documentation sections:

*   **API Documentation**: Detailed specifications for all REST API endpoints, including request/response formats and authentication requirements.
    *   [View API Documentation](backend/docs/API_Documentation.md)

*   **Data Models**: Information on the core data models and entities used throughout the application, including their fields and relationships.
    *   [View Data Models](backend/docs/Data_Models_Entities.md)

*   **Dynamic Filter Management**: Explanation of the dynamic filter system, allowing runtime configuration of security filters without redeployment.
    *   [View Filter Management Documentation](backend/docs/Filter_Management.md)

*   **Security Overview**: A deep dive into the security architecture, covering JWT-based authentication, authorization flows, and key security components.
    *   [View Security Overview](backend/docs/Security_Overview.md)

## Project Structure

*   `backend/`: Contains the Spring Boot application source code.
*   `backend/docs/`: Contains detailed documentation files.

## Troubleshooting

Refer to `backend/docs/Troubleshooting.md` for common issues and their solutions.

## Contributing

Further details on contributing to this project will be added soon.

## License

This project is licensed under the MIT License - see the LICENSE.md file for details.
