package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserProvider;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;

import java.util.Collections;

@JBossLog
@NoArgsConstructor
public class PhoneNumberValidateTan extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-validate-tan";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var hash = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_HASH);

        if (hash == null || phoneNumber == null) {
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
        final var authenticationSession = context.getAuthenticationSession();
        final var hash = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_HASH);
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var realm = context.getRealm();

        final var formData = context.getHttpRequest().getDecodedFormParameters();
        final var cancel = formData.getFirst("cancel");
        if (cancel != null) {
            context.resetFlow();
            return;
        }

        final var code = formData.getFirst("code");
        final var validate$ = smsService(context).confirmSmsCode(
                smsRequestContext(context, "validate_tan"),
                phoneNumber,
                code,
                hash
        );

        if (validate$.isPresent() && validate$.get()) {
            var user = context.getUser();

            if (user == null) {
                final var attrName = phoneAttributeName(context);
                final UserProvider userProvider = context.getSession().users();
                final var users = userProvider.searchForUserByUserAttributeStream(realm, attrName, phoneNumber).toList();

                if (!users.isEmpty()) {
                    if (users.size() > 1) {
                        log.warnf("Multiple users match %s=%s; using the first match", attrName, phoneNumber);
                    }
                    user = users.get(0);
                } else {
                    final var userByUsername = userProvider.getUserByUsername(realm, phoneNumber);
                    user = userByUsername != null ? userByUsername : userProvider.addUser(realm, phoneNumber);
                    user.setEnabled(true);
                }

                if (!user.isEnabled()) {
                    context.resetFlow();
                    return;
                }

                user.setAttribute(attrName, Collections.singletonList(phoneNumber));
            }

            context.setUser(user);
            context.success();
        } else {
            final var event = context.getEvent();
            event.error(Errors.INVALID_CODE);

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
        return false;
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
    public String getId() {
        return PROVIDER_ID;
    }
}
