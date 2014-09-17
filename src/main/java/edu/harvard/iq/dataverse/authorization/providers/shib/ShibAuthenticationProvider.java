package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.ExternalLinkAuthenticationProvider;
import java.net.URL;

public class ShibAuthenticationProvider implements ExternalLinkAuthenticationProvider {

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AuthenticationResponse authenticate( AuthenticationRequest req ) {
        // TODO the credentials map will contain the shib* headers. Find the persistent id of the 
        // user there, and return it.
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public URL getAuthenticationUrl() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
