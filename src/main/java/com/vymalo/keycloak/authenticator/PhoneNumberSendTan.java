package com.vymalo.keycloak.authenticator;

import com.vymalo.api.server.handler.ApiClient;
import com.vymalo.api.server.handler.Configuration;
import com.vymalo.api.server.handler.SmsApi;
import com.vymalo.api.server.model.SendSmsRequest;
import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.constants.Utils;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

@JBossLog
@NoArgsConstructor
public class PhoneNumberSendTan implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "phone-number-send-tan";
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };
    private static final Logger LOG = Logger.getLogger(PhoneNumberSendTan.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "26.0.7");

        ProviderConfigProperty property;

        // SMS Length
        property = new ProviderConfigProperty();
        property.setName(ConfigKey.CONF_PRP_SMS_CODE_LENGTH);
        property.setLabel("Length of the SMS code");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Length of the SMS code.");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ConfigKey.CONF_PRP_SMS_URL);
        property.setLabel("URL of SMS gateway");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("URL of SMS gateway");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ConfigKey.BASIC_AUTH_USERNAME);
        property.setLabel("Username for basic auth");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ConfigKey.BASIC_AUTH_PASSWORD);
        property.setLabel("Password for basic auth");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setHelpText("");
        configProperties.add(property);
    }

    // Send an SMS
    private static boolean sendSmsCode(String mobileNumber, String code, AuthenticatorConfigModel config, EventBuilder event) {
        final var smsUrl = Utils.getConfigString(config, ConfigKey.CONF_PRP_SMS_URL);
        final var basicUsr = Utils.getConfigString(config, ConfigKey.BASIC_AUTH_USERNAME);
        final var basicPwd = Utils.getConfigString(config, ConfigKey.BASIC_AUTH_PASSWORD);

        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(smsUrl);
        apiClient.setRequestInterceptor(builder -> {
            if (StringUtils.isNotEmpty(basicUsr) && StringUtils.isNotEmpty(basicPwd)) {
                String valueToEncode = basicUsr + ":" + basicPwd;
                final var basicAuth = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
                builder.header("Authorization", basicAuth);
            }
        });

        final var request = new SendSmsRequest();
        request.setPhoneNumber(mobileNumber);
        request.setSmsCode(new BigDecimal(code));

        try {
            final var smsApi = new SmsApi(apiClient);
            smsApi.sendSms(request);

            event.clone()
                    .detail("sms_code", code)
                    .error("SMS_SEND");
            return true;
        } catch (Throwable e) {
            event.clone()
                    .detail("sms_code", code)
                    .error("SMS_NOT_SEND: " + e.getLocalizedMessage());
            LOG.error(e);
            return false;
        }
    }

    private static String getSmsCode(long nrOfDigits) {
        if (nrOfDigits < 1) {
            throw new RuntimeException("Nr of digits must be bigger than 0");
        }

        final var maxValue = Math.pow(10.0, nrOfDigits); // 10 ^ nrOfDigits;
        final var r = new Random();
        final var code = (long) (r.nextFloat() * maxValue);
        return Long.toString(code);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        final var event = context.getEvent();
        final var user = context.getUser();
        final var authenticationSession = context.getAuthenticationSession();
        final var phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        final var authenticatorConfig = context.getAuthenticatorConfig();

        final var nrOfDigits = Utils.getConfigLong(authenticatorConfig, ConfigKey.CONF_PRP_SMS_CODE_LENGTH, 8L);
        final var code = getSmsCode(nrOfDigits);
        log.debugf("The code is [%s] for [%s]", code, user.getUsername());

        if (sendSmsCode(phoneNumber, code, authenticatorConfig, event)) {
            context
                    .getAuthenticationSession()
                    .setAuthNote(PhoneKey.ATTEMPTED_CODE_NAME, code);
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
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return PhoneNumberHelper.handleConfiguredFor(user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

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
    public boolean isConfigurable() {
        return true;
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
