package com.vymalo.keycloak.console;

import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsGatewayFactory;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

public final class SmsConsoleApp {
    private SmsConsoleApp() {
    }

    public static void main(String[] args) {
        final Map<String, SmsGatewayFactory> factories = loadFactories();

        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            System.out.println("Available sms providers:");
            factories.keySet().stream().sorted().forEach(provider -> System.out.println("- " + provider));
            System.out.println("Usage:");
            System.out.println("  list");
            System.out.println("  send <PROVIDER> <PHONE>");
            System.out.println("  validate <PROVIDER> <PHONE> <CODE> <HASH>");
            return;
        }

        final String providerType = args[0].toUpperCase();
        final SmsGatewayFactory factory = factories.get(providerType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerType + ". Use list command.");
        }

        if (args.length < 2) {
            throw new IllegalArgumentException("Missing operation. Use send or validate.");
        }

        final SmsGateway gateway = factory.create(readConfig());

        if ("send".equalsIgnoreCase(args[1]) && args.length >= 3) {
            final String phone = args[2];
            final Optional<String> hash = gateway.sendSmsAndGetHash(defaultContext("console_send"), phone);
            System.out.println("Hash: " + hash.orElse("<empty>"));
            return;
        }

        if ("validate".equalsIgnoreCase(args[1]) && args.length >= 5) {
            final String phone = args[2];
            final String code = args[3];
            final String hash = args[4];
            final Optional<Boolean> valid = gateway.confirmSmsCode(defaultContext("console_validate"), phone, code, hash);
            System.out.println("Valid: " + valid.orElse(false));
            return;
        }

        throw new IllegalArgumentException("Invalid arguments. Use list command.");
    }

    private static SmsServiceConfig readConfig() {
        return new SmsServiceConfig(
                envOrProp("SMS_API_URL"),
                readAuthMode(),
                envOrProp("OAUTH2_TOKEN_ENDPOINT"),
                envOrProp("SMS_API_AUTH_USERNAME"),
                envOrProp("SMS_API_AUTH_PASSWORD"),
                envOrProp("OAUTH2_CLIENT_ID"),
                envOrProp("OAUTH2_CLIENT_SECRET")
        );
    }

    private static SmsServiceConfig.SmsAuthMode readAuthMode() {
        final String raw = envOrProp("SMS_AUTH_MODE");
        if (raw == null || raw.isBlank()) {
            return SmsServiceConfig.SmsAuthMode.AUTO;
        }
        return SmsServiceConfig.SmsAuthMode.valueOf(raw.toUpperCase());
    }

    private static SmsRequestContext defaultContext(String step) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("step", step);
        return new SmsRequestContext("console", "console", "127.0.0.1", "console", "console", UUID.randomUUID().toString(), metadata);
    }

    private static Map<String, SmsGatewayFactory> loadFactories() {
        final List<SmsGatewayFactory> discovered = ServiceLoader.load(SmsGatewayFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        final Map<String, SmsGatewayFactory> factories = new HashMap<>();
        discovered.forEach(factory -> factories.put(factory.type().toUpperCase(), factory));
        return factories;
    }

    private static String envOrProp(String key) {
        final String env = System.getenv(key);
        if (env != null) {
            return env;
        }
        return System.getProperty(key);
    }
}
