package com.vymalo.keycloak.msg91;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vymalo.keycloak.services.SmsGateway;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Msg91SmsGateway implements SmsGateway {
    private static final Logger LOGGER = Logger.getLogger(Msg91SmsGateway.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_PATH = "/api/v5/otp";
    private static final String VERIFY_PATH = "/api/v5/otp/verify";

    private final SmsServiceConfig config;

    public Msg91SmsGateway(SmsServiceConfig c) {
        this.config = Objects.requireNonNull(c, "config");
    }

    @Override
    public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return Optional.empty();
        }

        final var authkey = msg91AuthKey(config);
        final var templateId = msg91TemplateId(config);
        if (StringUtils.isBlank(authkey) || StringUtils.isBlank(templateId)) {
            LOGGER.severe("MSG91 authkey (basicPassword or oauthClientSecret) and template_id (oauthClientId) are required");
            return Optional.empty();
        }

        final var otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        final var mobile = normalizeMobile(phoneNumber);

        try {
            final var uri = new URIBuilder(trimTrailingSlash(config.smsApiUrl()) + OTP_PATH)
                    .addParameter("otp", otp)
                    .addParameter("mobile", mobile)
                    .addParameter("authkey", authkey)
                    .addParameter("template_id", templateId)
                    .build();

            final var body = OBJECT_MAPPER.writeValueAsString(buildTemplateVariables(requestContext));

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(uri);
                post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
                post.setHeader("Content-Type", "application/JSON");
                post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    int status = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : -1;
                    String responseBody = response.getEntity() != null
                            ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                            : "";
                    if (status < 200 || status >= 300) {
                        LOGGER.log(Level.SEVERE, "MSG91 OTP send failed status=" + status + " body=" + StringUtils.abbreviate(responseBody, 500));
                        return Optional.empty();
                    }
                    return Optional.of(parseHashFromSendResponse(responseBody));
                }
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to send SMS via MSG91", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(code)) {
            return Optional.of(false);
        }

        final var authkey = msg91AuthKey(config);
        if (StringUtils.isBlank(authkey)) {
            LOGGER.severe("MSG91 authkey (basicPassword or oauthClientSecret) is required for OTP verify");
            return Optional.empty();
        }

        final var mobile = normalizeMobile(phoneNumber);

        try {
            final var uri = new URIBuilder(trimTrailingSlash(config.smsApiUrl()) + VERIFY_PATH)
                    .addParameter("mobile", mobile)
                    .addParameter("otp", code.trim())
                    .build();

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet get = new HttpGet(uri);
                get.setHeader("authkey", authkey);
                get.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

                try (CloseableHttpResponse response = client.execute(get)) {
                    int status = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : -1;
                    String responseBody = response.getEntity() != null
                            ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                            : "";
                    if (status < 200 || status >= 300) {
                        LOGGER.fine("MSG91 OTP verify non-success status=" + status + " body=" + StringUtils.abbreviate(responseBody, 500));
                        return Optional.of(false);
                    }
                    return Optional.of(isMsg91Success(responseBody));
                }
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to verify SMS code via MSG91", e);
            return Optional.empty();
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String msg91AuthKey(SmsServiceConfig config) {
        final var fromBasic = StringUtils.trimToNull(config.basicPassword());
        if (fromBasic != null) {
            return fromBasic;
        }
        return StringUtils.trimToNull(config.oauthClientSecret());
    }

    private static String msg91TemplateId(SmsServiceConfig config) {
        return StringUtils.trimToNull(config.oauthClientId());
    }

    private static String normalizeMobile(String phoneNumber) {
        return phoneNumber.replace(" ", "").replaceFirst("^\\+", "");
    }

    /**
     * JSON object for MSG91 template variables (e.g. {@code Param1}, {@code Param2}, {@code Param3}).
     */
    private static Map<String, String> buildTemplateVariables(SmsRequestContext requestContext) {
        final var meta = requestContext != null ? requestContext.metadata() : null;
        final var m = new LinkedHashMap<String, String>();
        m.put("Param1", stringMeta(meta, "Param1", requestContext != null ? requestContext.realm() : null, ""));
        return m;
    }

    private static String stringMeta(Map<String, Object> meta, String key, String fallback, String defaultIfBlank) {
        if (meta != null && meta.containsKey(key) && meta.get(key) != null) {
            return String.valueOf(meta.get(key));
        }
        if (StringUtils.isNotBlank(fallback)) {
            return fallback;
        }
        return defaultIfBlank;
    }

    private static String parseHashFromSendResponse(String json) {
        if (StringUtils.isBlank(json)) {
            return java.util.UUID.randomUUID().toString();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root != null) {
                for (String field : new String[] {"request_id", "requestId", "hash", "otp_id", "otpId"}) {
                    if (root.has(field) && !root.get(field).isNull()) {
                        String v = root.get(field).asText(null);
                        if (StringUtils.isNotBlank(v)) {
                            return v;
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // fall through to UUID
        }
        return java.util.UUID.randomUUID().toString();
    }

    private static boolean isMsg91Success(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null) {
                return false;
            }
            if (root.has("type") && "success".equalsIgnoreCase(root.get("type").asText())) {
                return true;
            }
            return root.path("message").asText("").toLowerCase().contains("verified");
        } catch (IOException e) {
            return false;
        }
    }
}
