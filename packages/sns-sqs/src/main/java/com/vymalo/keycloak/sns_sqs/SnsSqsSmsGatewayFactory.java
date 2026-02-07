package com.vymalo.keycloak.sns_sqs;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.Optional;

public final class SnsSqsSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "SNS_SQS";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new SnsSqsSmsGateway();
    }

    private static final class SnsSqsSmsGateway implements SmsGateway {
        @Override
        public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
            throw new UnsupportedOperationException("SNS/SQS provider is not implemented yet");
        }

        @Override
        public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
            throw new UnsupportedOperationException("SNS/SQS provider is not implemented yet");
        }
    }
}
