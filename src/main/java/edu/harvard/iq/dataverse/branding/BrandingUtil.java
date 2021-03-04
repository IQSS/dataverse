package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.source.DbSettingConfigSource;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

public class BrandingUtil {

    static DataverseServiceBean dataverseService;
    
    public static String getInstallationBrandName(String rootDataverseName) {
        String installationName = new DbSettingConfigSource().getValue(DbSettingConfigSource.PREFIX + ".InstitutionName");
        return StringUtils.isEmpty(installationName) ? dataverseService.getRootDataverseName() : installationName;
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

    public static void injectDataverseService(DataverseServiceBean dataverseService) {
        BrandingUtil.dataverseService = dataverseService;
    }
}
