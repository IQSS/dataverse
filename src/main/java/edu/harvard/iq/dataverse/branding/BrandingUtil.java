package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.source.DbSettingConfigSource;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

@Singleton
@Startup
public class BrandingUtil {

    @EJB
    static DataverseServiceBean dataverseService;
    
    public static String getInstallationBrandName() {
        String installationName = new DbSettingConfigSource().getValue(DbSettingConfigSource.PREFIX + ".InstitutionName");
        return StringUtils.isEmpty(installationName) ? dataverseService.getRootDataverseName() : installationName;
    }
    
    //Convenience to access root name without injecting dataverseService (e.g. in DatasetVersion)
    public static String getRootDataverseCollectionName() {
        return dataverseService.getRootDataverseName();
    }

    public static String getSupportTeamName(InternetAddress systemAddress) {
        if (systemAddress != null) {
            String personalName = systemAddress.getPersonal();
            if (personalName != null) {
                return personalName;
            }
        }
        String rootDataverseName=dataverseService.getRootDataverseName();
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

    public static String getContactHeader(InternetAddress systemAddress) {
        return BundleUtil.getStringFromBundle("contact.header", Arrays.asList(getSupportTeamName(systemAddress)));
    }

    public static void injectDataverseService(DataverseServiceBean dataverseService) {
        BrandingUtil.dataverseService = dataverseService;
    }
}
