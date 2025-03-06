package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;

@JBossLog
@NoArgsConstructor
public class PhoneNumberConfirmNumber extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-confirm-number";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var existingUser = context.getUser();

        if (existingUser != null) {
            context.success();
            return;
        }

        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var internationalized$ = smsService.format(phoneNumber);

        final var challenge = context
                .form()
                .setAttribute("phoneNumber", internationalized$.orElseThrow())
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
    public String getDisplayType() {
        return "SMS -2 Confirm phone number";
    }

    @Override
    public String getReferenceCategory() {
        return "confirm-phone-number";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Confirm user's phone number before sending";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
