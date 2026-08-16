package com.vymalo.keycloak.logger;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class LoggerSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "LOGGER";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new LoggerSmsGateway(config);
    }
}
