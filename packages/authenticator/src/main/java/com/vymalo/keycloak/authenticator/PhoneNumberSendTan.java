package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticationExecutionModel;

@JBossLog
@NoArgsConstructor
public class PhoneNumberSendTan extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-send-tan";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var event = context.getEvent();

        final var hash$ = smsService(context).sendSmsAndGetHash(
                smsRequestContext(context, "send_tan"),
                phoneNumber
        );

        if (hash$.isPresent()) {
            final var hash = hash$.get();
            event.success();

            context
                    .getAuthenticationSession()
                    .setAuthNote(PhoneKey.ATTEMPTED_HASH, hash);
            context.success();
        } else {
            final var challenge = context
                    .form()
                    .setError("sms_could_not_be_sent")
                    .createForm("sms-tan-not-send-error.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }

    }

    @Override
    public void action(AuthenticationFlowContext context) {
        final var formData = context.getHttpRequest().getDecodedFormParameters();
        final var cancel = formData.getFirst("cancel");

        if (cancel != null) {
            context.resetFlow();
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public String getDisplayType() {
        return "SMS -4 Send SMS Tan";
    }

    @Override
    public String getReferenceCategory() {
        return "phone-send-tan";
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
        return "Send tan to a user before going to next page";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
