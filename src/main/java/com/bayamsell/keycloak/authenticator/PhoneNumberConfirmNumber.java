package com.bayamsell.keycloak.authenticator;

import com.bayamsell.keycloak.constants.PhoneKey;
import com.google.i18n.phonenumbers.NumberParseException;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bayamsell.keycloak.constants.PhoneNumber.handleConfiguredFor;
import static com.bayamsell.keycloak.constants.PhoneNumber.phoneNumberUtil;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import static com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@JBossLog
@NoArgsConstructor
public class PhoneNumberConfirmNumber implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "phone-number-confirm-number";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "1.0.0");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel existingUser = context.getUser();

        if (existingUser != null) {
            context.success();
            return;
        }

        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);

        PhoneNumber parsed = null;
        try {
            parsed = phoneNumberUtil.parse(phoneNumber, null);
        } catch (NumberParseException ignored) {
            // this can never happen, since we validate this phoneNumber a step before
        }

        String internationalized = phoneNumberUtil.format(parsed, PhoneNumberFormat.INTERNATIONAL);

        Response challenge = context
                .form()
                .setAttribute("phoneNumber", internationalized)
                .createForm("confirm-user-phone-number.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context
                .getHttpRequest()
                .getDecodedFormParameters();
        String cancel = formData.getFirst("cancel");

        if (cancel != null) {
            context.resetFlow();
            return;
        }

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return handleConfiguredFor(user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public String getDisplayType() {
        return "SMS -2 Confirm phone number";
    }

    @Override
    public String getReferenceCategory() {
        return "confirm-phone-number";
    }

    @Override
    public boolean isConfigurable() {
        return false;
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
        return "Confirm user's phone number before sending";
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
