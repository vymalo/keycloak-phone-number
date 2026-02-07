package com.vymalo.keycloak.api;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class ApiSmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "API";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new ApiSmsGateway(config);
    }
}
