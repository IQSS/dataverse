package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server-generated API signing secret ({@link Key#ApiSigningSecret}): generated on first use
 * and stored in the database, which stays the single source of truth - deleting the setting
 * rotates the secret, and every node picks a change up immediately. Across servers, the unique
 * constraint on the setting table decides a first-generation race.
 */
@Stateless
public class ApiSigningSecretServiceBean {

    /**
     * Admins may store their own secret via the settings API, but a short one would allow offline
     * brute-forcing of the signing key from a single captured signed URL.
     */
    public static final int MIN_SECRET_LENGTH = 32;

    private static final Logger logger = Logger.getLogger(ApiSigningSecretServiceBean.class.getCanonicalName());
    // deliberately not getInstanceStrong(), which can block on entropy on a fresh VM
    private static final SecureRandom secureRandom = new SecureRandom();

    @EJB
    SettingsServiceBean settingsService;

    public String getSecret() {
        String secret = settingsService.getValueForKey(Key.ApiSigningSecret);
        if (secret == null || secret.isEmpty()) {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            secret = Base64.getEncoder().encodeToString(bytes);
            try {
                settingsService.setValueForKey(Key.ApiSigningSecret, secret);
            } catch (PersistenceException raceLost) {
                logger.log(Level.FINE, "Another node stored the API signing secret first; adopting it.", raceLost);
                secret = settingsService.getValueForKey(Key.ApiSigningSecret);
            }
        }
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("Could not read or generate the API signing secret.");
        }
        return secret;
    }

    public String getSigningKey(String userApiToken) {
        return getSecret() + userApiToken;
    }
}
