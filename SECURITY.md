# Security Policy

This document outlines the security policy for the Keycloak Phone Number Login Plugin.

## Reporting a Vulnerability

If you discover a security vulnerability, please report it to us by creating an issue in our GitHub repository. We take all security reports seriously and will investigate them promptly.

## Security Stack

### API Security

The API endpoints are protected by the following security schemes, as defined in the [`openapi/sms-open-api.yml`](openapi/sms-open-api.yml:177) file:

*   **Basic Authentication**: All API requests must include a valid `Authorization` header with a Basic authentication token.
*   **OAuth2 (Client Credentials)**: The API also supports OAuth2 with the `clientCredentials` flow, providing a secure way to authenticate client applications.

### Dependency Management

The project does not currently have any explicit security-focused dependencies, such as vulnerability scanners or static analysis tools. The project inherits from the `keycloak-parent` POM, which may provide some security features, but no specific security configurations are present in the project's own [`pom.xml`](pom.xml).

### CI/CD Security

The CI/CD pipeline, configured in [`.github/workflows/ci.yml`](.github/workflows/ci.yml), does not currently include any security scanning steps. The workflow is limited to building and packaging the application, with no automated security testing like SAST, DAST, or dependency checking.

## Recommendations

To improve the security of the project, we recommend the following:

*   **Integrate a dependency vulnerability scanner**: Add a tool like the OWASP Dependency-Check Maven plugin to the build process to automatically scan for known vulnerabilities in the project's dependencies.
*   **Add a SAST tool to the CI/CD pipeline**: Integrate a static application security testing (SAST) tool into the CI/CD pipeline to automatically scan the codebase for potential security vulnerabilities.
*   **Regularly review and update dependencies**: Periodically review the project's dependencies and update them to the latest versions to ensure that any known vulnerabilities are patched.
