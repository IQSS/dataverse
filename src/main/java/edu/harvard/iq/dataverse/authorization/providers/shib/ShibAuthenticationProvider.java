package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;

public class ShibAuthenticationProvider implements AuthenticationProvider {

    @Override
    public String getId() {
        return "shib";
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), "Shibboleth Provider", "Shibboleth user repository");
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest req) {
        /**
         * @todo Should we really implement this? It feels like unnecessary
         * overhead to pass AuthenticationRequest and AuthenticationResponse
         * back and forth when all the processing is done by the Shibboleth
         * Identity Providers.
         */
        throw new UnsupportedOperationException("Not supported yet. ");
    }

}
