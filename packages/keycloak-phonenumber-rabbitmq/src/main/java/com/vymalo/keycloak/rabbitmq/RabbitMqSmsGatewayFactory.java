package com.vymalo.keycloak.rabbitmq;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class RabbitMqSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "RABBITMQ";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new RabbitMqSmsGateway(config);
    }
}
