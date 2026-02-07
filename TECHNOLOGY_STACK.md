# Technology Stack

This document provides an overview of the technology stack used in the Keycloak Phone Number Login Plugin.

## Backend

*   **Language**: The application is written in **Java 21**.
*   **Framework**: The project is an extension for the **Keycloak 26.5.2** identity and access management platform.
*   **Build Tool**: **Apache Maven** is used for dependency management and building the project.

## Containerization

*   **Containerization**: The project is containerized using **Docker**, with a [`Dockerfile`](Dockerfile) provided for building the application image.
*   **Orchestration**: **Docker Compose** is used to orchestrate multiple Keycloak versions and mock services for SMS and OAuth2, as defined in the [`compose.yml`](compose.yml) file.

## Messaging Integrations

The application is designed to be modular and supports various messaging queues for sending SMS messages:

*   **Amazon SNS/SQS**
*   **RabbitMQ**
*   **AMQP**
*   **Apache Kafka**

## API Specification

*   **API Specification**: The project uses the **OpenAPI** specification to define its API for sending and validating SMS messages. The specification can be found in the [`openapi/sms-open-api.yml`](openapi/sms-open-api.yml) file.

## Theming

*   **Theming Engine**: The user interface is themed using **FreeMarker** templates, which can be found in the `packages/keycloak-phonenumber-theme` directory.

## Key Libraries

The project utilizes several key libraries to provide its functionality:

*   **Lombok**: Used to reduce boilerplate code in the Java source files.
*   **Google Geocoder**: Used for geocoding phone numbers.
*   **Google Libphonenumber**: Used for parsing, formatting, and validating phone numbers.
