package edu.harvard.iq.dataverse.common;

import javax.mail.internet.InternetAddress;
import java.util.Arrays;
import java.util.Locale;

public class BrandingUtil {

    public static String getInstallationBrandName(String rootDataverseName) {
        return rootDataverseName;
    }

    public static String getSupportTeamName(InternetAddress systemAddress, String rootDataverseName) {
        return getSupportTeamName(systemAddress, rootDataverseName, BundleUtil.getCurrentLocale());
    }

    public static String getSupportTeamName(InternetAddress systemAddress, String rootDataverseName, Locale locale) {
        if (systemAddress != null) {
            String personalName = systemAddress.getPersonal();
            if (personalName != null) {
                return personalName;
            }
        }
        if (rootDataverseName != null && !rootDataverseName.isEmpty()) {
            return rootDataverseName + " " + BundleUtil.getStringFromBundleWithLocale("contact.support", locale);
        }
        String saneDefault = BundleUtil.getStringFromBundleWithLocale("dataverse", locale);
        return BundleUtil.getStringFromBundleWithLocale("contact.support", locale, saneDefault);
    }

    public static String getSupportTeamEmailAddress(InternetAddress systemAddress) {
        if (systemAddress == null) {
            return null;
        }
        return systemAddress.getAddress();
    }

    public static String getContactHeader(InternetAddress systemAddress, String rootDataverseName) {
        return BundleUtil.getStringFromBundle("contact.header", getSupportTeamName(systemAddress, rootDataverseName));
    }

}
