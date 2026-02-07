package com.vymalo.keycloak.kafka;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class KafkaSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "KAFKA";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new KafkaSmsGateway(config);
    }
}
