package com.vymalo.keycloak.authenticator;

import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.PhoneNumberHelper;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsService;
import com.vymalo.keycloak.services.SmsServiceConfig;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractPhoneNumberAuthenticator implements
        Authenticator,
        AuthenticatorFactory,
        ServerInfoAwareProviderFactory {

    public static final String CFG_SMS_API_URL = "smsApiUrl";
    public static final String CFG_ALLOWED_COUNTRY_PATTERN = "allowedCountryPattern";
    public static final String CFG_USER_PHONE_ATTRIBUTE_NAME = "userPhoneAttributeName";
    public static final String CFG_SMS_PROVIDER = "smsProvider";
    public static final String CFG_SMS_AUTH_MODE = "smsAuthMode";
    public static final String CFG_OAUTH_TOKEN_ENDPOINT = "oauthTokenEndpoint";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Map<String, String> infos = new HashMap<>();

    static {
        infos.put("version", "26.5.2");
        infos.put("repo", "https://github.com/vymalo/keycloak-phone-number");

        configProperties.add(buildString(CFG_SMS_API_URL, "SMS API URL", "Base URL of the SMS API (non-secret). Fallback: env SMS_API_URL"));
        configProperties.add(buildString(CFG_ALLOWED_COUNTRY_PATTERN, "Allowed Countries", "Regex on ISO region code (ex: cm|de|fr). Fallback: env SMS_API_COUNTRY_PATTERN"));
        configProperties.add(buildString(CFG_USER_PHONE_ATTRIBUTE_NAME, "Phone Attribute", "User attribute used to store the phone number. Fallback: env SMS_API_ATTRIBUTE_PHONE_NAME (default: phoneNumber)"));

        ProviderConfigProperty provider = new ProviderConfigProperty();
        provider.setName(CFG_SMS_PROVIDER);
        provider.setLabel("SMS Provider");
        provider.setHelpText("How TAN messages are dispatched. OPENAPI is implemented; QUEUE and TWILIO are placeholders for now.");
        provider.setType(ProviderConfigProperty.LIST_TYPE);
        provider.setOptions(List.of("OPENAPI", "QUEUE", "TWILIO"));
        provider.setDefaultValue("OPENAPI");
        configProperties.add(provider);

        ProviderConfigProperty authMode = new ProviderConfigProperty();
        authMode.setName(CFG_SMS_AUTH_MODE);
        authMode.setLabel("SMS Auth Mode");
        authMode.setHelpText("How to authenticate against the SMS API: AUTO (default), BASIC, OAUTH2, NONE. Secrets stay in env vars.");
        authMode.setType(ProviderConfigProperty.LIST_TYPE);
        authMode.setOptions(List.of("AUTO", "BASIC", "OAUTH2", "NONE"));
        authMode.setDefaultValue("AUTO");
        configProperties.add(authMode);

        configProperties.add(buildString(CFG_OAUTH_TOKEN_ENDPOINT, "OAuth2 Token Endpoint", "OAuth2 token endpoint (non-secret). Fallback: env OAUTH2_TOKEN_ENDPOINT"));
    }

    private static ProviderConfigProperty buildString(String name, String label, String helpText) {
        ProviderConfigProperty prop = new ProviderConfigProperty();
        prop.setName(name);
        prop.setLabel(label);
        prop.setHelpText(helpText);
        prop.setType(ProviderConfigProperty.STRING_TYPE);
        return prop;
    }

    protected final String phoneAttributeName(AuthenticationFlowContext context) {
        return AuthenticatorConfigResolver.getConfigOrEnv(
                context,
                CFG_USER_PHONE_ATTRIBUTE_NAME,
                ConfigKey.USER_PHONE_ATTRIBUTE_NAME,
                PhoneNumberHelper.DEFAULT_PHONE_KEY_NAME
        );
    }

    protected final String allowedCountryPattern(AuthenticationFlowContext context) {
        return AuthenticatorConfigResolver.getConfigOrEnv(
                context,
                CFG_ALLOWED_COUNTRY_PATTERN,
                ConfigKey.ALLOWED_COUNTRY_PATTERN,
                ".*"
        );
    }

    protected final SmsService smsService(AuthenticationFlowContext context) {
        final String provider = AuthenticatorConfigResolver.getConfig(context, CFG_SMS_PROVIDER).orElse("OPENAPI").trim().toUpperCase();
        if (!"OPENAPI".equals(provider)) {
            throw new IllegalStateException("smsProvider=" + provider + " is not implemented yet. Use OPENAPI for now.");
        }

        final String smsApiUrl = AuthenticatorConfigResolver.getConfigOrEnv(
                context,
                CFG_SMS_API_URL,
                ConfigKey.CONF_PRP_SMS_URL,
                ""
        );

        final String authModeRaw = AuthenticatorConfigResolver.getConfig(context, CFG_SMS_AUTH_MODE).orElse("AUTO");
        SmsServiceConfig.SmsAuthMode authMode;
        try {
            authMode = SmsServiceConfig.SmsAuthMode.valueOf(authModeRaw.trim().toUpperCase());
        } catch (Exception ignored) {
            authMode = SmsServiceConfig.SmsAuthMode.AUTO;
        }

        final String oauthTokenEndpoint = AuthenticatorConfigResolver.getConfigOrEnv(
                context,
                CFG_OAUTH_TOKEN_ENDPOINT,
                "OAUTH2_TOKEN_ENDPOINT",
                null
        );

        // Secrets: only env/system properties.
        final String basicUsr = System.getenv(ConfigKey.BASIC_AUTH_USERNAME) != null
                ? System.getenv(ConfigKey.BASIC_AUTH_USERNAME)
                : System.getProperty(ConfigKey.BASIC_AUTH_USERNAME);
        final String basicPwd = System.getenv(ConfigKey.BASIC_AUTH_PASSWORD) != null
                ? System.getenv(ConfigKey.BASIC_AUTH_PASSWORD)
                : System.getProperty(ConfigKey.BASIC_AUTH_PASSWORD);

        final String clientId = System.getenv("OAUTH2_CLIENT_ID") != null
                ? System.getenv("OAUTH2_CLIENT_ID")
                : System.getProperty("OAUTH2_CLIENT_ID");
        final String clientSecret = System.getenv("OAUTH2_CLIENT_SECRET") != null
                ? System.getenv("OAUTH2_CLIENT_SECRET")
                : System.getProperty("OAUTH2_CLIENT_SECRET");

        SmsServiceConfig cfg = new SmsServiceConfig(
                smsApiUrl,
                authMode,
                oauthTokenEndpoint,
                basicUsr,
                basicPwd,
                clientId,
                clientSecret
        );

        return new SmsService(cfg);
    }

    protected final SmsRequestContext smsRequestContext(AuthenticationFlowContext context, String step) {
        final var authSession = context.getAuthenticationSession();
        final var client = authSession != null ? authSession.getClient() : null;
        final var connection = context.getConnection();

        String realm = context.getRealm() != null ? context.getRealm().getName() : null;
        String clientId = client != null ? client.getClientId() : null;
        String ipAddress = connection != null ? connection.getRemoteAddr() : null;
        String userAgent = context.getHttpRequest() != null && context.getHttpRequest().getHttpHeaders() != null
                ? context.getHttpRequest().getHttpHeaders().getHeaderString("User-Agent")
                : null;
        String sessionId = authSession != null ? authSession.getParentSession().getId() : null;

        Map<String, Object> metadata = new HashMap<>();
        if (step != null) {
            metadata.put("step", step);
        }

        return new SmsRequestContext(realm, clientId, ipAddress, userAgent, sessionId, UUID.randomUUID().toString(), metadata);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public boolean isConfigurable() {
        return true;
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
    public Map<String, String> getOperationalInfo() {
        return infos;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }
}
