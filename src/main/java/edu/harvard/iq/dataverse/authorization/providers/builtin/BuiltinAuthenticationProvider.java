package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import java.util.Arrays;
import java.util.List;
import static edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider.Credential;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;

/**
 * An authentication provider built into the application. Uses JPA and the 
 * local database to store the users.
 * 
 * @author michael
 */
public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider {
    
    public static final String PROVIDER_ID = "builtin";
    /**
     * TODO: Think more about if it really makes sense to have the key for a
     * credential be a Bundle key. What if we want to reorganize our Bundle
     * files and rename some Bundle keys? Would login be broken until we update
     * the strings below?
     */
    public static final String KEY_USERNAME_OR_EMAIL = "login.builtin.credential.usernameOrEmail";
    public static final String KEY_PASSWORD = "login.builtin.credential.password";
    private static List<Credential> CREDENTIALS_LIST;
      
    final BuiltinUserServiceBean bean;
    final AuthenticationServiceBean authBean;
    private PasswordValidatorServiceBean passwordValidatorService;

    public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean, PasswordValidatorServiceBean passwordValidatorService, AuthenticationServiceBean auBean  ) {
        this.bean = aBean;
        this.authBean = auBean;
        this.passwordValidatorService = passwordValidatorService;
        CREDENTIALS_LIST = Arrays.asList(new Credential(KEY_USERNAME_OR_EMAIL), new Credential(KEY_PASSWORD, true));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.builtin"), "Internal user repository");
    }

    @Override
    public boolean isPasswordUpdateAllowed() {
        return true;
    }

    @Override
    public boolean isUserInfoUpdateAllowed() {
        return true;
    }

    @Override
    public boolean isUserDeletionAllowed() {
        return true;
    }
    
    @Override
    public void deleteUser(String userIdInProvider) {
        bean.removeUser(userIdInProvider);
    }
    
    @Override
    public void updatePassword(String userIdInProvider, String newPassword) {
        BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
        biUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(newPassword),
                                       PasswordEncryption.getLatestVersionNumber());
        bean.save(biUser);
    }
    
    /**
     * Validates that the passed password is indeed the password of the user.
     * @param userIdInProvider
     * @param password
     * @return {@code true} if the password matches the user's password; {@code false} otherwise.
     */
    @Override
    public Boolean verifyPassword( String userIdInProvider, String password ) {
        BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
        if ( biUser == null ) return null;
        return PasswordEncryption.getVersion(biUser.getPasswordEncryptionVersion())
                                 .check(password, biUser.getEncryptedPassword());
    }
    

    @Override
    public AuthenticationResponse authenticate( AuthenticationRequest authReq ) {
        BuiltinUser u = bean.findByUserName(authReq.getCredential(KEY_USERNAME_OR_EMAIL) );
        AuthenticatedUser authUser = null;
        
        if(u == null) { //If can't find by username in builtin, get the auth user and then the builtin
            authUser = authBean.getAuthenticatedUserByEmail(authReq.getCredential(KEY_USERNAME_OR_EMAIL));
            if (authUser == null) { //if can't find by email return bad username, etc.
                return AuthenticationResponse.makeFail("Bad username, email address, or password");
            }
            u = bean.findByUserName(authUser.getUserIdentifier());
        }
        
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
//        } else {
//            return AuthenticationResponse.makeSuccess(u.getUserName(), u.getDisplayInfo());
        }
        final List<String> errors = passwordValidatorService.validate(authReq.getCredential(KEY_PASSWORD));
        if (!errors.isEmpty()) {
            try {
                String passwordResetUrl = bean.requestPasswordComplianceLink(u);
                return AuthenticationResponse.makeBreakout(u.getUserName(), passwordResetUrl);
            } catch (PasswordResetException ex) {
                return AuthenticationResponse.makeError("Error while attempting to upgrade password", ex);
            }
        }
        if(null == authUser) {
            authUser = authBean.getAuthenticatedUser(u.getUserName());
        }
        
        return AuthenticationResponse.makeSuccess(u.getUserName(), authUser.getDisplayInfo());
   }

    @Override
    public List<Credential> getRequiredCredentials() {
        return CREDENTIALS_LIST;
    }

    @Override
    public boolean isOAuthProvider() {
        return false;
    }

}
