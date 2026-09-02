package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * The server-side API signing secret. When {@code dataverse.api.signing-secret} is set (and long
 * enough), that value is used, so signed URLs survive restarts and validate on every server of a
 * multi-server installation. Otherwise a strong random secret is generated at startup and kept only
 * in memory: it is never stored anywhere, and restarting the server invalidates previously issued
 * signed URLs (which are short-lived anyway).
 */
@Startup
@Singleton
@Lock(LockType.READ)
public class ApiSigningSecretServiceBean {

    /**
     * Admins may configure their own secret, but a short one would allow offline brute-forcing of
     * the signing key from a single captured signed URL.
     */
    public static final int MIN_SECRET_LENGTH = 36;

    private static final Logger logger = Logger.getLogger(ApiSigningSecretServiceBean.class.getCanonicalName());
    // deliberately not getInstanceStrong(), which can block on entropy on a fresh VM
    private static final SecureRandom secureRandom = new SecureRandom();

    // package-private so FixedSigningSecret can serve a fixed value in tests
    String secret;

    @PostConstruct
    void init() {
        String configured = JvmSettings.API_SIGNING_SECRET.lookupOptional().orElse("");
        if (configured.length() >= MIN_SECRET_LENGTH) {
            secret = configured;
            logger.info("Using the configured API signing secret (dataverse.api.signing-secret).");
            return;
        }
        if (!configured.isEmpty()) {
            logger.warning("Ignoring dataverse.api.signing-secret: it is shorter than " + MIN_SECRET_LENGTH
                    + " characters, which would weaken the signing key. Using a generated secret instead.");
        }
        byte[] bytes = new byte[MIN_SECRET_LENGTH];
        secureRandom.nextBytes(bytes);
        secret = Base64.getEncoder().encodeToString(bytes);
        logger.warning("No persistent API signing secret is configured; generated a temporary one. Signed URLs"
                + " will not survive a server restart, and on a multi-server installation each server signs"
                + " with its own secret. Set dataverse.api.signing-secret (at least " + MIN_SECRET_LENGTH
                + " characters) if you need either.");
    }

    public String getSecret() {
        return secret;
    }

    public String getSigningKey(String userApiToken) {
        return getSecret() + userApiToken;
    }
}
