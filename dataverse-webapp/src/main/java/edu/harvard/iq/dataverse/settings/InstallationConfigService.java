package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.util.MailUtil;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.InternetAddress;
import java.io.Serializable;

/**
 * Service responsible for retrieving basic installation config
 * data like name of installation or name of support team
 *
 * @author madryk
 */
@Stateless
public class InstallationConfigService implements Serializable {

    @EJB
    private SettingsServiceBean settingService;

    @EJB
    private DataverseServiceBean dataverseService;


    // -------------------- LOGIC --------------------

    public String getNameOfInstallation() {
        String rootDataverseName = dataverseService.findRootDataverse().getName();
        return BrandingUtil.getInstallationBrandName(rootDataverseName);
    }

    public String getSupportTeamName() {
        String systemEmail = settingService.getValueForKey(SettingsServiceBean.Key.SystemEmail);
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
        return BrandingUtil.getSupportTeamName(systemAddress, dataverseService.findRootDataverse().getName());
    }
}
