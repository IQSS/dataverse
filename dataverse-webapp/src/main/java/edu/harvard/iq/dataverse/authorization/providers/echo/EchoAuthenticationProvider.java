package edu.harvard.iq.dataverse.authorization.providers.echo;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;

import java.util.Arrays;
import java.util.List;

/**
 * A placeholder user provider, that authenticates everyone, using their credentials.
 *
 * @author michael
 * @deprecated This was a useful example of a non-builtin authentication
 * provider but now we have OAuth and Shib examples to follow. We could consider
 * deleting this class.
 */
@Deprecated
public class EchoAuthenticationProvider implements CredentialsAuthenticationProvider {

    private final String id;
    private final String prefix;
    private final String postfix;
    private final AuthenticationProviderDisplayInfo info;
    private final String KEY_NAME = "login.echo.credential.name";
    private final String KEY_EMAIL = "login.echo.credential.email";
    private final String KEY_AFFILIATION = "login.echo.credential.affiliation";


    public EchoAuthenticationProvider(String id, String prefix, String postfix, AuthenticationProviderDisplayInfo someInfo) {
        this.id = id;
        this.prefix = prefix;
        this.postfix = postfix;
        info = someInfo;
    }

    public EchoAuthenticationProvider(String id) {
        this(id, "", "",
             new AuthenticationProviderDisplayInfo(id, "Echo",
                                                   "Authenticate everyone using their credentials")
        );
    }

    @Override
    public List<Credential> getRequiredCredentials() {
        return Arrays.asList(new Credential(KEY_NAME),
                             new Credential(KEY_EMAIL),
                             new Credential(KEY_AFFILIATION));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return info;
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        AuthenticatedUserDisplayInfo disinf = new AuthenticatedUserDisplayInfo(
                prefix + " " + request.getCredential(KEY_NAME) + " " + postfix,
                prefix + " " + request.getCredential(KEY_NAME) + " " + postfix,
                request.getCredential(KEY_EMAIL),
                request.getCredential(KEY_AFFILIATION),
                null);
        return AuthenticationResponse.makeSuccess(disinf.getEmailAddress(), disinf);
    }

}
