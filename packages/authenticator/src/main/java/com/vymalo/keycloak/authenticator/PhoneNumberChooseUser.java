package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.PhoneKey;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserProvider;

import java.util.Collections;

@JBossLog
@NoArgsConstructor
public class PhoneNumberChooseUser extends AbstractPhoneNumberAuthenticator {

    public static final String PROVIDER_ID = "phone-number-choose-user";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var event = context.getEvent();
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);

        final var realm = context.getRealm();
        final var attrName = phoneAttributeName(context);

        var user = context.getUser();

        if (user != null && !user.isEnabled()) {
            event.detail("phone_number", phoneNumber)
                    .user(user)
                    .error(Errors.USER_DISABLED);
            context.clearUser();
            context.resetFlow();
            return;
        }

        if (user == null) {
            UserProvider userProvider = context.getSession().users();
            final var users = userProvider
                    .searchForUserByUserAttributeStream(realm, attrName, phoneNumber)
                    .toList();

            if (users.size() > 1) {
                log.warnf("Multiple users match %s=%s; using the first match", attrName, phoneNumber);
            }

            if (!users.isEmpty()) {
                user = users.get(0);
            }
        }

        if (user != null && user.isEnabled()) {
            user.setAttribute(attrName, Collections.singletonList(phoneNumber));
            context.setUser(user);
        } else {
            context.clearUser();
        }

        context
                .getAuthenticationSession()
                .setAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER, phoneNumber);

        context.success();
    }

    @Override
    public String getDisplayType() {
        return "SMS -3 Choose user by Phone number";
    }

    @Override
    public String getReferenceCategory() {
        return "phone-choose";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Resolve a user by phone number (without creating a user before phone verification)";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
