package com.bayamsell.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bayamsell.keycloak.constants.PhoneNumber.phoneNumberUtil;

@AllArgsConstructor
@Getter
public class CountryPhoneCode {

    public static final Set<CountryPhoneCode> allCountries;

    static {
        HashSet<CountryPhoneCode> countries = new HashSet<>();
        Set<Integer> supportedCallingCodes = phoneNumberUtil.getSupportedCallingCodes();

        for (Integer supportedCallingCode : supportedCallingCodes) {
            String countryCode = phoneNumberUtil.getRegionCodeForCountryCode(supportedCallingCode);
            Locale locale = new Locale("", countryCode);
            String countryName = locale.getDisplayCountry();
            countries.add(new CountryPhoneCode(countryName, "+" + supportedCallingCode));
        }
        allCountries = countries
                .stream()
                .sorted(Comparator.comparing(o -> o.label))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private final String label;
    private final String code;
}
