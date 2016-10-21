package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
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
        user.setFirstName("Firsty");
        user.setLastName("Last");
        user.setEmail("email@host.com");
        user.setAffiliation("an institute");
        user.setPosition("a position");
        user.updateEncryptedPassword("password", PasswordEncryption.getLatestVersionNumber());
        return user;
    }
    
}
