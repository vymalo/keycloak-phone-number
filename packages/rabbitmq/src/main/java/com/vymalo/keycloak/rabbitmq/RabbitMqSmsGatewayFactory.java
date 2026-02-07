package com.vymalo.keycloak.rabbitmq;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.Optional;

public final class RabbitMqSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "RABBITMQ";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new RabbitMqSmsGateway();
    }

    private static final class RabbitMqSmsGateway implements SmsGateway {
        @Override
        public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
            throw new UnsupportedOperationException("RabbitMQ provider is not implemented yet");
        }

        @Override
        public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
            throw new UnsupportedOperationException("RabbitMQ provider is not implemented yet");
        }
    }
}
