package edu.harvard.iq.dataverse.util;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class MailUtil {

    private static final Logger logger = Logger.getLogger(MailUtil.class.getCanonicalName());

    public static InternetAddress parseSystemAddress(String systemEmail) {
        if (systemEmail != null) {
            try {
                InternetAddress parsedSystemEmail = new InternetAddress(systemEmail);
                logger.fine("parsed system email: " + parsedSystemEmail);
                return parsedSystemEmail;
            } catch (AddressException ex) {
                logger.info("Email will not be sent due to invalid value in " + SystemEmail + " setting: " + ex);
                return null;
            }
        }
        logger.fine("Email will not be sent because the " + SystemEmail + " setting is null.");
        return null;
    }
}
