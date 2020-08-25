package com.bayamsell.keycloak.authenticator;

import com.bayamsell.keycloak.constants.PhoneKey;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
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
import java.util.Objects;

import static com.bayamsell.keycloak.constants.PhoneNumber.handleConfiguredFor;

@JBossLog
@NoArgsConstructor
public class PhoneNumberValidateTan implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "phone-number-validate-tan";
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
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        String tanSecret = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_CODE_NAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null || tanSecret == null || phoneNumber == null) {
            context.resetFlow();
            return;
        }

        Response challenge = context.form()
                .createForm("request-user-tan-code.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();

        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String attemptedCode = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_CODE_NAME);

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String cancel = formData.getFirst("cancel");
        if (cancel != null) {
            context.resetFlow();
            return;
        }

        String code = formData.getFirst("code");

        if (Objects.equals(attemptedCode, code)) {
            context.setUser(user);
            context.success();
        } else {
            EventBuilder event = context.getEvent();
            event.clone()
                    .detail("sms_code", code)
                    .user(user)
                    .error(Errors.INVALID_CODE);

            Response challenge = context
                    .form()
                    .setAttribute("smsCode", code)
                    .setError(Errors.INVALID_CODE)
                    .createForm("request-user-tan-code.ftl");
            context.challenge(challenge);
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
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
        return "SMS -5 Validate SMS Tan";
    }

    @Override
    public String getReferenceCategory() {
        return "phone-validate-tan";
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
        return "Validate the tan send in the previous page";
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
