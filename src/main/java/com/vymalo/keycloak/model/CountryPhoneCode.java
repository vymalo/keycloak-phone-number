package com.vymalo.keycloak.model;

import com.vymalo.keycloak.constants.PhoneNumberHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public final class CountryPhoneCode {

    public static final Set<CountryPhoneCode> allCountries;

    static {
        final var countries = new HashSet<CountryPhoneCode>();
        final var supportedCallingCodes = PhoneNumberHelper.phoneNumberUtil.getSupportedCallingCodes();

        for (final var supportedCallingCode : supportedCallingCodes) {
            String countryCode = PhoneNumberHelper.phoneNumberUtil.getRegionCodeForCountryCode(supportedCallingCode);
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CountryPhoneCode) obj;
        return Objects.equals(this.label, that.label) &&
                Objects.equals(this.code, that.code);
    }

}
