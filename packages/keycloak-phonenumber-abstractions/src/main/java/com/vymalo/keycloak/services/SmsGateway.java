package com.vymalo.keycloak.services;

import java.util.Optional;

public interface SmsGateway {
    Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber);

    Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash);
}
