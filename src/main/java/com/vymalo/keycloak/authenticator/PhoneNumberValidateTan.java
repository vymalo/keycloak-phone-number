package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

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
        final var user = context.getUser();
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var hash = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_HASH);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null || hash == null || phoneNumber == null) {
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
        final var user = context.getUser();
        final var authenticationSession = context.getAuthenticationSession();
        final var hash = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_HASH);
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);

        final var formData = context.getHttpRequest().getDecodedFormParameters();
        final var cancel = formData.getFirst("cancel");
        if (cancel != null) {
            context.resetFlow();
            return;
        }

        final var code = formData.getFirst("code");
        final var validate$ = smsService.confirmSmsCode(phoneNumber, code, hash);

        if (validate$.isPresent() && validate$.get()) {
            context.setUser(user);
            context.success();
        } else {
            final var event = context.getEvent();
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
