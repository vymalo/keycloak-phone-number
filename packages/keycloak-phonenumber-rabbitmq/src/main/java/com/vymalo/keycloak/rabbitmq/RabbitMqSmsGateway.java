package com.vymalo.keycloak.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.vymalo.keycloak.services.AbstractCodeGeneratingSmsGateway;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RabbitMqSmsGateway extends AbstractCodeGeneratingSmsGateway {
    private static final Logger LOGGER = Logger.getLogger(RabbitMqSmsGateway.class.getName());
    private static final String PROVIDER = "RABBITMQ";
    private static final String DEFAULT_QUEUE = "keycloak.sms.events";

    private final SmsServiceConfig config;
    private final String queueName;

    public RabbitMqSmsGateway(SmsServiceConfig config) {
        super(PROVIDER);
        this.config = config;
        this.queueName = readEnvOrProperty("SMS_RABBITMQ_QUEUE", DEFAULT_QUEUE);
    }

    @Override
    protected boolean dispatch(String payload, SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        final var factory = new ConnectionFactory();
        try {
            factory.setUri(config.smsApiUrl());
            if (notBlank(config.basicUsername())) {
                factory.setUsername(config.basicUsername());
            }
            if (notBlank(config.basicPassword())) {
                factory.setPassword(config.basicPassword());
            }

            try (var connection = factory.newConnection(); var channel = connection.createChannel()) {
                channel.queueDeclare(queueName, true, false, false, null);
                final var props = new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .build();
                channel.basicPublish("", queueName, props, payload.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch SMS event via RabbitMQ", e);
            return false;
        }
    }

    private static String readEnvOrProperty(String key, String defaultValue) {
        final var fromEnv = System.getenv(key);
        if (notBlank(fromEnv)) {
            return fromEnv.trim();
        }

        final var fromProperty = System.getProperty(key);
        if (notBlank(fromProperty)) {
            return fromProperty.trim();
        }
        return defaultValue;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
