package edu.harvard.iq.dataverse.branding;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the BrandingUtil once it's ready.
     */
    @Singleton
    @Startup
    public class BrandingUtilHelper {

        @EJB
        DataverseServiceBean dataverseSvc;
        @EJB SettingsServiceBean settingsSvc;
        
        @PostConstruct
        public void injectService() {
            BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        }
    }