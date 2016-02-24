package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.UserLister;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.List;
import static edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider.Credential;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Set;

/**
 * An authentication provider built into the application. Uses JPA and the 
 * local database to store the users.
 * 
 * @author michael
 */
public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider, UserLister, GroupProvider {
    
    public static final String PROVIDER_ID = "builtin";
    private static String KEY_USERNAME_OR_EMAIL;
    private static String KEY_PASSWORD;
    private static List<Credential> CREDENTIALS_LIST;
      
    final BuiltinUserServiceBean bean;

    public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean ) {
        bean = aBean;
        KEY_USERNAME_OR_EMAIL = BundleUtil.getStringFromBundle("login.builtin.credential.usernameOrEmail");
        KEY_PASSWORD = BundleUtil.getStringFromBundle("login.builtin.credential.password");
        CREDENTIALS_LIST = Arrays.asList(new Credential(KEY_USERNAME_OR_EMAIL), new Credential(KEY_PASSWORD, true));
    }

    @Override
    public List<User> listUsers() {
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
        BuiltinUser u = bean.findByUsernameOrEmail(authReq.getCredential(KEY_USERNAME_OR_EMAIL) );
        if ( u == null ) return AuthenticationResponse.makeFail("Bad username, email address, or password");
        
        boolean userAuthenticated = PasswordEncryption.getVersion(u.getPasswordEncryptionVersion())
                                            .check(authReq.getCredential(KEY_PASSWORD), u.getEncryptedPassword() );
        if ( ! userAuthenticated ) {
            return AuthenticationResponse.makeFail("Bad username or password");
        }
        
        
        if ( u.getPasswordEncryptionVersion() < PasswordEncryption.getLatestVersionNumber() ) {
            try {
                String passwordResetUrl = bean.requestPasswordUpgradeLink(u);
                
                return AuthenticationResponse.makeBreakout(u.getUserName(), passwordResetUrl);
            } catch (PasswordResetException ex) {
                return AuthenticationResponse.makeError("Error while attempting to upgrade password", ex);
            }
        } else {
            return AuthenticationResponse.makeSuccess(u.getUserName(), u.getDisplayInfo());
        }
   }

    @Override
    public List<Credential> getRequiredCredentials() {
        return CREDENTIALS_LIST;
    }

    @Override
    public String getGroupProviderAlias() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getGroupProviderInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set groupsFor(RoleAssignee u, DvObject o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set groupsFor(DataverseRequest u, DvObject o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Group get(String groupAlias) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set findGlobalGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
