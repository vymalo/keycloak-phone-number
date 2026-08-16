package com.vymalo.keycloak.logger;

import com.vymalo.keycloak.services.AbstractCodeGeneratingSmsGateway;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggerSmsGateway extends AbstractCodeGeneratingSmsGateway {
    private static final Logger LOGGER = Logger.getLogger(LoggerSmsGateway.class.getName());
    private static final String PROVIDER = "LOGGER";

    public LoggerSmsGateway(SmsServiceConfig config) {
        super(PROVIDER);
    }

    @Override
    protected boolean dispatch(String payload, SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        LOGGER.info("OTP for " + phoneNumber + " is " + code);
        return true;
    }
}
