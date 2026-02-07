# Keycloak Phone Number Login Plugin

This plugin helps you to login by phone number.

| Keycloak Version | Plugin Version |
|------------------|----------------|
| 21               | ✅ 1.1.0        |
| 22               | ✅ 26.1.3       |
| 23               | ✅ 26.1.3       |
| 24               | ✅ 26.1.3       |
| 25               | ✅ 26.1.3       |
| 26               | ✅ 26.1.3       |

![Example](./docs/screenshot.png)

## 1. What It Is

The **Keycloak Phone Number Login Plugin** is a custom extension for Keycloak that enables authentication via SMS-based
phone number login. Instead of (or in addition to) traditional username/password logins, this plugin allows end-users to
authenticate using their phone number. The process includes:

- **Phone Number Entry:** Users enter their phone number.
- **Confirmation:** The system displays a formatted version of the number for user confirmation.
- **User Lookup/Creation:** The plugin resolves a user by phone number (and creates one after phone verification if needed).
- **SMS TAN Sending:** A TAN is requested and sent via an external SMS API.
- **TAN Validation:** The user inputs the TAN to complete authentication.
- **Profile Update:** On successful TAN verification, users can update their profile information.

This modular plugin uses a multi-step flow and is designed with separation of concerns in mind, using a dedicated
service layer for SMS and phone number processing.

## New Feature: Dual Authentication Support for SMS Service

The `SmsService` has been enhanced to support both **Basic Authentication** and **OAuth2 Client Credentials Grant** for secure communication with the SMS provider. The plugin prioritizes OAuth2 if the required environment variables are set; otherwise, it falls back to Basic Auth.

## Provider Implementations

All declared providers are now implemented:

- `API`
- `AMQP`
- `RABBITMQ`
- `KAFKA`
- `SNS_SQS`

`API` is the REST provider. `AMQP`, `RABBITMQ`, `KAFKA`, and `SNS_SQS` now each have their own `SmsGateway` implementation in their respective packages.
For non-`API` providers, the plugin generates a TAN + hash, publishes a `SEND_SMS` event payload to the configured transport, and validates the submitted TAN locally (hash-bound, one-time, 5-minute TTL).

`SMS_API_URL` is interpreted per provider:
- `API`: REST base URL
- `KAFKA`: Kafka bootstrap servers (example: `localhost:9092`)
- `RABBITMQ`: RabbitMQ AMQP URI (example: `amqp://guest:guest@localhost:5672/%2f`)
- `AMQP`: AMQP URI
- `SNS_SQS`: optional AWS endpoint override (useful for LocalStack)

## 2. How to Use It

### Downloading the Plugin

You can download the latest plugin JARs from GitHub releases using `curl`. For example:

```bash
curl -L -o keycloak-phonenumber-login.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-login-<version>.jar
curl -L -o keycloak-phonenumber-theme.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-theme-<version>.jar
curl -L -o keycloak-phonenumber-api.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-api-<version>.jar
# Optional provider gateways (mount the one you configure as smsProvider)
curl -L -o keycloak-phonenumber-amqp.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-amqp-<version>.jar
curl -L -o keycloak-phonenumber-rabbitmq.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-rabbitmq-<version>.jar
curl -L -o keycloak-phonenumber-kafka.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-kafka-<version>.jar
curl -L -o keycloak-phonenumber-sns-sqs.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v<version>/keycloak-phonenumber-sns-sqs-<version>.jar
```

### Mounting into Keycloak

Once you have the JAR, mount it into the Keycloak server. For example, when running Keycloak in Docker, you can mount
the plugin as a volume:

#### Docker Example

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=password \
  -e SMS_API_URL=http://your-sms-api-url \
  -e SMS_API_COUNTRY_PATTERN='cm|de|fr' \
  -e SMS_API_AUTH_USERNAME=someuser \
  -e SMS_API_AUTH_PASSWORD=somepassword \
  -e OAUTH2_CLIENT_ID=some-client-id \
  -e OAUTH2_CLIENT_SECRET=some-client-secret \
  -e OAUTH2_TOKEN_ENDPOINT=http://token-mock:8080/token \
  -v /path/to/keycloak-phonenumber-login.jar:/opt/keycloak/providers/keycloak-phonenumber-login.jar \
  -v /path/to/keycloak-phonenumber-kafka.jar:/opt/keycloak/providers/keycloak-phonenumber-kafka.jar \
  -v /path/to/keycloak-phonenumber-theme.jar:/opt/keycloak/providers/keycloak-phonenumber-theme.jar \
  quay.io/keycloak/keycloak:26.5.2 start-dev
