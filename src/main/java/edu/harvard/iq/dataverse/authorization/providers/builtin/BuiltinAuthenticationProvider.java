package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.UserLister;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.List;
import static edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider.Credential;

/**
 * An authentication provider built into the application. Uses JPA and the 
 * local database to store the users.
 * 
 * @author michael
 */
public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider, UserLister, GroupCreator {
    
    public static final String PROVIDER_ID = "builtin";
    private static final String KEY_USERNAME = "Username";
    private static final String KEY_PASSWORD = "Password";
    private static final List<Credential> CREDENTIALS_LIST = Arrays.asList( new Credential(KEY_USERNAME), new Credential(KEY_PASSWORD, true) );
      
    final BuiltinUserServiceBean bean;

    public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean ) {
        bean = aBean;
    }

    @Override
    public List<User> listUsers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Group createGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), "Build-in Provider", "Internal user repository");
    }

    @Override
    public AuthenticationResponse authenticate( AuthenticationRequest authReq ) {
        BuiltinUser u = bean.findByUserName( authReq.getCredential(KEY_USERNAME) );
        if ( u == null ) return AuthenticationResponse.makeFail("Bad username");
        if ( u.getEncryptedPassword().equals( bean.encryptPassword(authReq.getCredential(KEY_PASSWORD)))) {
            return AuthenticationResponse.makeSuccess(u.getUserName(), u.getDisplayInfo());
        } 
        return AuthenticationResponse.makeFail("Bad username or password");
   }

    @Override
    public List<Credential> getRequiredCredentials() {
        return CREDENTIALS_LIST;
    }
}
