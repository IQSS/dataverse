package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This is a small helper bean for the MPCONFIG DbSettingConfigSource.
 * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
 * settings service into the MPCONFIG POJO once it's ready.
 *
 * MPCONFIG requires it's sources to be POJOs. No direct dependency injection possible.
 */
@Singleton
@Startup
public class BrandingUtilHelper {
    @EJB
    DataverseServiceBean dataverseService;
    
    @PostConstruct
    public void injectService() {
        BrandingUtil.injectDataverseService(dataverseService);
    }
}
