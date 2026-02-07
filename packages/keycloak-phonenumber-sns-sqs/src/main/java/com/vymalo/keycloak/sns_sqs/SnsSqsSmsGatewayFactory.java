package com.vymalo.keycloak.sns_sqs;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class SnsSqsSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "SNS_SQS";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new SnsSqsSmsGateway(config);
    }
}
