package com.vymalo.keycloak.authenticator;

import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

@JBossLog
@NoArgsConstructor
public class PhoneNumberUpdateUser extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-update-user";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.CONDITIONAL,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getFirstName() != null && user.getLastName() != null && user.getEmail() != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        user.addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Override
    public String getDisplayType() {
        return "SMS -6 Update user after tan login";
    }

    @Override
    public String getReferenceCategory() {
        return "phone-update-user";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Update user's information after login by phone number";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
