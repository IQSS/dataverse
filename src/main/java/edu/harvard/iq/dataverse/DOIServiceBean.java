package edu.harvard.iq.dataverse;

public abstract class DOIServiceBean extends AbstractGlobalIdServiceBean {

    public static final String DOI_PROTOCOL = "doi";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String HTTP_DOI_RESOLVER_URL = "http://doi.org/";
    public static final String DXDOI_RESOLVER_URL = "https://dx.doi.org/";
    public static final String HTTP_DXDOI_RESOLVER_URL = "http://dx.doi.org/";
    public DOIServiceBean() {
        super();
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

    public GlobalId parsePersistentId(String protocol, String identifierString) {
        if (!DOI_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = this.parsePersistentIdentifier(protocol, identifierString);
        if (!GlobalIdServiceBean.checkDOIAuthority(globalId.getAuthority())) {
            return null;
        }
        return globalId;
    }

}