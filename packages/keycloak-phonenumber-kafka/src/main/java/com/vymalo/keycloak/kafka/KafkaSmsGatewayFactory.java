package com.vymalo.keycloak.kafka;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.Optional;

public final class KafkaSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "KAFKA";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new KafkaSmsGateway();
    }

    private static final class KafkaSmsGateway implements SmsGateway {
        @Override
        public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
            throw new UnsupportedOperationException("Kafka provider is not implemented yet");
        }

        @Override
        public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
            throw new UnsupportedOperationException("Kafka provider is not implemented yet");
        }
    }
}
