package com.vymalo.keycloak.authenticator;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;
import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.constants.Utils;
import com.vymalo.keycloak.model.CountryPhoneCode;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import static org.keycloak.models.DefaultActionTokenKey.ACTION_TOKEN_USER_ID;

@JBossLog
@NoArgsConstructor
public class PhoneNumberGetNumber implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "get-user-phone-number";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "26.0.7");

        ProviderConfigProperty property;

        // Phone name
        property = new ProviderConfigProperty();
        property.setName(ConfigKey.USER_PHONE_ATTRIBUTE_NAME);
        property.setLabel("Phone attribute name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("User phone names's attribute");
        configProperties.add(property);
    }

    private static boolean handlePreExistingUser(AuthenticationFlowContext context, UserModel existingUser) {
        String attrName = Utils.getConfigString(context.getAuthenticatorConfig(), ConfigKey.USER_PHONE_ATTRIBUTE_NAME, PhoneNumberHelper.DEFAULT_PHONE_KEY_NAME);
        final var phoneNumbers = existingUser.getAttributeStream(attrName).toList();
        if (!phoneNumbers.isEmpty()) {
            String phoneNumber = phoneNumbers.get(0);

            log.debugf("Forget-password triggered when re-authenticating user after first broker login. Pre-filling request-user-phone-number screen with user's phone '%s' ", phoneNumber);
            context.setUser(existingUser);
            Response challenge = context.form()
                    .setAttribute(attrName, phoneNumber)
                    .setAttribute("countries", CountryPhoneCode.allCountries)
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

        final var challenge = context.form()
                .setAttribute("regionPrefix", "")
                .setAttribute("countries", CountryPhoneCode.allCountries)
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
                    .setAttribute("countries", CountryPhoneCode.allCountries)
                    .createForm("request-user-phone-number.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        phoneNumberWithoutPrefix = StringUtils.trim(phoneNumberWithoutPrefix);

        while (phoneNumberWithoutPrefix.startsWith("0")) {
            phoneNumberWithoutPrefix = StringUtils.removeStart(phoneNumberWithoutPrefix, "0");
        }

        var phoneNumber = regionPrefix + phoneNumberWithoutPrefix;

        Phonenumber.PhoneNumber number;
        try {
            number = PhoneNumberHelper.phoneNumberUtil.parse(phoneNumber, null);
        } catch (NumberParseException e) {
            event.clone()
                    .detail("phone_number", phoneNumber)
                    .error("parse number error: " + e.getMessage());
            context.clearUser();
            Response challenge = context
                    .form()
                    .setError("wrong_phone_number_or_region")
                    .setAttribute("phoneNumber", phoneNumberWithoutPrefix)
                    .setAttribute("regionPrefix", regionPrefix)
                    .setAttribute("countries", CountryPhoneCode.allCountries)
                    .createForm("request-user-phone-number.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        phoneNumber = PhoneNumberHelper.phoneNumberUtil.format(number, PhoneNumberFormat.E164);

        context
                .getAuthenticationSession()
                .setAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER, phoneNumber);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return PhoneNumberHelper.handleConfiguredFor(user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

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
    public boolean isConfigurable() {
        return true;
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
        return "Get a user by his phone number";
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
