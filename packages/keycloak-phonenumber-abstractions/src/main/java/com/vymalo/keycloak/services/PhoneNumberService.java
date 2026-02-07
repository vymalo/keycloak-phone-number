package com.vymalo.keycloak.services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Optional;

public final class PhoneNumberService {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private PhoneNumberService() {
    }

    public static Optional<Phonenumber.PhoneNumber> parse(String phoneNumber) {
        try {
            return Optional.ofNullable(PHONE_NUMBER_UTIL.parse(phoneNumber, null));
        } catch (NumberParseException err) {
            return Optional.empty();
        }
    }

    public static Optional<String> formatE164(String phoneNumber) {
        return parse(phoneNumber).map(parsed -> PHONE_NUMBER_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
    }
}
