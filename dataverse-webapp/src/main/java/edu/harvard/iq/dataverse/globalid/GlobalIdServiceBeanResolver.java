package edu.harvard.iq.dataverse.globalid;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Stateless
public class GlobalIdServiceBeanResolver {

    private static final Logger log = LoggerFactory.getLogger(GlobalIdServiceBeanResolver.class);

    private final Map<String, GlobalIdServiceBean> serviceBeans = new HashMap<>();

    private HandlenetServiceBean handlenetService;

    private SettingsServiceBean settingsService;

    private DOIEZIdServiceBean doiEZId;

    private DOIDataCiteServiceBean doiDataCite;

    private FakePidProviderServiceBean fakePidProvider;

    @PostConstruct
    protected void setup() {
        serviceBeans.put("hdl", handlenetService);
        serviceBeans.put("doi", getDoiProvider());
    }

    /**
     * @deprecated for use by EJB proxy only.
     */
    public GlobalIdServiceBeanResolver() {
    }

    @Inject
    public GlobalIdServiceBeanResolver(SettingsServiceBean settingsService, HandlenetServiceBean handlenetService,
                                       DOIEZIdServiceBean doiEZId, DOIDataCiteServiceBean doiDataCite,
                                       FakePidProviderServiceBean fakePidProvider) {
        this.handlenetService = handlenetService;
        this.settingsService = settingsService;
        this.doiEZId = doiEZId;
        this.doiDataCite = doiDataCite;
        this.fakePidProvider = fakePidProvider;
    }

    public GlobalIdServiceBean resolve(String protocol) {
        final GlobalIdServiceBean globalIdServiceBean = serviceBeans.get(protocol);

        if (globalIdServiceBean == null) {
            log.error("Unknown protocol: {}", protocol);
            return null;
        }

        return globalIdServiceBean;
    }

    public GlobalIdServiceBean resolve() {
        return resolve(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol));
    }

    private GlobalIdServiceBean getDoiProvider() {
        String doiProvider = settingsService.getValueForKey(SettingsServiceBean.Key.DoiProvider);
        switch (doiProvider) {
            case "EZID":
                return doiEZId;
            case "DataCite":
                return doiDataCite;
            case "FAKE":
                return fakePidProvider;
            default:
                log.error("Unknown doiProvider: {}", doiProvider);
                return null;
        }
    }
}
