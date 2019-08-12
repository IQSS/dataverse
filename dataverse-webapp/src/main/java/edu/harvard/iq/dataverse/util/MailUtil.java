package edu.harvard.iq.dataverse.util;

import org.apache.commons.lang3.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;

public class MailUtil {

    private static final Logger logger = Logger.getLogger(MailUtil.class.getCanonicalName());

    public static InternetAddress parseSystemAddress(String systemEmail) {
        if (StringUtils.isNotEmpty(systemEmail)) {
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
