package edu.harvard.iq.dataverse.common;

import javax.mail.internet.InternetAddress;
import java.util.Locale;

public class BrandingUtil {

    public static String getInstallationBrandName(String rootDataverseName) {
        return rootDataverseName;
    }

    public static String getSupportTeamName(InternetAddress systemAddress, String rootDataverseName) {
        return getSupportTeamName(systemAddress, rootDataverseName, BundleUtil.getCurrentLocale());
    }

    public static String getSupportTeamName(InternetAddress systemAddress, String rootDataverseName, Locale locale) {
        if (systemAddress != null && systemAddress.getPersonal() != null) {
            return systemAddress.getPersonal();
        }
        if (rootDataverseName != null && !rootDataverseName.isEmpty()) {
            return rootDataverseName + " " + BundleUtil.getStringFromBundleWithLocale("contact.support", locale);
        }
        String saneDefault = BundleUtil.getStringFromBundleWithLocale("dataverse", locale);
        return BundleUtil.getStringFromBundleWithLocale("contact.support", locale, saneDefault);
    }

    public static String getSupportTeamEmailAddress(InternetAddress systemAddress) {
        return systemAddress == null ? null : systemAddress.getAddress();
    }

    public static String getContactHeader(InternetAddress systemAddress, String rootDataverseName) {
        return BundleUtil.getStringFromBundle("contact.header", getSupportTeamName(systemAddress, rootDataverseName));
    }
}