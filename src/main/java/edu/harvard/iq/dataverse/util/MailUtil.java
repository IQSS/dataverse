package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import java.util.Arrays;
import java.util.List;
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

    public static String getSubjectTextBasedOnNotification(UserNotification userNotification, String rootDataverseName, Object objectOfNotification) {
        List<String> rootDvNameAsList = Arrays.asList(BrandingUtil.getInstallationBrandName(rootDataverseName));
        switch (userNotification.getType()) {
            case ASSIGNROLE:
                return BundleUtil.getStringFromBundle("notification.email.assign.role.subject", rootDvNameAsList);
            case REVOKEROLE:
                return BundleUtil.getStringFromBundle("notification.email.revoke.role.subject", rootDvNameAsList);
            case CREATEDV:
                return BundleUtil.getStringFromBundle("notification.email.create.dataverse.subject", rootDvNameAsList);
            case REQUESTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.request.file.access.subject", rootDvNameAsList);
            case GRANTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.grant.file.access.subject", rootDvNameAsList);
            case REJECTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.rejected.file.access.subject", rootDvNameAsList);
            case CREATEDS:
                return BundleUtil.getStringFromBundle("notification.email.create.dataset.subject", rootDvNameAsList);
            case SUBMITTEDDS:
                return BundleUtil.getStringFromBundle("notification.email.submit.dataset.subject", rootDvNameAsList);
            case PUBLISHEDDS:
                return BundleUtil.getStringFromBundle("notification.email.publish.dataset.subject", rootDvNameAsList);
            case PUBLISHFAILED_PIDREG:
                return BundleUtil.getStringFromBundle("notification.email.publishFailure.dataset.subject", rootDvNameAsList);
            case RETURNEDDS:
                return BundleUtil.getStringFromBundle("notification.email.returned.dataset.subject", rootDvNameAsList);
            case CREATEACC:
                return BundleUtil.getStringFromBundle("notification.email.create.account.subject", rootDvNameAsList);
            case CHECKSUMFAIL:
                return BundleUtil.getStringFromBundle("notification.email.checksumfail.subject", rootDvNameAsList);
            case FILESYSTEMIMPORT:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject", rootDvNameAsList);
                }

            case CHECKSUMIMPORT:
                return BundleUtil.getStringFromBundle("notification.email.import.checksum.subject", rootDvNameAsList);
            case CONFIRMEMAIL:
                return BundleUtil.getStringFromBundle("notification.email.verifyEmail.subject", rootDvNameAsList);
            case APIGENERATED:
                return BundleUtil.getStringFromBundle("notification.email.apiTokenGenerated.subject",  rootDvNameAsList);
            case INGESTCOMPLETED:
                return BundleUtil.getStringFromBundle("notification.email.ingestCompleted.subject", rootDvNameAsList);
            case INGESTCOMPLETEDWITHERRORS:
                return BundleUtil.getStringFromBundle("notification.email.ingestCompletedWithErrors.subject", rootDvNameAsList);
        }
        return "";
    }

}
