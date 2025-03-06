package com.vymalo.keycloak.constants;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class Utils {

    public static String getEnv(String key) {
        final var r = System.getenv(key);
        if (r == null) {
            return System.getProperty(key);
        }
        
        if (r.isBlank()) {
            log.error("Environment variable " + key + " is empty");
        }
        
        return r;
    }

    public static String getEnv(String key, String defaultValue) {
        final var r = System.getenv(key);
        if (r == null) {
            return System.getProperty(key, defaultValue);
        }

        if (r.isBlank()) {
            log.error("Environment variable " + key + " is empty");
        }
        return r;
    }

}
