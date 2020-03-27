package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.util.MailUtil;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
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

    @Inject
    private SettingsServiceBean settingService;

    @EJB
    private DataverseDao dataverseDao;


    // -------------------- LOGIC --------------------

    public String getNameOfInstallation() {
        String rootDataverseName = dataverseDao.findRootDataverse().getName();
        return BrandingUtil.getInstallationBrandName(rootDataverseName);
    }

    public String getSupportTeamName() {
        String systemEmail = settingService.getValueForKey(SettingsServiceBean.Key.SystemEmail);
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
        return BrandingUtil.getSupportTeamName(systemAddress, dataverseDao.findRootDataverse().getName());
    }
}
