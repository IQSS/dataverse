package edu.harvard.iq.dataverse.authorization.providers.echo;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder user provider, that authenticates everyone, using their credentials.
 * 
 * @author michael
 */
public class EchoAuthenticationProvider implements CredentialsAuthenticationProvider {
    
    private final String id;

    public EchoAuthenticationProvider(String id) {
        this.id = id;
    }
    
    @Override
    public List<Credential> getRequiredCredentials() {
        return Arrays.asList( new Credential("Name"),
                              new Credential("Email"),
                              new Credential("Affiliation") );
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), "Echo",
                "Authenticate everyone using their credentials");
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        RoleAssigneeDisplayInfo disinf = new RoleAssigneeDisplayInfo(request.getCredential("Name"),
                request.getCredential("Email"),
                request.getCredential("Affiliation"));
        return AuthenticationResponse.makeSuccess(disinf.getEmailAddress(), disinf);
    }
    
}