```

Mount the gateway jar that matches your configured `smsProvider` (`API`, `AMQP`, `RABBITMQ`, `KAFKA`, `SNS_SQS`).

#### Kubernetes Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      volumes:
        - name: plugin-volume
          emptyDir: {}
      initContainers:
        - name: download-plugin
          image: curlimages/curl:8.1.2
          env:
            - name: VERSION
              value: 26.5.2
          command:
            - sh
            - -c
            - |
              curl -L -o /plugin/keycloak-phonenumber-login.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v$VERSION/keycloak-phonenumber-login-$VERSION.jar
              curl -L -o /plugin/keycloak-phonenumber-api.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v$VERSION/keycloak-phonenumber-api-$VERSION.jar
              curl -L -o /plugin/keycloak-phonenumber-theme.jar https://github.com/vymalo/keycloak-phone-number/releases/download/v$VERSION/keycloak-phonenumber-theme-$VERSION.jar
          volumeMounts:
            - name: plugin-volume
              mountPath: /plugin
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:26.5.2
          ports:
            - containerPort: 8080
          env:
            - name: KEYCLOAK_ADMIN
              value: "admin"
            - name: KEYCLOAK_ADMIN_PASSWORD
              value: "password"
            - name: SMS_API_URL
              value: "http://your-sms-api-url"
            - name: SMS_API_COUNTRY_PATTERN
              value: "cm|de|fr"
            - name: SMS_API_AUTH_USERNAME
              value: "someuser"
            - name: SMS_API_AUTH_PASSWORD
              value: "somepassword"
            - name: OAUTH2_CLIENT_ID
              value: "some-client-id"
            - name: OAUTH2_CLIENT_SECRET
              value: "some-client-secret"
            - name: OAUTH2_TOKEN_ENDPOINT
              value: "http://token-mock:8080/token"
          volumeMounts:
            - name: plugin-volume
              mountPath: /opt/keycloak/providers
```

*Tip:* For Kubernetes, it's more common to use a sidecar or init container to download the plugin from GitHub and copy
it into the appropriate directory.

---

## 3. Environment Variables

The following environment variables are used by the plugin:

- **KEYCLOAK_ADMIN**: The administrator username for Keycloak.  
- **KEYCLOAK_ADMIN_PASSWORD**: The administrator password for Keycloak.  
- **KC_LOG_CONSOLE_COLOR**: Enables colored logging in the console (set to `'true'` or `'false'`).  
- **KC_HTTP_PORT**: The HTTP port on which Keycloak runs.  
- **SMS_API_URL**: The base URL of the SMS API service.  
- **SMS_API_COUNTRY_PATTERN**: A regex pattern to match supported phone number country codes.
- **SMS_API_AUTH_USERNAME**: The basic auth username for the SMS API.
- **SMS_API_AUTH_PASSWORD**: The basic auth password for the SMS API.
- **OAUTH2_CLIENT_ID**: The client ID for OAuth2 authentication with the SMS provider.  
- **OAUTH2_CLIENT_SECRET**: The client secret for OAuth2 authentication with the SMS provider.  
- **OAUTH2_TOKEN_ENDPOINT**: The URL of the token endpoint for OAuth2 authentication (e.g., `http://token-mock:8080/token` for testing, or `http://your-auth-server/oauth/token` in production).
- **SMS_KAFKA_TOPIC**: Kafka topic used by `KAFKA` provider (default: `keycloak.sms.events`).
- **SMS_RABBITMQ_QUEUE**: Queue used by `RABBITMQ` provider (default: `keycloak.sms.events`).
- **SMS_AMQP_EXCHANGE**: Exchange used by `AMQP` provider (default: `keycloak.sms.events`).
- **SMS_AMQP_ROUTING_KEY**: Routing key used by `AMQP` provider (default: `sms.send`).
- **SMS_SNS_TOPIC_ARN**: SNS topic ARN used by `SNS_SQS` provider.
- **SMS_SQS_QUEUE_URL**: SQS queue URL used by `SNS_SQS` provider.


Configure these variables in your deployment (Docker, Kubernetes, etc.) as shown in the examples above.  

---

## 4. Architecture

The plugin is built as a monorepo under `packages/`:

- **`packages/keycloak-phonenumber-abstractions`**: shared interfaces and models used by all providers.
- **`packages/keycloak-phonenumber-login`**: Keycloak authenticator flow implementation.
- **`packages/keycloak-phonenumber-api`**: OpenAPI-based SMS provider implementation.
- **`packages/keycloak-phonenumber-sns-sqs`**: SNS/SQS provider implementation.
- **`packages/keycloak-phonenumber-rabbitmq`**: RabbitMQ provider implementation.
- **`packages/keycloak-phonenumber-amqp`**: AMQP provider implementation.
- **`packages/keycloak-phonenumber-kafka`**: Kafka provider implementation.
- **`packages/keycloak-phonenumber-theme`**: login theme package.

Dependency injection (via CDI) is used to wire components together, making the system more modular, testable, and
maintainable.

---

## 5. Contribute

We welcome contributions! Here’s how you can get involved:

### Reporting Issues

- Use the GitHub issues page to report bugs, suggest enhancements, or request features.
- Ensure your issue includes detailed reproduction steps and relevant logs.

### Contributing Code

- **Fork the Repository:**  
  Create a fork, then clone it locally.
- **Branching Model:**
    - Use feature branches for new features or fixes.
    - Ensure your branch name is descriptive (e.g., `feature/sms-service-refactor`).
- **Coding Standards:**  
  Follow the existing coding conventions.
    - Write unit tests for your changes.
    - Run `mvn clean install` and ensure all tests pass.
- **Pull Requests:**  
  Submit a pull request with a clear description of your changes, referencing any issues addressed.

### Documentation & Feedback

- Update documentation as needed with your changes.
- Provide feedback on areas for improvement or additional features.

---

*For more details or questions, please contact [dev@ssegning.com](mailto:dev@ssegning.com).*
