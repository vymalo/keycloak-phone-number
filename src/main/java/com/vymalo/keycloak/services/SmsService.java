package com.vymalo.keycloak.services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.vymalo.api.server.handler.ApiClient;
import com.vymalo.api.server.handler.ApiException;
import com.vymalo.api.server.handler.SmsApi;
import com.vymalo.api.server.model.SendSmsRequest;
import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JBossLog
public class SmsService {
    public static final String DEFAULT_PHONE_KEY_NAME = "phoneNumber";
    public static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    
    private static final String AUTH_KEY = "Zt00qvsDXZiWrfqPFJjj";
    private static final String AUTH_TOKEN = "ed3dxCIPJ0WMD1ZUbvz6gIOsfTpQsV5pKNVrEgnS";
    private static final String SENDER_ID = "iheal";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Getter
    private static final Set<CountryPhoneCode> allCountries;

    @Getter
    private static final SmsService instance;

    static {
        // Use SMSCountry URL with override capability for testing
        final var smsUrl = Utils.getEnv(ConfigKey.CONF_PRP_SMS_URL, "https://restapi.smscountry.com/v0.1");
        final var allowedCountries = Pattern.compile(Utils.getEnv(ConfigKey.ALLOWED_COUNTRY_PATTERN, ".*"), Pattern.CASE_INSENSITIVE).asMatchPredicate();

        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(smsUrl);
        
        // Configure Basic Auth manually via interceptor since ApiClient doesn't expose setUsername/setPassword directly for this generator version
        apiClient.setRequestInterceptor(builder -> {
            String valueToEncode = AUTH_KEY + ":" + AUTH_TOKEN;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", basicAuth);
        });

        instance = new SmsService(apiClient);

        allCountries = phoneNumberUtil.getSupportedCallingCodes()
                .stream()
                .map(supportedCallingCode -> {
                    final var countryCode = phoneNumberUtil.getRegionCodeForCountryCode(supportedCallingCode);
                    final var locale = new Locale("", countryCode);
                    final var countryName = locale.getDisplayCountry();

                    if (!allowedCountries.test(countryCode)) {
                        log.debug(String.format("Country %s (%s) is not allowed", countryName, countryCode));
                        return null;
                    }

                    log.debug(String.format("Country %s (%s) is allowed", countryName, countryCode));
                    return new CountryPhoneCode(countryName, "+" + supportedCallingCode);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(o -> o.label))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private final SmsApi smsApi;

    private SmsService(ApiClient apiClient) {
        this.smsApi = new SmsApi(apiClient);
    }

    public Optional<String> sendSmsAndGetHash(String phoneNumber) {
        log.debugf("Attempting to send SMS to %s", phoneNumber);
        // 1. Generate OTP
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        log.debugf("Generated OTP: %s", code);

        // 2. Send SMS
        final var request = new SendSmsRequest()
                .text(String.format("Dear Customer, Your OTP to log in to iHeal is %s. This OTP is valid for 10 minutes. Please do not share it with anyone. – iHeal – Your Everyday Wellness Companion!", code))
                .number(phoneNumber.replace("+", "")) // Strip '+' as required by SMSCountry API
                .senderId(SENDER_ID)
                .tool("API"); // Add tool parameter as seen in successful curl

        try {
            // API call now uses configured Basic Auth in ApiClient
            // We pass the AUTH_KEY as the path parameter because the OpenAPI spec defines it as /Accounts/{authKey}/SMSes/
            log.debugf("Sending request to SMSCountry with AuthKey in path: %s", AUTH_KEY);
            smsApi.sendSms(AUTH_KEY, request);
            log.debug("SMS sent successfully via API");
            
            // 3. Generate Hash
            String hash = calculateHash(phoneNumber, code);
            log.debugf("Generated hash for validation: %s", hash);
            return Optional.of(hash);
        } catch (ApiException e) {
            log.errorf("Failed to send SMS via SMSCountry. Status: %s, Body: %s", e.getCode(), e.getResponseBody());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error generating hash or processing SMS", e);
            return Optional.empty();
        }
    }

    public Optional<Boolean> confirmSmsCode(String phoneNumber, String code, String hash) {
        try {
            String expectedHash = calculateHash(phoneNumber, code);
            return Optional.of(expectedHash.equals(hash));
        } catch (Exception e) {
            log.error("Failed to validate SMS code", e);
            return Optional.empty();
        }
    }

    private String calculateHash(String phoneNumber, String code) throws NoSuchAlgorithmException, InvalidKeyException {
        String data = phoneNumber + code;
        Mac sha256_HMAC = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secret_key = new SecretKeySpec(AUTH_TOKEN.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    public boolean handleConfiguredFor(UserModel user) {
        final var phoneNumber = KeycloakModelUtils.resolveAttribute(user, DEFAULT_PHONE_KEY_NAME, false);
        if (!phoneNumber.isEmpty()) {
            final var firstPhoneNumber = phoneNumber.stream().findFirst().flatMap(this::parse);
            return firstPhoneNumber.isPresent();
        }
        return true;
    }

    public Optional<Phonenumber.PhoneNumber> parse(String phoneNumber) {
        try {
            return Optional.ofNullable(phoneNumberUtil.parse(phoneNumber, null));
        } catch (NumberParseException err) {
            log.warn(err);
            return Optional.empty();
        }
    }

    public Optional<String> format(String phoneNumber) {
        final var parsed$ = parse(phoneNumber);
        return parsed$.map(parsed -> phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
    }

    @Data
    @RequiredArgsConstructor
    public static final class CountryPhoneCode {
        private final String label;
        private final String code;
    }
}
