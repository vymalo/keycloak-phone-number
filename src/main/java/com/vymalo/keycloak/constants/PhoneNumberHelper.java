package com.vymalo.keycloak.constants;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance;

public class PhoneNumberHelper {
    public static final String DEFAULT_PHONE_KEY_NAME = "phoneNumber";
    public static final PhoneNumberUtil phoneNumberUtil = getInstance();
    private static final Logger LOG = Logger.getLogger(PhoneNumberHelper.class);

    private PhoneNumberHelper() {
    }

    public static boolean handleConfiguredFor(UserModel user) {
        if (user != null) {
            final var phoneNumber = KeycloakModelUtils.resolveAttribute(user, DEFAULT_PHONE_KEY_NAME, false);
            if (!phoneNumber.isEmpty()) {
                try {
                    String firstPhoneNumber = phoneNumber.stream().findFirst().get();
                    phoneNumberUtil.parse(firstPhoneNumber, null);
                } catch (NumberParseException exception) {
                    LOG.warn(exception);
                    return false;
                }
            }
        }
        return true;
    }
}
