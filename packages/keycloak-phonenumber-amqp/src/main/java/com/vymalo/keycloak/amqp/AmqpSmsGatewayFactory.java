package com.vymalo.keycloak.amqp;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class AmqpSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "AMQP";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new AmqpSmsGateway(config);
    }
}
