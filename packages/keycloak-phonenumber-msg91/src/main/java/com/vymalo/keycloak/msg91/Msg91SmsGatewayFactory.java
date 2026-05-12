package com.vymalo.keycloak.msg91;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsServiceConfig;

public final class Msg91SmsGatewayFactory implements SmsGatewayFactory {
    @Override
    public String type() {
        return "MSG91";
    }

    @Override
    public SmsGateway create(SmsServiceConfig config) {
        return new Msg91SmsGateway(config);
    }
}
