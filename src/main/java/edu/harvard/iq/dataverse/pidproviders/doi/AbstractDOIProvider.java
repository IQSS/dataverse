package edu.harvard.iq.dataverse.pidproviders.doi;

import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;


public abstract class AbstractDOIProvider extends AbstractPidProvider {

    public static final String DOI_PROTOCOL = "doi";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String HTTP_DOI_RESOLVER_URL = "http://doi.org/";
    public static final String DXDOI_RESOLVER_URL = "https://dx.doi.org/";
    public static final String HTTP_DXDOI_RESOLVER_URL = "http://dx.doi.org/";

    public AbstractDOIProvider(String id, String label, String providerAuthority, String providerShoulder, String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList) {
        super(id, label, DOI_PROTOCOL, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat, managedList, excludedList);
    }

    //For Unmanged provider
    public AbstractDOIProvider(String name, String label) {
        super(name, label, DOI_PROTOCOL);
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        if (pidString.startsWith(DOI_RESOLVER_URL)) {
            pidString = pidString.replace(DOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        } else if (pidString.startsWith(HTTP_DOI_RESOLVER_URL)) {
            pidString = pidString.replace(HTTP_DOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        } else if (pidString.startsWith(DXDOI_RESOLVER_URL)) {
            pidString = pidString.replace(DXDOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {

        if (!DOI_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        if (globalId!=null && !PidProvider.checkDOIAuthority(globalId.getAuthority())) {
            return null;
        }
        return globalId;
    }
    
    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {

        if (!DOI_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    public String getUrlPrefix() {
        return DOI_RESOLVER_URL;
    }

    protected String getProviderKeyName() {
        return null;
    }
    
    public String getProtocol() {
        return DOI_PROTOCOL;
    }
}