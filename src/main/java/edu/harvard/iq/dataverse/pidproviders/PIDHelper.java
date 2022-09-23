package edu.harvard.iq.dataverse.pidproviders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import javax.annotation.PostConstruct;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean;
import edu.harvard.iq.dataverse.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import edu.harvard.iq.dataverse.branding.BrandingUtil;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the BrandingUtil once it's ready.
     */
@Stateless
    public class PIDHelper {

        @Inject
        DOIDataCiteServiceBean datacitePidSvc;
        @Inject
        DOIEZIdServiceBean ezidPidSvc;
        @Inject
        HandlenetServiceBean handlePidSvc;
        @Inject
        FakePidProviderServiceBean fakePidSvc;
        @Inject
        PermaLinkPidProviderServiceBean permaPidSvc;
        
        
        List<GlobalIdServiceBean> providerList;
        
        @PostConstruct
        public void listServices() {
        providerList = new ArrayList<GlobalIdServiceBean>(Arrays.asList(datacitePidSvc, ezidPidSvc, handlePidSvc, permaPidSvc, fakePidSvc));
        }
        
        /**
         * 
         * @param identifier The string to be parsed
         * @throws IllegalArgumentException if the passed string cannot be parsed.
         */
        public GlobalId parseAsGlobalID(String identifier) {
            for(GlobalIdServiceBean pidProvider: providerList) {
                GlobalId globalId = pidProvider.parsePersistentId(identifier);
                if(globalId!=null) {
                    return globalId;
                }
            }
            throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
        }
            // set the protocol, authority, and identifier via parsePersistentId
    /*        for(String beanName: BeanDispatcher.DISPATCHER.keySet()) {
                Global
            }
            */
    }