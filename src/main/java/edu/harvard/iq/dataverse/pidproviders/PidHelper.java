package edu.harvard.iq.dataverse.pidproviders;

import java.util.Arrays;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import edu.harvard.iq.dataverse.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the BrandingUtil once it's ready.
     */
    @Startup
    @Singleton
    public class PidHelper {

        @EJB
        DOIDataCiteServiceBean datacitePidSvc;
        @EJB
        DOIEZIdServiceBean ezidPidSvc;
        @EJB
        HandlenetServiceBean handlePidSvc;
        @EJB
        FakePidProviderServiceBean fakePidSvc;
        @EJB
        PermaLinkPidProviderServiceBean permaPidSvc;
        @EJB
        UnmanagedDOIServiceBean unmanagedDOISvc;
        @EJB
        UnmanagedHandlenetServiceBean unmanagedHandleSvc;

        @PostConstruct
        public void listServices() {
            PidUtil.addAllToProviderList(Arrays.asList(datacitePidSvc, ezidPidSvc, handlePidSvc, permaPidSvc, fakePidSvc));
            PidUtil.addAllToUnmanagedProviderList(Arrays.asList(unmanagedDOISvc, unmanagedHandleSvc));
        }

    }