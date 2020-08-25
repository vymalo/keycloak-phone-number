package com.bayamsell.keycloak.constants;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance;

public class PhoneNumber {

    public static final String DEFAULT_PHONE_KEY_NAME = "phoneNumber";

    public static final PhoneNumberUtil phoneNumberUtil = getInstance();

    public static boolean handleConfiguredFor(UserModel user) {
        if (user != null) {
            String phoneNumber = KeycloakModelUtils.resolveFirstAttribute(user, DEFAULT_PHONE_KEY_NAME);
            if (phoneNumber != null) {
                try {
                    phoneNumberUtil.parse(phoneNumber, null);
                } catch (NumberParseException ignored) {
                    return false;
                }
            }
        }
        return true;
    }
}
