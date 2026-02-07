package com.vymalo.keycloak.services;

public interface SmsGatewayFactory {
    String type();

    SmsGateway create(SmsServiceConfig config);
}
