package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.Utils;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;

import java.util.Map;
import java.util.Optional;

@JBossLog
public final class AuthenticatorConfigResolver {

    private AuthenticatorConfigResolver() {
    }

    public static Optional<String> getConfig(AuthenticationFlowContext context, String key) {
        if (context == null || context.getAuthenticatorConfig() == null) {
            return Optional.empty();
        }

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        if (config == null) {
            return Optional.empty();
        }

        String value = config.get(key);
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public static String getConfigOrEnv(AuthenticationFlowContext context, String configKey, String envKey, String defaultValue) {
        return getConfig(context, configKey).orElseGet(() -> {
            String value = Utils.getEnv(envKey, defaultValue);
            if (StringUtils.isBlank(value)) {
                log.warnf("Resolved blank value for %s (config=%s, env=%s)", configKey, configKey, envKey);
                return defaultValue;
            }
            return value;
        });
    }
}

