package com.vymalo.keycloak.services;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PhoneCountryService {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    private static final Map<String, Set<CountryPhoneCode>> COUNTRIES_CACHE = new ConcurrentHashMap<>();

    private PhoneCountryService() {
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
                .sorted(Comparator.comparing(CountryPhoneCode::label))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static java.util.function.Predicate<String> compileCountryPredicate(String allowedCountryPattern) {
        final String pattern = StringUtils.defaultIfBlank(allowedCountryPattern, ".*");
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).asMatchPredicate();
        } catch (Exception e) {
            return Pattern.compile(".*").asMatchPredicate();
        }
    }
}
