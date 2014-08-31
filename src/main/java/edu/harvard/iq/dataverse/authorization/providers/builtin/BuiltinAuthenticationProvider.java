package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.UserLister;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An authentication provider built into the application. Uses JPA and the 
 * local database to store the users.
 * 
 * @author michael
 */
public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider, UserLister, GroupCreator {
    
    private static final String KEY_USERNAME = "Username";
    private static final String KEY_PASSWORD = "Password";
    private static final List<String> CREDENTIALS_LIST = Arrays.asList( KEY_USERNAME, KEY_PASSWORD );
      
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
        return "builtin";
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo("Build-in Provider", "Internal user repository");
    }

    @Override
    public String authenticate(Map<String, String> credentials) throws AuthorizationException {
        BuiltinUser u = bean.findByUserName( credentials.get(KEY_USERNAME) );
        if ( u == null ) return null;
        if ( u.getEncryptedPassword().equals( bean.encryptPassword(credentials.get(KEY_PASSWORD)))) {
            return u.getUserName();
        } 
        return null;
   }

    @Override
    public List<String> getRequiredCredentials() {
        return CREDENTIALS_LIST;
    }
}
