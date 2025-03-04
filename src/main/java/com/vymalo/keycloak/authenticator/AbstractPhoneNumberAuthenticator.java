package com.vymalo.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPhoneNumberAuthenticator implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "26.1.3");
    }

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
    public String getReferenceCategory() {
        throw new UnsupportedOperationException();
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
}
