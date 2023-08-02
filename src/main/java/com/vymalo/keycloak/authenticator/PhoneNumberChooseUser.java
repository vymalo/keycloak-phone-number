package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.constants.Utils;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.*;

@JBossLog
@NoArgsConstructor
public class PhoneNumberChooseUser implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "phone-number-choose-user";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "1.1.0");

        final var property = new ProviderConfigProperty();
        property.setName(ConfigKey.USER_PHONE_ATTRIBUTE_NAME);
        property.setLabel("Phone attribute name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("User phone names's attribute");
        configProperties.add(property);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var event = context.getEvent();
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);

        final var realm = context.getRealm();
        final var attrName = Utils
                .getConfigString(context.getAuthenticatorConfig(),
                        ConfigKey.USER_PHONE_ATTRIBUTE_NAME, PhoneNumberHelper.DEFAULT_PHONE_KEY_NAME);

        var user = context.getUser();

        if (user != null && !user.isEnabled()) {
            event.clone()
                    .detail("phone_number", phoneNumber)
                    .user(user)
                    .error(Errors.USER_DISABLED);
            context.clearUser();
            context.resetFlow();
            return;
        }

        if (user != null && user.isEnabled()) {
            user.setAttribute(attrName, Collections.singletonList(phoneNumber));
        } else {
            UserProvider userProvider = context.getSession().users();
            final var users = userProvider
                    .searchForUserByUserAttributeStream(realm, attrName, phoneNumber)
                    .toList();

            if (users.size() == 0 && user == null) {
                final var newUser = userProvider.addUser(realm, phoneNumber);
                newUser.setAttribute(attrName, Collections.singletonList(phoneNumber));
                newUser.setEnabled(true);
                user = newUser;
            } else {
                user = users.get(0);
            }
        }

        context.setUser(user);
        context
                .getAuthenticationSession()
                .setAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER, phoneNumber);

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return PhoneNumberHelper.handleConfiguredFor(user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public String getDisplayType() {
        return "SMS -3 Choose user by Phone number";
    }

    @Override
    public String getReferenceCategory() {
        return "phone-choose";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Choose a user, by his/her phone number, to reset credentials for";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
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
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return infos;
    }

}
