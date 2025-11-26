package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.constants.Utils;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;

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
        String phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var event = context.getEvent();

        // If phone number is not in auth note, try to get it from user attribute
        if (phoneNumber == null && context.getUser() != null) {
            final var attrName = Utils
                    .getEnv(ConfigKey.USER_PHONE_ATTRIBUTE_NAME, PhoneNumberHelper.DEFAULT_PHONE_KEY_NAME);
            final var phoneNumbers = context.getUser().getAttributeStream(attrName).toList();
            if (!phoneNumbers.isEmpty()) {
                phoneNumber = phoneNumbers.get(0);
                log.debugf("Found phone number from user attribute: %s", phoneNumber);
                // Update session with the found number so subsequent steps use it
                authenticationSession.setAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER, phoneNumber);
            }
        }

        if (phoneNumber == null) {
            log.warn("No phone number found for sending SMS TAN");
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form()
                            .setError("missing_phone_number")
                            .createForm("request-user-phone-number.ftl")); // Fallback or error page
            return;
        }

        final var hash$ = smsService.sendSmsAndGetHash(phoneNumber);

        if (hash$.isPresent()) {
            final var hash = hash$.get();
            event.detail("sms_hash", hash)
                 .success();

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
        return true;
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
