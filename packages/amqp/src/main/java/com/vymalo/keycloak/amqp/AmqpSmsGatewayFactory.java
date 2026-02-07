package com.vymalo.keycloak.amqp;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.Optional;

public final class AmqpSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "AMQP";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new AmqpSmsGateway();
    }

    private static final class AmqpSmsGateway implements SmsGateway {
        @Override
        public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
            throw new UnsupportedOperationException("AMQP provider is not implemented yet");
        }

        @Override
        public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
            throw new UnsupportedOperationException("AMQP provider is not implemented yet");
        }
    }
}
