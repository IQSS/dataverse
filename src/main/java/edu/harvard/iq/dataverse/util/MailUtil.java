package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import java.util.Arrays;
import java.util.ResourceBundle;
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

    public static String getSubjectTextBasedOnNotification(UserNotification userNotification, String rootDataverseName) {
        switch (userNotification.getType()) {
            case ASSIGNROLE:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.assign.role.subject");
            case REVOKEROLE:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.revoke.role.subject");
            case CREATEDV:
                return BundleUtil.getStringFromBundle("notification.email.create.dataverse.subject", Arrays.asList(BrandingUtil.getInstallationBrandName(rootDataverseName)));
            case REQUESTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.request.file.access.subject");
            case GRANTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.grant.file.access.subject");
            case REJECTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.rejected.file.access.subject");
            case MAPLAYERUPDATED:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.update.maplayer");
            case MAPLAYERDELETEFAILED:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.maplayer.deletefailed.subject");
            case CREATEDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.create.dataset.subject");
            case SUBMITTEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.submit.dataset.subject");
            case PUBLISHEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.publish.dataset.subject");
            case RETURNEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.returned.dataset.subject");
            case CREATEACC:
                return BundleUtil.getStringFromBundle("notification.email.create.account.subject", Arrays.asList(BrandingUtil.getInstallationBrandName(rootDataverseName)));
            case CHECKSUMFAIL:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.checksumfail.subject");
            case FILESYSTEMIMPORT:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.import.filesystem.subject");
            case CHECKSUMIMPORT:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.import.checksum.subject");
        }
        return "";
    }

}
