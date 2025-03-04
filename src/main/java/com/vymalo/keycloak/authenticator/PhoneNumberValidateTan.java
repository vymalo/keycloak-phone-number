package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JBossLog
@NoArgsConstructor
public class PhoneNumberValidateTan extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-validate-tan";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

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
                .setAttribute("phoneNumber", phoneNumber)
                .createForm("request-user-tan-code.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String attemptedCode = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_CODE_NAME);
        String phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);

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
                    .setAttribute("phoneNumber", phoneNumber)
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
        return PhoneNumberHelper.handleConfiguredFor(user);
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
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
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
    public String getId() {
        return PROVIDER_ID;
    }
}
