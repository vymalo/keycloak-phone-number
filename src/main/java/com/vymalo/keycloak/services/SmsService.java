package com.vymalo.keycloak.services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.vymalo.api.server.handler.ApiClient;
import com.vymalo.api.server.handler.ApiException;
import com.vymalo.api.server.handler.SmsApi;
import com.vymalo.api.server.model.ConfirmTanRequest;
import com.vymalo.api.server.model.SendSmsRequest;
import com.vymalo.keycloak.constants.ConfigKey;
import com.vymalo.keycloak.constants.Utils;
import lombok.*;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JBossLog
public class SmsService {
    public static final String DEFAULT_PHONE_KEY_NAME = "phoneNumber";
    public static final PhoneNumberUtil phoneNumberUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();
    @Getter
    private static final Set<CountryPhoneCode> allCountries;

    @Getter
    private static final SmsService instance;

    static {
        final var smsUrl = Utils.getEnv(ConfigKey.CONF_PRP_SMS_URL);
        final var basicUsr = Utils.getEnv(ConfigKey.BASIC_AUTH_USERNAME);
        final var basicPwd = Utils.getEnv(ConfigKey.BASIC_AUTH_PASSWORD);
        final var allowedCountries = Pattern.compile(Utils.getEnv(ConfigKey.ALLOWED_COUNTRY_PATTERN, "*"), Pattern.CASE_INSENSITIVE).asMatchPredicate();

        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(smsUrl);
        apiClient.setRequestInterceptor(builder -> {
            if (StringUtils.isNotEmpty(basicUsr) && StringUtils.isNotEmpty(basicPwd)) {
                final var valueToEncode = basicUsr + ":" + basicPwd;
                final var basicAuth = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
                builder.header("Authorization", basicAuth);
            }
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
        final var request = new SendSmsRequest()
                .phoneNumber(phoneNumber);

        try {
            return Optional.ofNullable(smsApi.sendSms(request).getHash());
        } catch (ApiException e) {
            log.error("Failed to send SMS", e);
            return Optional.empty();
        }
    }

    public Optional<Boolean> confirmSmsCode(String phoneNumber, String code, String hash) {
        final var request = new ConfirmTanRequest()
                .code(code)
                .phoneNumber(phoneNumber)
                .hash(hash);

        try {
            return Optional.of(smsApi.validateSms(request).getValid());
        } catch (ApiException e) {
            log.error("Failed to validate SMS code", e);
            return Optional.empty();
        }
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
