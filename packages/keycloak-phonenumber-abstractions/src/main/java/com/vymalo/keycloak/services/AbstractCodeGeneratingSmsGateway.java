package com.vymalo.keycloak.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractCodeGeneratingSmsGateway implements SmsGateway {
    private static final Duration TAN_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentMap<String, Challenge> CHALLENGES = new ConcurrentHashMap<>();

    private final String providerType;

    protected AbstractCodeGeneratingSmsGateway(String providerType) {
        this.providerType = providerType;
    }

    @Override
    public final Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return Optional.empty();
        }

        cleanupExpired();
        final var code = generateCode();
        final var hash = UUID.randomUUID().toString();
        final var payload = buildSmsSendPayload(requestContext, phoneNumber, code, hash);

        if (!dispatch(payload, requestContext, phoneNumber, code, hash)) {
            return Optional.empty();
        }

        final var expiresAtMillis = System.currentTimeMillis() + TAN_TTL.toMillis();
        CHALLENGES.put(hash, new Challenge(providerType, phoneNumber, code, expiresAtMillis));
        return Optional.of(hash);
    }

    @Override
    public final Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        cleanupExpired();

        if (isBlank(phoneNumber) || isBlank(code) || isBlank(hash)) {
            return Optional.of(false);
        }

        final var challenge = CHALLENGES.get(hash);
        if (challenge == null) {
            return Optional.of(false);
        }

        if (System.currentTimeMillis() >= challenge.expiresAtMillis) {
            CHALLENGES.remove(hash);
            return Optional.of(false);
        }

        if (!providerType.equals(challenge.providerType)) {
            return Optional.of(false);
        }

        if (!phoneNumber.equals(challenge.phoneNumber)) {
            return Optional.of(false);
        }

        final var valid = code.equals(challenge.code);
        if (valid) {
            CHALLENGES.remove(hash);
        }
        return Optional.of(valid);
    }

    protected abstract boolean dispatch(
            String payload,
            SmsRequestContext requestContext,
            String phoneNumber,
            String code,
            String hash
    );

    protected String providerType() {
        return providerType;
    }

    private String buildSmsSendPayload(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        final var metadata = requestContext != null ? requestContext.metadata() : null;
        final var realm = requestContext != null ? requestContext.realm() : null;
        final var clientId = requestContext != null ? requestContext.clientId() : null;
        final var ipAddress = requestContext != null ? requestContext.ipAddress() : null;
        final var userAgent = requestContext != null ? requestContext.userAgent() : null;
        final var sessionId = requestContext != null ? requestContext.sessionId() : null;
        final var traceId = requestContext != null ? requestContext.traceId() : null;

        final var sb = new StringBuilder(512);
        sb.append("{");
        appendJsonField(sb, "type", "SEND_SMS");
        sb.append(",");
        appendJsonField(sb, "provider", providerType);
        sb.append(",");
        appendJsonField(sb, "phoneNumber", phoneNumber);
        sb.append(",");
        appendJsonField(sb, "code", code);
        sb.append(",");
        appendJsonField(sb, "hash", hash);
        sb.append(",");
        appendJsonField(sb, "realm", realm);
        sb.append(",");
        appendJsonField(sb, "clientId", clientId);
        sb.append(",");
        appendJsonField(sb, "ipAddress", ipAddress);
        sb.append(",");
        appendJsonField(sb, "userAgent", userAgent);
        sb.append(",");
        appendJsonField(sb, "sessionId", sessionId);
        sb.append(",");
        appendJsonField(sb, "traceId", traceId);
        sb.append(",\"metadata\":");
        appendJsonObject(sb, metadata);
        sb.append("}");
        return sb.toString();
    }

    private static void appendJsonObject(StringBuilder sb, Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            appendJsonField(sb, entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : null);
            first = false;
        }
        sb.append("}");
    }

    private static void appendJsonField(StringBuilder sb, String key, String value) {
        sb.append("\"").append(escapeJson(key)).append("\":");
        if (value == null) {
            sb.append("null");
            return;
        }
        sb.append("\"").append(escapeJson(value)).append("\"");
    }

    private static String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        final var escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            final var ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static void cleanupExpired() {
        final long now = System.currentTimeMillis();
        CHALLENGES.entrySet().removeIf(entry -> now >= entry.getValue().expiresAtMillis);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record Challenge(
            String providerType,
            String phoneNumber,
            String code,
            long expiresAtMillis
    ) {
    }
}
