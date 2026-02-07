# Developer Stack

This document provides an overview of the developer stack and workflows used in the Keycloak Phone Number Login Plugin.

## CI/CD

The project uses **GitHub Actions** for its CI/CD pipelines. The workflows are defined in the [`.github/workflows`](.github/workflows) directory and include:

*   **[`ci.yml`](.github/workflows/ci.yml)**: This workflow is responsible for continuous integration. It is triggered on every push and pull request to any branch except `master`, and it builds and tests the project using Java 21 and Maven.
*   **[`releases.yml`](.github/workflows/releases.yml)**: This workflow manages the release process. It is triggered on pushes to the `master` branch, builds the project, archives the resulting JAR files as artifacts, and creates a GitHub release.
*   **[`auto-assign.yml`](.github/workflows/auto-assign.yml)**: This workflow automates the assignment of new issues and pull requests to a designated user.

## Build Tool

*   **Build Tool**: The project uses **Maven** as its build tool, with a multi-module structure defined in the root [`pom.xml`](pom.xml).

## Development Environment

The development environment is supported by the following tools:

*   **Containerization**: **Docker** is used for containerization, with a [`Dockerfile`](Dockerfile) and [`compose.yml`](compose.yml) provided to create a consistent development and deployment environment.
*   **Mocking**: **Wiremock** is used for mocking external services, which is useful for independent development and testing. The Wiremock configuration can be found in the `wiremock` directory.
*   **API Management**: The project uses the **OpenAPI** specification to define its APIs, which helps with documentation, client generation, and ensuring API consistency. The OpenAPI specification can be found in the [`openapi/sms-open-api.yml`](openapi/sms-open-api.yml) file.
