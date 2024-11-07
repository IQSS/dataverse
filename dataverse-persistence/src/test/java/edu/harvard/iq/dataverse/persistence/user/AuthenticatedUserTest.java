/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tested class: AuthenticatedUser.java
 *
 * @author bsilverstein
 */
public class AuthenticatedUserTest {

    public AuthenticatedUserTest() {
    }

    public static AuthenticatedUser testUser;
    public static Timestamp expResult;
    public static Timestamp loginTime = Timestamp.valueOf("2000-01-01 00:00:00.0");
    public static final String IDENTIFIER_PREFIX = "@";

    @BeforeEach
    public void setUp() {
        testUser = MocksFactory.makeAuthenticatedUser("Homer", "Simpson");
        expResult = testUser.getCreatedTime();
    }

    @Test
    public void testGetIdentifier() {
        
        String result = testUser.getIdentifier();
        assertEquals(testUser.getIdentifier(), result);
    }

    @Test
    public void testApplyDisplayInfo() {

        AuthenticatedUserDisplayInfo inf = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu",
                "0000-0001-2345-6789", "UnitTester", "https://ror.org/04k0tth05", "In-Memory user");
        testUser.applyDisplayInfo(inf);
        assertEquals(inf, testUser.getDisplayInfo());
    }

    @Test
    public void testGetDisplayInfo() {

        // given
        testUser.setOrcid("0000-0001-2345-6789");
        testUser.setAffiliationROR("https://ror.org/04k0tth05");
        AuthenticatedUserDisplayInfo expResult = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu",
                "0000-0001-2345-6789", "UnitTester", "https://ror.org/04k0tth05", "In-Memory user");
        // when
        AuthenticatedUserDisplayInfo result = testUser.getDisplayInfo();

        // then
        assertEquals(expResult, result);

    }

    @Test
    public void testIsAuthenticated() {

        boolean expResult = true;
        boolean result = testUser.isAuthenticated();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetId() {

        Long expResult = testUser.getId();
        assertEquals(expResult, testUser.getId());
    }

    @Test
    public void testSetId() {

        Long id = 1776L;
        testUser.setId(id);
        assertEquals(id, testUser.getId());
    }

    @Test
    public void testGetUserIdentifier() {

        String expResult = testUser.getUserIdentifier();
        assertEquals(expResult, testUser.getUserIdentifier());
    }

    @Test
    public void testSetUserIdentifier() {

        String userIdentifier = "Davis";
        testUser.setUserIdentifier(userIdentifier);
        assertEquals(testUser.getUserIdentifier(), userIdentifier);
    }

    @Test
    public void testGetName() {

        String expResult = testUser.getName();
        String result = testUser.getName();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetEmail() {

        String expResult = testUser.getEmail();
        assertEquals(expResult, testUser.getEmail());
    }

    @Test
    public void testSetEmail() {
 
        String email = "HomerSimpson@someU.edu";
        testUser.setEmail(email);
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    public void testGetAffiliation() {

        String expResult = "UnitTester";
        String result = testUser.getAffiliation();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetAffiliation() {

        String affiliation = "FamilyMan";
        testUser.setAffiliation(affiliation);
        assertEquals(affiliation, testUser.getAffiliation());
    }

    @Test
    public void testGetPosition() {

        String result = testUser.getPosition();
        assertEquals("In-Memory user", result);
    }

    @Test
    public void testSetPosition() {

        String position = "";
        testUser.setPosition(position);
    }

    @Test
    public void testGetLastName() {

        String expResult = "Simpson";
        String result = testUser.getLastName();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetLastName() {

        String lastName = "";
        testUser.setLastName(lastName);
    }

    @Test
    public void testGetFirstName() {
 
        String result = testUser.getFirstName();
        assertEquals("Homer", result);
    }

    @Test
    public void testSetFirstName() {

        String firstName = "";
        testUser.setFirstName(firstName);
    }

    @Test
    public void testGetEmailConfirmed() {

        Timestamp expResult = null;
        Timestamp result = testUser.getEmailConfirmed();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetEmailConfirmed() {
  
        Timestamp emailConfirmed = null;
        testUser.setEmailConfirmed(emailConfirmed);
    }

    @Test
    public void testGetShibIdentityProvider() {

        String expResult = testUser.getShibIdentityProvider();
        assertEquals(expResult, testUser.getShibIdentityProvider());
    }

    @Test
    public void testSetShibIdentityProvider() {

        String shibIdentityProvider = "Davis";
        testUser.setShibIdentityProvider(shibIdentityProvider);
        String result = testUser.getShibIdentityProvider();
        assertEquals("Davis", result);
    }

    @Test
    public void testToString() {

        String expResult = "[AuthenticatedUser identifier:" + testUser.getIdentifier() + "]";
        String result = testUser.toString();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetSortByString() {

        String expResult = testUser.getLastName() + " " + testUser.getFirstName() + " " + testUser.getUserIdentifier();
        String result = testUser.getSortByString();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetLastLoginTime() {

        testUser.setLastLoginTime(loginTime);
        Timestamp lastLogin = testUser.getLastLoginTime();
        assertEquals(loginTime, lastLogin);
    }

    @Test
    public void testGetLastLoginTime() {

        Timestamp expResult = testUser.getLastLoginTime();
        assertEquals(expResult, testUser.getLastLoginTime());
    }

    @Test
    public void testGetCreatedTime() {

        Timestamp result = testUser.getCreatedTime();
        assertEquals(testUser.getCreatedTime(), result);
    }

    @Test
    public void testSetCreatedTime() {

        Timestamp createdTime = new Timestamp(new Date().getTime());
        testUser.setCreatedTime(createdTime);
        assertEquals(testUser.getCreatedTime(), createdTime);
    }

    @Test
    public void testGetLastApiUseTime() {

        Timestamp result = testUser.getLastApiUseTime();
        assertEquals(testUser.getLastApiUseTime(), result);
    }

    @Test
    public void testSetLastApiUseTime() {

        Timestamp lastApiUseTime = new Timestamp(new Date().getTime());
        testUser.setLastApiUseTime(lastApiUseTime);
        assertEquals(testUser.getLastApiUseTime(), lastApiUseTime);
    }

    @Test
    public void testSetLastApiUseToCurrentTime() {

        testUser.setLastApiUseTime(new Timestamp(new Date().getTime()));
        Timestamp expResult = testUser.getLastApiUseTime();
        assertEquals(expResult, testUser.getLastApiUseTime());
    }

    @Test
    public void testIsSuperuser() {

        boolean expResult = false;
        boolean result = testUser.isSuperuser();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetSuperuser() {
 
        boolean superuser = true;
        testUser.setSuperuser(superuser);
        assertEquals(testUser.isSuperuser(), true);
    }

    @Test
    public void testGetAuthenticatedUserLookup() {
 
        AuthenticatedUserLookup result = testUser.getAuthenticatedUserLookup();
        assertEquals(testUser.getAuthenticatedUserLookup(), result);
    }

    @Test
    public void testSetAuthenticatedUserLookup() {

        AuthenticatedUserLookup authenticatedUserLookup = testUser.getAuthenticatedUserLookup();
        testUser.setAuthenticatedUserLookup(authenticatedUserLookup);
        assertEquals(authenticatedUserLookup, testUser.getAuthenticatedUserLookup());
    }

    @Test
    public void testHashCode() {
 
        AuthenticatedUser instance = new AuthenticatedUser();
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
    }
}
