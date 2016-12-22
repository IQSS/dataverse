package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import static edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers.instance;
import edu.harvard.iq.dataverse.mocks.MockBuiltinUserServiceBean;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author michael
 */
public class BuiltinAuthenticationProviderTest {
    
    BuiltinAuthenticationProvider sut = null;
    MockBuiltinUserServiceBean bean = null;
    
    @Before
    public void setup() {
        bean = new MockBuiltinUserServiceBean();
        sut = new BuiltinAuthenticationProvider(bean);
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

    /**
     * Test of updateUserInfo method, of class BuiltinAuthenticationProvider.
     */
    @Test
    public void testUpdateUserInfo() {
        BuiltinUser user = bean.save(makeBuiltInUser());
        AuthenticatedUserDisplayInfo newInfo = new AuthenticatedUserDisplayInfo("nf", "nl", "ema@il.com", "newAffi", "newPos");
        sut.updateUserInfo(user.getUserName(), newInfo);
        assertEquals( newInfo, user.getDisplayInfo() );
    }

    private BuiltinUser makeBuiltInUser() {
        BuiltinUser user = new BuiltinUser();
        user.setUserName("username");
        user.setFirstName("Firsty");
        user.setLastName("Last");
        user.setEmail("email@host.com");
        user.setAffiliation("an institute");
        user.setPosition("a position");
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
        String crdUsername = sut.getRequiredCredentials().get(0).getTitle();
        String crdPassword = sut.getRequiredCredentials().get(1).getTitle();
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
