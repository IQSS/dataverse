/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tested class: AuthenticatedUser.java
 *
 * @author bsilverstein
 */
public class AuthenticatedUserTest {

    public AuthenticatedUserTest() {
    }

    public static AuthenticatedUser testUser = MocksFactory.makeAuthenticatedUser("Homer", "Simpson");
    public static Timestamp expResult = testUser.getCreated();
    public static Timestamp loginTime = Timestamp.valueOf("2000-01-01 00:00:00.0");
    public static final String IDENTIFIER_PREFIX = "@";

    @Test
    public void testGetIdentifier() {
        System.out.println("getIdentifier for testUser");
        String result = testUser.getIdentifier();
        assertEquals(testUser.getIdentifier(), result);
    }

    @Test
    public void testGetDisplayInfo() {
        System.out.println("getDisplayInfo");
        AuthenticatedUserDisplayInfo expResult = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu", "UnitTester", "In-Memory user");
        AuthenticatedUserDisplayInfo result = testUser.getDisplayInfo();
        assertEquals(expResult, result);

    }

    @Test
    public void testApplyDisplayInfo() {
        System.out.println("applyDisplayInfo");
        AuthenticatedUserDisplayInfo inf = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu", "UnitTester", "In-Memory user");
        testUser.applyDisplayInfo(inf);
        assertEquals(inf, testUser.getDisplayInfo());
    }

