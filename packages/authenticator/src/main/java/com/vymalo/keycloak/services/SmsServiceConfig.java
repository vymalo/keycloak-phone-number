package com.vymalo.keycloak.services;

import org.apache.commons.lang3.StringUtils;

public record SmsServiceConfig(
        String smsApiUrl,
        SmsAuthMode authMode,
        String oauthTokenEndpoint,
        String basicUsername,
        String basicPassword,
        String oauthClientId,
        String oauthClientSecret
) {
    public SmsServiceConfig {
        if (StringUtils.isBlank(smsApiUrl)) {
            throw new IllegalArgumentException("smsApiUrl is required");
        }
        if (authMode == null) {
            authMode = SmsAuthMode.AUTO;
        }
    }

    public enum SmsAuthMode {
        AUTO,
        BASIC,
        OAUTH2,
        NONE
    }
}

