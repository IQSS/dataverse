package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import javax.mail.internet.InternetAddress;

public class BrandingUtil {

    public static String getInstallationBrandName(String rootDataverseName) {
        return rootDataverseName;
    }

    public static String getSupportTeamName(InternetAddress systemAddress, String rootDataverseName) {
        if (systemAddress != null) {
            String personalName = systemAddress.getPersonal();
            if (personalName != null) {
                return personalName;
            }
        }
        if (rootDataverseName != null && !rootDataverseName.isEmpty()) {
            return rootDataverseName + " " + BundleUtil.getStringFromBundle("contact.support");
        }
        String saneDefault = BundleUtil.getStringFromBundle("dataverse");
        return BundleUtil.getStringFromBundle("contact.support", Arrays.asList(saneDefault));
    }

    public static String getSupportTeamEmailAddress(InternetAddress systemAddress) {
        if (systemAddress == null) {
            return null;
        }
        return systemAddress.getAddress();
    }

    public static String getContactHeader(InternetAddress systemAddress, String rootDataverseName) {
        return BundleUtil.getStringFromBundle("contact.header", Arrays.asList(getSupportTeamName(systemAddress, rootDataverseName)));
    }

}