    @Test
    public void testIsAuthenticated() {
        System.out.println("isAuthenticated");
        boolean expResult = true;
        boolean result = testUser.isAuthenticated();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetId() {
        System.out.println("getId");
        Long expResult = testUser.getId();
        assertEquals(expResult, testUser.getId());
    }

    @Test
    public void testSetId() {
        System.out.println("setId");
        Long id = 1776L;
        testUser.setId(id);
        assertEquals(id, testUser.getId());
    }

    @Test
    public void testGetUserIdentifier() {
        System.out.println("getUserIdentifier");
        String expResult = testUser.getUserIdentifier();
        assertEquals(expResult, testUser.getUserIdentifier());
    }

    @Test
    public void testSetUserIdentifier() {
        System.out.println("setUserIdentifier");
        String userIdentifier = "Davis";
        testUser.setUserIdentifier(userIdentifier);
        assertEquals(testUser.getUserIdentifier(), userIdentifier);
    }

    @Test
    public void testGetName() {
        System.out.println("getName");
        String expResult = "Homer Simpson";
        String result = testUser.getName();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetEmail() {
        System.out.println("getEmail");
        String expResult = testUser.getEmail();;
        assertEquals(expResult, testUser.getEmail());
    }

    @Test
    public void testSetEmail() {
        System.out.println("setEmail");
        String email = "HomerSimpson@someU.edu";
        testUser.setEmail(email);
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    public void testGetAffiliation() {
        System.out.println("getAffiliation");
        String expResult = "UnitTester";
        String result = testUser.getAffiliation();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetAffiliation() {
        System.out.println("setAffiliation");
        String affiliation = "FamilyMan";
        testUser.setAffiliation(affiliation);
        assertEquals(affiliation, testUser.getAffiliation());
    }

    @Test
    public void testGetPosition() {
        System.out.println("getPosition");
        String result = testUser.getPosition();
        assertEquals("In-Memory user", result);
    }

    @Test
    public void testSetPosition() {
        System.out.println("setPosition");
        String position = "";
        testUser.setPosition(position);
    }

    @Test
    public void testGetLastName() {
        System.out.println("getLastName");
        String expResult = "Simpson";
        String result = testUser.getLastName();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetLastName() {
        System.out.println("setLastName");
        String lastName = "";
        testUser.setLastName(lastName);
    }

    @Test
    public void testGetFirstName() {
        System.out.println("getFirstName");
        String result = testUser.getFirstName();
        assertEquals("Homer", result);
    }

    @Test
    public void testSetFirstName() {
        System.out.println("setFirstName");
        String firstName = "";
        testUser.setFirstName(firstName);
    }

    @Test
    public void testGetEmailConfirmed() {
        System.out.println("getEmailConfirmed");
        Timestamp expResult = null;
        Timestamp result = testUser.getEmailConfirmed();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetEmailConfirmed() {
        System.out.println("setEmailConfirmed");
        Timestamp emailConfirmed = null;
        testUser.setEmailConfirmed(emailConfirmed);
    }

    @Test
    public void testGetShibIdentityProvider() {
        System.out.println("getShibIdentityProvider");
        String expResult = testUser.getShibIdentityProvider();;
        assertEquals(expResult, testUser.getShibIdentityProvider());
    }

    @Test
    public void testSetShibIdentityProvider() {
        System.out.println("setShibIdentityProvider");
        String shibIdentityProvider = "Davis";
        testUser.setShibIdentityProvider(shibIdentityProvider);
        String result = testUser.getShibIdentityProvider();
        assertEquals("Davis", result);
    }

    @Test
    public void testToString() {
        System.out.println("toString");
        String expResult = "[AuthenticatedUser identifier:" + testUser.getIdentifier() + "]";
        String result = testUser.toString();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetSortByString() {
        System.out.println("getSortByString");
        String expResult = testUser.getLastName() + " " + testUser.getFirstName() + " " + testUser.getUserIdentifier();
        String result = testUser.getSortByString();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetLastLogin() {
        System.out.println("setLastLogin");
        testUser.setLastLogin(loginTime);
        Timestamp lastLogin = testUser.getLastLogin();
        assertEquals(loginTime, lastLogin);
    }

    @Test
    public void testGetLastLogin() {
        System.out.println("getLastLogin");
        Timestamp expResult = testUser.getLastLogin();
        assertEquals(expResult, testUser.getLastLogin());
    }

    @Test
    public void testSetLastLoginToCurrentTime() {
        System.out.println("setLastLoginToCurrentTime");
        testUser.setLastLoginToCurrentTime();
        Timestamp expResult = testUser.getLastLogin();
        assertEquals(expResult, testUser.getLastLogin());
    }

    @Test
    public void testSetCreatedToCurrentTime() {
        System.out.println("setCreatedToCurrentTime");
        testUser.setCreatedToCurrentTime();
        Timestamp expResult = testUser.getCreated();
        assertEquals(expResult, testUser.getCreated());
    }

    @Test
    public void testGetCreated() {
        System.out.println("getCreated");
        Timestamp result = testUser.getCreated();
        assertEquals(testUser.getCreated(), result);
    }

    @Test
    public void testSetCreated() {
        System.out.println("setCreated");
        Timestamp created = new Timestamp(new Date().getTime());
        testUser.setCreated(created);
        assertEquals(testUser.getCreated(), created);
    }

    @Test
    public void testGetLastApiUse() {
        System.out.println("getLastApiUse");
        Timestamp result = testUser.getLastApiUse();
        assertEquals(testUser.getLastApiUse(), result);
    }

    @Test
    public void testSetLastApiUse() {
        System.out.println("setLastApiUse");
        Timestamp lastApiUse = new Timestamp(new Date().getTime());
        testUser.setLastApiUse(lastApiUse);
        assertEquals(testUser.getLastApiUse(), lastApiUse);
    }

    @Test
    public void testSetLastApiUseToCurrentTime() {
        System.out.println("setLastApiUseToCurrentTime");
        testUser.setLastApiUseToCurrentTime();
        Timestamp expResult = testUser.getLastApiUse();
        assertEquals(expResult, testUser.getLastApiUse());
    }
    /**
     * All commented tests below have only been generated / are not complete for
     * AuthenticatedUser.java The tests above should all run fine, someone can
     * finish the remaining tests whenever they see fit.
     */

    @Test
    public void testIsSuperuser() {
        System.out.println("isSuperuser");
        boolean expResult = false;
        boolean result = testUser.isSuperuser();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetSuperuser() {
        System.out.println("setSuperuser");
        boolean superuser = true;
        testUser.setSuperuser(superuser);
        assertEquals(testUser.isSuperuser(), true);
    }

    @Test
    public void testSetModificationTime() {
        System.out.println("setModificationTime");
        Timestamp modificationTime = Timestamp.valueOf("2000-01-01 00:00:00.0");
        testUser.setModificationTime(modificationTime);
        assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), modificationTime);
    }

    @Test
    public void testGetAuthenticatedUserLookup() {
        System.out.println("getAuthenticatedUserLookup");
        AuthenticatedUserLookup result = testUser.getAuthenticatedUserLookup();
        assertEquals(testUser.getAuthenticatedUserLookup(), result);
    }

    @Test
    public void testSetAuthenticatedUserLookup() {
        System.out.println("setAuthenticatedUserLookup");
        AuthenticatedUserLookup authenticatedUserLookup = testUser.getAuthenticatedUserLookup();
        testUser.setAuthenticatedUserLookup(authenticatedUserLookup);
        assertEquals(authenticatedUserLookup, testUser.getAuthenticatedUserLookup());
    }

    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        AuthenticatedUser instance = new AuthenticatedUser();
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
    }
//    @Test
//    public void testEquals() {
//        System.out.println("equals");
//        Object object = (testUser instanceof AuthenticatedUser);
//        boolean expResult = true;
//        boolean result = testUser.equals(object);
//        assertEquals(expResult, result);
//    }
//    @Test
//    public void testGetDatasetLocks() {
//        System.out.println("getDatasetLocks");
//        List<DatasetLock> expResult = null;
//        List<DatasetLock> result = instance.getDatasetLocks();
//        assertEquals(expResult, result);
//    }
//
//    @Test
//    public void testSetDatasetLocks() {
//        System.out.println("setDatasetLocks");
//        List<DatasetLock> datasetLocks = null;
//        instance.setDatasetLocks(datasetLocks);
//    }
}
