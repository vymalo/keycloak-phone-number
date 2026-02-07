package com.vymalo.keycloak.services;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.vymalo.api.server.handler.ApiClient;
import com.vymalo.api.server.handler.ApiException;
import com.vymalo.api.server.handler.SmsApi;
import com.vymalo.api.server.model.ConfirmTanRequest;
import com.vymalo.api.server.model.SendSmsRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JBossLog
public final class SmsService {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private static final Map<String, TokenCacheEntry> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<CountryPhoneCode>> COUNTRIES_CACHE = new ConcurrentHashMap<>();

    private final SmsApi smsApi;
    private final SmsServiceConfig config;

    public SmsService(SmsServiceConfig config) {
        this.config = Objects.requireNonNull(config, "config");

        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(config.smsApiUrl());
        apiClient.setRequestInterceptor(builder -> applyAuth(builder, config));

        this.smsApi = new SmsApi(apiClient);
    }

    public Optional<String> sendSmsAndGetHash(SmsRequestContext requestContext, String phoneNumber) {
        final var request = new SendSmsRequest()
                .phoneNumber(phoneNumber)
                .realm(requestContext != null ? requestContext.realm() : null)
                .clientId(requestContext != null ? requestContext.clientId() : null)
                .ipAddress(requestContext != null ? requestContext.ipAddress() : null)
                .userAgent(requestContext != null ? requestContext.userAgent() : null)
                .sessionId(requestContext != null ? requestContext.sessionId() : null)
                .traceId(requestContext != null ? requestContext.traceId() : null)
                .metadata(requestContext != null ? requestContext.metadata() : null);

        try {
            return Optional.ofNullable(smsApi.sendSms(request).getHash());
        } catch (ApiException e) {
            log.error("Failed to send SMS", e);
            return Optional.empty();
        }
    }

    public Optional<Boolean> confirmSmsCode(SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        final var request = new ConfirmTanRequest()
                .code(code)
                .phoneNumber(phoneNumber)
                .hash(hash)
                .realm(requestContext != null ? requestContext.realm() : null)
                .clientId(requestContext != null ? requestContext.clientId() : null)
                .ipAddress(requestContext != null ? requestContext.ipAddress() : null)
                .userAgent(requestContext != null ? requestContext.userAgent() : null)
                .sessionId(requestContext != null ? requestContext.sessionId() : null)
                .traceId(requestContext != null ? requestContext.traceId() : null)
                .metadata(requestContext != null ? requestContext.metadata() : null);

        try {
            return Optional.ofNullable(smsApi.validateSms(request).getValid());
        } catch (ApiException e) {
            log.error("Failed to validate SMS code", e);
            return Optional.empty();
        }
    }

    public static Set<CountryPhoneCode> getAllCountries(String allowedCountryPattern) {
        final String cacheKey = StringUtils.defaultString(allowedCountryPattern, "");
        return COUNTRIES_CACHE.computeIfAbsent(cacheKey, key -> computeCountries(allowedCountryPattern));
    }

    private static Set<CountryPhoneCode> computeCountries(String allowedCountryPattern) {
        final var allowedCountries = compileCountryPredicate(allowedCountryPattern);

        return PHONE_NUMBER_UTIL.getSupportedCallingCodes()
                .stream()
                .map(supportedCallingCode -> {
                    final var countryCode = PHONE_NUMBER_UTIL.getRegionCodeForCountryCode(supportedCallingCode);
                    final var locale = new Locale("", countryCode);
                    final var countryName = locale.getDisplayCountry();

                    if (!allowedCountries.test(countryCode)) {
                        return null;
                    }

                    return new CountryPhoneCode(countryName, "+" + supportedCallingCode);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(o -> o.label))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static java.util.function.Predicate<String> compileCountryPredicate(String allowedCountryPattern) {
        final String pattern = StringUtils.defaultIfBlank(allowedCountryPattern, ".*");
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).asMatchPredicate();
        } catch (Exception e) {
            log.warnf("Invalid allowed country pattern '%s'; allowing all countries", pattern);
            return Pattern.compile(".*").asMatchPredicate();
        }
    }

