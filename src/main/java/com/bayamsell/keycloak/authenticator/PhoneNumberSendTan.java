package com.bayamsell.keycloak.authenticator;

import com.bayamsell.api.server.handler.ApiClient;
import com.bayamsell.api.server.handler.Configuration;
import com.bayamsell.api.server.handler.SmsApi;
import com.bayamsell.api.server.model.SendSmsRequest;
import com.bayamsell.keycloak.constants.ConfigKey;
import com.bayamsell.keycloak.constants.PhoneKey;
import com.bayamsell.keycloak.constants.Utils;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.bayamsell.keycloak.constants.PhoneNumber.handleConfiguredFor;

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
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final Map<String, String> infos = new HashMap<>();
    private static final ApiClient apiClient = Configuration.getDefaultApiClient();

    static {
        infos.put("version", "1.0.0");

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
        String smsUrl = Utils.getConfigString(config, ConfigKey.CONF_PRP_SMS_URL);
        String basicUsr = Utils.getConfigString(config, ConfigKey.BASIC_AUTH_USERNAME);
        String basicPwd = Utils.getConfigString(config, ConfigKey.BASIC_AUTH_PASSWORD);

        ApiClient apiClient = Configuration.getDefaultApiClient();
        apiClient.setUsername(basicUsr);
        apiClient.setPassword(basicPwd);

        SmsApi smsApi = new SmsApi();
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumber(mobileNumber);
        request.setSmsCode(new BigDecimal(code));

        try {
            URI uri = new URI(smsUrl);
            String path = uri.getPath();
            String baseUrl = smsUrl.split(path)[0];
            apiClient.setBasePath(baseUrl);
            smsApi.sendSms(path, request);
            event.clone()
                    .detail("sms_code", code)
                    .error("SMS_SEND");
            return true;
        } catch (Throwable e) {
            event.clone()
                    .detail("sms_code", code)
                    .error("SMS_NOT_SEND: " + e.getLocalizedMessage());
            return false;
        }
    }

    private static String getSmsCode(long nrOfDigits) {
        if (nrOfDigits < 1) {
            throw new RuntimeException("Nr of digits must be bigger than 0");
        }

        double maxValue = Math.pow(10.0, nrOfDigits); // 10 ^ nrOfDigits;
        Random r = new Random();
        long code = (long) (r.nextFloat() * maxValue);
        return Long.toString(code);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String phoneNumber = authenticationSession.getAuthNote(PhoneKey.ATTEMPTED_PHONE_NUMBER);
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();

        long nrOfDigits = Utils.getConfigLong(authenticatorConfig, ConfigKey.CONF_PRP_SMS_CODE_LENGTH, 8L);
        String code = getSmsCode(nrOfDigits);
        log.infof("The code is [%s] for [%s]", code, user.getUsername());

        if (sendSmsCode(phoneNumber, code, authenticatorConfig, event)) {
            context
                    .getAuthenticationSession()
                    .setAuthNote(PhoneKey.ATTEMPTED_CODE_NAME, code);
            context.success();
        } else {
            Response challenge = context
                    .form()
                    .setError("SMS could not be sent.")
                    .createForm("sms-tan-not-send-error.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }

    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String cancel = formData.getFirst("cancel");

        if (cancel != null) {
            context.resetFlow();
            return;
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return handleConfiguredFor(user);
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
