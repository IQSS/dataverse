package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.util.BundleUtil;

public class ShibAuthenticationProvider implements AuthenticationProvider {

    public static final String PROVIDER_ID = "shib";
    
    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.shib"), "Shibboleth user repository");
    }

    @Override
    public boolean isOAuthProvider() {
        return false;
    }

    // We don't override "isEmailVerified" because we're using timestamps
    // ("emailconfirmed" on the "authenticateduser" table) to know if
    // Shib users have confirmed/verified their email or not.
    
    /* 
     * Specific to Shibboleth auth, the provider needs to know the entityId of 
     * itself as a Shibboleth "service provider" - the registered identifier
     * of shibd running on the same host. 
     */
    private String serviceProviderEntityId = null; 
    
    public String getServiceProviderEntityId() {
        return serviceProviderEntityId; 
    }
    
    public void setServiceProviderEntityId(String serviceProviderEntityId) {
        this.serviceProviderEntityId = serviceProviderEntityId; 
    }

}
