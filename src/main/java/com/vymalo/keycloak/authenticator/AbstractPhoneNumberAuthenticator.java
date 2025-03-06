package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.services.SmsService;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPhoneNumberAuthenticator implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "26.1.3");
        infos.put("repo", "https://github.com/vymalo/keycloak-phone-number");
    }

    protected final SmsService smsService = SmsService.getInstance();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return infos;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        if (user != null) {
            return smsService.handleConfiguredFor(user);
        }
        return true;
    }
}
