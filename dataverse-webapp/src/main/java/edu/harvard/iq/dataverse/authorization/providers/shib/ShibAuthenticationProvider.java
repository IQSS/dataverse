package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.util.BundleUtil;

public class ShibAuthenticationProvider implements AuthenticationProvider {

    public static final String PROVIDER_ID = "shib";

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

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

}