    private static void applyAuth(HttpRequest.Builder builder, SmsServiceConfig config) {
        final var mode = config.authMode();
        final var basicUsr = StringUtils.trimToNull(config.basicUsername());
        final var basicPwd = StringUtils.trimToNull(config.basicPassword());
        final var clientId = StringUtils.trimToNull(config.oauthClientId());
        final var clientSecret = StringUtils.trimToNull(config.oauthClientSecret());
        final var tokenEndpoint = StringUtils.trimToNull(config.oauthTokenEndpoint());

        if (mode == SmsServiceConfig.SmsAuthMode.NONE) {
            return;
        }

        if (mode == SmsServiceConfig.SmsAuthMode.BASIC) {
            setBasicAuth(builder, basicUsr, basicPwd);
            return;
        }

        if (mode == SmsServiceConfig.SmsAuthMode.OAUTH2) {
            try {
                setOAuth2(builder, clientId, clientSecret, tokenEndpoint);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to set OAuth2 Authorization header", e);
            }
            return;
        }

        // AUTO
        if (StringUtils.isNotEmpty(tokenEndpoint) && StringUtils.isNotEmpty(clientId) && StringUtils.isNotEmpty(clientSecret)) {
            try {
                setOAuth2(builder, clientId, clientSecret, tokenEndpoint);
                return;
            } catch (Exception e) {
                log.warn("Failed to set OAuth2 token, falling back to basic auth", e);
            }
        }

        setBasicAuth(builder, basicUsr, basicPwd);
    }

    private static void setBasicAuth(HttpRequest.Builder builder, String basicUsr, String basicPwd) {
        if (StringUtils.isEmpty(basicUsr) || StringUtils.isEmpty(basicPwd)) {
            return;
        }

        final var valueToEncode = basicUsr + ":" + basicPwd;
        final var basicAuth = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8));
        builder.header("Authorization", basicAuth);
    }

    private static void setOAuth2(HttpRequest.Builder builder, String clientId, String clientSecret, String tokenEndpoint) throws IOException {
        if (StringUtils.isEmpty(tokenEndpoint) || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            return;
        }

        String token = getAccessToken(clientId, clientSecret, tokenEndpoint);
        builder.header("Authorization", "Bearer " + token);
    }

    private static String getAccessToken(String clientId, String clientSecret, String tokenEndpoint) throws IOException {
        final var cacheKey = tokenEndpoint + "|" + clientId + "|" + sha256Hex(clientSecret);
        final var cached = TOKEN_CACHE.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < cached.expiresAtMillis) {
            return cached.accessToken;
        }

        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionTimeToLive(30, TimeUnit.SECONDS)
                .build()) {
            HttpPost post = new HttpPost(tokenEndpoint);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "client_credentials"));
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : -1;
                String json = response.getEntity() != null ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
                if (status < 200 || status >= 300) {
                    throw new IOException("Token endpoint returned status " + status + " body=" + StringUtils.abbreviate(json, 500));
                }

                if (StringUtils.isBlank(json)) {
                    throw new IOException("Token endpoint returned empty body");
                }

                Map<String, Object> tokenData = JsonSerialization.readValue(json, Map.class);
                String accessToken = (String) tokenData.get("access_token");
                Number expiresIn = (Number) tokenData.getOrDefault("expires_in", 3600);

                if (StringUtils.isBlank(accessToken)) {
                    throw new IOException("Token endpoint response missing access_token");
                }

                long expiresAt = System.currentTimeMillis() + (expiresIn.longValue() * 1000L) - 30_000L;
                TOKEN_CACHE.put(cacheKey, new TokenCacheEntry(accessToken, expiresAt));
                return accessToken;
            }
        } catch (Exception e) {
            throw new IOException("Failed to fetch OAuth2 token", e);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    private record TokenCacheEntry(String accessToken, long expiresAtMillis) {
    }

    @Data
    @RequiredArgsConstructor
    public static final class CountryPhoneCode {
        private final String label;
        private final String code;
    }
}
