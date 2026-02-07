package com.vymalo.keycloak.services;

import java.util.Map;

public record SmsRequestContext(
        String realm,
        String clientId,
        String ipAddress,
        String userAgent,
        String sessionId,
        String traceId,
        Map<String, Object> metadata
) {
}

