package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;

import javax.mail.internet.InternetAddress;

public class BrandingUtil {

    private static DataverseServiceBean dataverseService;
    
    public static String getInstallationBrandName() {
        //ToDo #7387 which will make this call return something different than getRootDataverseCollectionName() 
        return dataverseService.getRootDataverseName();
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
