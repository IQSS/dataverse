package edu.harvard.iq.dataverse.util;

import java.util.logging.Logger;

/**
 * System-wide configuration
 */
public class SystemConfig {

    private static final Logger logger = Logger.getLogger(SystemConfig.class.getCanonicalName());

    /**
     * A JVM option for the advertised fully qualified domain name (hostname) of
     * the Dataverse installation, such as "dataverse.example.com", which may
     * differ from the hostname that the server knows itself as.
     *
     * The equivalent in DVN 3.x was "dvn.inetAddress".
     */
    public static final String FQDN = "dataverse.fqdn";

    /**
     * A JVM option to override the number of minutes for which a password reset
     * token is valid ({@link #minutesUntilPasswordResetTokenExpires}).
     */
    private static final String PASSWORD_RESET_TIMEOUT_IN_MINUTES = "dataverse.auth.password-reset-timeout-in-minutes";

    /**
     * The number of minutes for which a password reset token is valid. Can be
     * overridden by {@link #PASSWORD_RESET_TIMEOUT_IN_MINUTES}.
     */
    public static int getMinutesUntilPasswordResetTokenExpires() {
        final int reasonableDefault = 60;
        String configuredValueAsString = System.getProperty(PASSWORD_RESET_TIMEOUT_IN_MINUTES);
        if (configuredValueAsString != null) {
            int configuredValueAsInteger = 0;
            try {
                configuredValueAsInteger = Integer.parseInt(configuredValueAsString);
                if (configuredValueAsInteger > 0) {
                    return configuredValueAsInteger;
                } else {
                    logger.info(PASSWORD_RESET_TIMEOUT_IN_MINUTES + " is configured as a negative number \"" + configuredValueAsInteger + "\". Using default value instead: " + reasonableDefault);
                    return reasonableDefault;
                }
            } catch (NumberFormatException ex) {
                logger.info("Unable to convert " + PASSWORD_RESET_TIMEOUT_IN_MINUTES + " from \"" + configuredValueAsString + "\" into an integer value: " + ex + ". Using default value " + reasonableDefault);
            }
        }
        return reasonableDefault;
    }

}
