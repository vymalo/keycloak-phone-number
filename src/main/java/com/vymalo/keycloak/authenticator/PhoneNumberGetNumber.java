package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.constants.Utils;
import com.vymalo.keycloak.services.SmsService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;

import static org.keycloak.models.DefaultActionTokenKey.ACTION_TOKEN_USER_ID;

@JBossLog
@NoArgsConstructor
public class PhoneNumberGetNumber extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "get-user-phone-number";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    private static boolean handlePreExistingUser(AuthenticationFlowContext context, UserModel existingUser) {
        final var attrName = Utils
                .getEnv(ConfigKey.USER_PHONE_ATTRIBUTE_NAME, PhoneNumberHelper.DEFAULT_PHONE_KEY_NAME);
        final var phoneNumbers = existingUser.getAttributeStream(attrName).toList();
        if (!phoneNumbers.isEmpty()) {
            String phoneNumber = phoneNumbers.get(0);

            log.debugf("Forget-password triggered when re-authenticating user after first broker login. Pre-filling request-user-phone-number screen with user's phone '%s' ", phoneNumber);
            context.setUser(existingUser);
            Response challenge = context.form()
                    .setAttribute(attrName, phoneNumber)
                    .setAttribute("countries", SmsService.getAllCountries())
                    .createForm("request-user-phone-number.ftl");
            context.challenge(challenge);
            return true;
        }
        return false;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var existingUserId = context.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.EXISTING_USER_INFO);
        if (existingUserId != null) {
            UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());
            if (handlePreExistingUser(context, existingUser)) return;
        }

        final var actionTokenUserId = context.getAuthenticationSession().getAuthNote(ACTION_TOKEN_USER_ID);
        if (actionTokenUserId != null) {
            UserModel existingUser = context.getSession().users().getUserById(context.getRealm(), actionTokenUserId);
            if (handlePreExistingUser(context, existingUser)) return;
        }

        // Check if the user is already authenticated in the context (e.g., standard login flow)
        if (context.getUser() != null) {
            if (handlePreExistingUser(context, context.getUser())) return;
        }

        final var challenge = context.form()
                .setAttribute("regionPrefix", "+91")
                .setAttribute("countries", SmsService.getAllCountries())
                .createForm("request-user-phone-number.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel")) {
            context.attempted();
            return;
        }

        String phoneNumberWithoutPrefix = formData.getFirst("phone");
        String regionPrefix = formData.getFirst("regionPrefix");

        if (phoneNumberWithoutPrefix == null
                || phoneNumberWithoutPrefix.isEmpty()
                || regionPrefix == null
                || regionPrefix.isEmpty()) {
            event.error("missing_phone_number_or_region");
            Response challenge = context.form()
                    .setError("missing_phone_number_or_region")
                    .setAttribute("regionPrefix", regionPrefix)
                    .setAttribute("countries", SmsService.getAllCountries())
                    .createForm("request-user-phone-number.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        phoneNumberWithoutPrefix = StringUtils.trim(phoneNumberWithoutPrefix);

        while (phoneNumberWithoutPrefix.startsWith("0")) {
            phoneNumberWithoutPrefix = StringUtils.removeStart(phoneNumberWithoutPrefix, "0");
        }

        final var phoneNumber = regionPrefix + phoneNumberWithoutPrefix;

        final var phoneNumber$ = smsService.format(phoneNumber);
        if (phoneNumber$.isEmpty()) {
            event.clone()
                    .detail("phone_number", phoneNumber)
                    .error("parse number error: Can't parse phone number");
            context.clearUser();
            Response challenge = context
                    .form()
                    .setError("wrong_phone_number_or_region")
                    .setAttribute("phoneNumber", phoneNumberWithoutPrefix)
                    .setAttribute("regionPrefix", regionPrefix)
                    .setAttribute("countries", SmsService.getAllCountries())
                    .createForm("request-user-phone-number.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        context
                .getAuthenticationSession()
                .setAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER, phoneNumber$.get());
        context.success();
    }

    @Override
    public String getDisplayType() {
        return "SMS -1 Get Phone number";
    }

    @Override
    public String getReferenceCategory() {
        return "get-phone-number";
    }
    
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Get a user by his phone number";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
