package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.mocks.MockAuthenticationServiceBean;
import edu.harvard.iq.dataverse.mocks.MockBuiltinUserServiceBean;
import edu.harvard.iq.dataverse.mocks.MockPasswordValidatorServiceBean;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author michael
 */
public class BuiltinAuthenticationProviderTest {
    
    BuiltinAuthenticationProvider sut = null;
    PasswordValidatorServiceBean passwordValidatorService;
    MockBuiltinUserServiceBean bean = null;
    AuthenticationServiceBean authBean = null;
    
    @BeforeEach
    public void setup() {
        bean = new MockBuiltinUserServiceBean();
        passwordValidatorService = new MockPasswordValidatorServiceBean();
        authBean = new MockAuthenticationServiceBean();
        sut = new BuiltinAuthenticationProvider(bean, passwordValidatorService, authBean);
    }

    /**
     * Test of getId method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testGetId() {
        assertEquals("builtin", sut.getId());
    }

    /**
     * Test of getInfo method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testGetInfo() {
        String expResult = "builtin";
        String result = sut.getInfo().getId();
        assertEquals(expResult, result);
    }

    /**
     * Test of isPasswordUpdateAllowed method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testIsPasswordUpdateAllowed() {
        assertTrue( sut.isPasswordUpdateAllowed() );
    }

    /**
     * Test of isUserInfoUpdateAllowed method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testIsUserInfoUpdateAllowed() {
        assertTrue( sut.isUserInfoUpdateAllowed() );
    }

    /**
     * Test of isUserDeletionAllowed method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testIsUserDeletionAllowed() {
        assertTrue( sut.isUserDeletionAllowed() );
    }

    /**
     * Test of deleteUser method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testDeleteUser() {
        BuiltinUser u = makeBuiltInUser();
        assertTrue( bean.users.isEmpty() );
        bean.save(u);
        assertFalse( bean.users.isEmpty() );
        
        sut.deleteUser( u.getUserName() );
        
        assertTrue( bean.users.isEmpty() );
    }

    /**
     * Test of updatePassword method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testUpdatePassword() {
        BuiltinUser user = bean.save(makeBuiltInUser());
        final String newPassword = "newPassword";
        assertFalse( sut.verifyPassword(user.getUserName(), newPassword) );
        sut.updatePassword(user.getUserName(), newPassword);
        assertTrue( sut.verifyPassword(user.getUserName(), newPassword));
    }

    
    private BuiltinUser makeBuiltInUser() {
        BuiltinUser user = new BuiltinUser();
        user.setUserName("username");
        user.updateEncryptedPassword(PasswordEncryption.get().encrypt("password"), PasswordEncryption.getLatestVersionNumber());
        return user;
    }

    /**
     * Test of verifyPassword method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testVerifyPassword() {
        bean.save(makeBuiltInUser());
        assertEquals( Boolean.TRUE,  sut.verifyPassword("username", "password"));
        assertEquals( Boolean.FALSE, sut.verifyPassword("username", "xxxxxxxx"));
        assertEquals( null,          sut.verifyPassword("xxxxxxxx", "xxxxxxxx"));
    }

    /**
     * Test of authenticate method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testAuthenticate() {
        bean.save(makeBuiltInUser());
        String crdUsername = sut.getRequiredCredentials().get(0).getKey();
        String crdPassword = sut.getRequiredCredentials().get(1).getKey();
        AuthenticationRequest req = new AuthenticationRequest();
        req.putCredential(crdUsername, "username");
        req.putCredential(crdPassword, "password");
        AuthenticationResponse result = sut.authenticate(req);
        assertEquals(AuthenticationResponse.Status.SUCCESS, result.getStatus());
        
        req = new AuthenticationRequest();
        req.putCredential(crdUsername, "xxxxxxxx");
        req.putCredential(crdPassword, "password");
        result = sut.authenticate(req);
        assertEquals(AuthenticationResponse.Status.FAIL, result.getStatus());
        
        req = new AuthenticationRequest();
        req.putCredential(crdUsername, "username");
        req.putCredential(crdPassword, "xxxxxxxx");
        result = sut.authenticate(req);
        assertEquals(AuthenticationResponse.Status.FAIL, result.getStatus());
        
        BuiltinUser u2 = makeBuiltInUser();
        u2.setUserName("u2");
        u2.updateEncryptedPassword(PasswordEncryption.getVersion(0).encrypt("password"), 0);
        bean.save(u2);
        
        req = new AuthenticationRequest();
        req.putCredential(crdUsername, "u2");
        req.putCredential(crdPassword, "xxxxxxxx");
        result = sut.authenticate(req);
        assertEquals(AuthenticationResponse.Status.FAIL, result.getStatus());
        
        req = new AuthenticationRequest();
        req.putCredential(crdUsername, "u2");
        req.putCredential(crdPassword, "password");
        result = sut.authenticate(req);
        assertEquals(AuthenticationResponse.Status.BREAKOUT, result.getStatus());
    }
    
}
