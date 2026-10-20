package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.util.StringUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;

public final class OrcidSignUpTokens {

    public static final long STATE_TIMEOUT_MILLIS = 15L * 60 * 1000;
    public static final long ORCID_TOKEN_TIMEOUT_MILLIS = 30L * 60 * 1000;

    private static final String STATE_PURPOSE = "signup-orcid-state";
    private static final String ORCID_TOKEN_PURPOSE = "signup-orcid";
    private static final String SEPARATOR = "~";
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrcidSignUpTokens() {
    }

    public static String createState(String redirectUri, String secret, long nowMillis) {
        String nonce = new BigInteger(64, RANDOM).toString(36);
        return StringUtil.encrypt(String.join(SEPARATOR, STATE_PURPOSE, Long.toString(nowMillis), nonce, redirectUri), secret);
    }

    public static boolean isValidState(String state, String redirectUri, String secret, long nowMillis) {
        return decryptFields(state, secret, 4)
                .filter(fields -> STATE_PURPOSE.equals(fields[0]))
                .filter(fields -> isWithin(fields[1], nowMillis, STATE_TIMEOUT_MILLIS))
                .filter(fields -> fields[3].equals(redirectUri))
                .isPresent();
    }

    public static String createOrcidToken(String orcidUrl, String secret, long nowMillis) {
        return StringUtil.encrypt(String.join(SEPARATOR, ORCID_TOKEN_PURPOSE, Long.toString(nowMillis), orcidUrl), secret);
    }

    public static Optional<String> readOrcidToken(String orcidToken, String secret, long nowMillis) {
        return decryptFields(orcidToken, secret, 3)
                .filter(fields -> ORCID_TOKEN_PURPOSE.equals(fields[0]))
                .filter(fields -> isWithin(fields[1], nowMillis, ORCID_TOKEN_TIMEOUT_MILLIS))
                .map(fields -> fields[2])
                .filter(orcidUrl -> !orcidUrl.isBlank());
    }

    private static Optional<String[]> decryptFields(String value, String secret, int expectedFields) {
        if (value == null || value.isBlank() || secret == null) {
            return Optional.empty();
        }
        try {
            String[] fields = StringUtil.decrypt(value, secret).split(SEPARATOR, expectedFields);
            return fields.length == expectedFields ? Optional.of(fields) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static boolean isWithin(String createdMillis, long nowMillis, long timeoutMillis) {
        try {
            long age = nowMillis - Long.parseLong(createdMillis);
            return age >= 0 && age < timeoutMillis;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
