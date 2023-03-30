package com.vymalo.keycloak.constants;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.AuthenticatorConfigModel;

@JBossLog
public class Utils {

    public static String getConfigString(AuthenticatorConfigModel config, String configName) {
        return getConfigString(config, configName, null);
    }

    public static String getConfigString(AuthenticatorConfigModel config, String configName, String defaultValue) {
        String value = defaultValue;
        if (config != null && config.getConfig() != null) {
            // Get value
            value = config.getConfig().get(configName);
        }
        return value;
    }

    public static Long getConfigLong(AuthenticatorConfigModel config, String configName, Long defaultValue) {
        Long value = defaultValue;
        if (config.getConfig() != null) {
            // Get value
            String obj = config.getConfig().get(configName);
            try {
                value = Long.valueOf(obj); // s --> ms
            } catch (NumberFormatException nfe) {
                log.error("Can not convert " + obj + " to a number.");
            }
        }
        return value;
    }

}
