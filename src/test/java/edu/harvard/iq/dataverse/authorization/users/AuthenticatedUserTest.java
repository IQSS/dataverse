/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tested class: AuthenticatedUser.java
 *
 * @author bsilverstein
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticatedUserTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;
    @InjectMocks
    private UserNotificationServiceBean userNotificationService;

    public AuthenticatedUserTest() {
    }

    public static AuthenticatedUser testUser;
    public static Timestamp expResult;
    public static Timestamp loginTime = Timestamp.valueOf("2000-01-01 00:00:00.0");
    public static final String IDENTIFIER_PREFIX = "@";
    public static final Set<Type> mutedTypes = EnumSet.of(Type.ASSIGNROLE, Type.REVOKEROLE);

    @BeforeEach
    public void setUp() {
        testUser = MocksFactory.makeAuthenticatedUser("Homer", "Simpson");
        expResult = testUser.getCreatedTime();
    }

    @Test
    public void testGetIdentifier() {
        System.out.println("getIdentifier for testUser");
        String result = testUser.getIdentifier();
        assertEquals(testUser.getIdentifier(), result);
    }

    @Test
    public void testApplyDisplayInfo() {
        System.out.println("applyDisplayInfo");
        AuthenticatedUserDisplayInfo inf = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu", "UnitTester", "In-Memory user");
        testUser.applyDisplayInfo(inf);
        assertEquals(inf, testUser.getDisplayInfo());
    }

    @Test
    public void testGetDisplayInfo() {
        System.out.println("getDisplayInfo");
        AuthenticatedUserDisplayInfo expResult = new AuthenticatedUserDisplayInfo("Homer", "Simpson", "Homer.Simpson@someU.edu", "UnitTester", "In-Memory user");
        AuthenticatedUserDisplayInfo result = testUser.getDisplayInfo();
        assertEquals(expResult, result);

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
        String expResult = testUser.getName();
        String result = testUser.getName();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetEmail() {
        System.out.println("getEmail");
        String expResult = testUser.getEmail();
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
    public void testSetLastLoginTime() {
        System.out.println("setLastLogin");
        testUser.setLastLoginTime(loginTime);
        Timestamp lastLogin = testUser.getLastLoginTime();
        assertEquals(loginTime, lastLogin);
    }

    @Test
    public void testGetLastLoginTime() {
        System.out.println("getLastLoginTime");
        Timestamp expResult = testUser.getLastLoginTime();
        assertEquals(expResult, testUser.getLastLoginTime());
    }

    @Test
    public void testGetCreatedTime() {
        System.out.println("getCreatedTime");
        Timestamp result = testUser.getCreatedTime();
        assertEquals(testUser.getCreatedTime(), result);
    }

    @Test
    public void testSetCreatedTime() {
        System.out.println("setCreatedTime");
        Timestamp createdTime = new Timestamp(new Date().getTime());
        testUser.setCreatedTime(createdTime);
        assertEquals(testUser.getCreatedTime(), createdTime);
    }

    @Test
    public void testGetLastApiUseTime() {
        System.out.println("getLastApiUseTime");
        Timestamp result = testUser.getLastApiUseTime();
        assertEquals(testUser.getLastApiUseTime(), result);
    }

    @Test
    public void testSetLastApiUseTime() {
        System.out.println("setLastApiUseTime");
        Timestamp lastApiUseTime = new Timestamp(new Date().getTime());
        testUser.setLastApiUseTime(lastApiUseTime);
        assertEquals(testUser.getLastApiUseTime(), lastApiUseTime);
    }

    @Test
    public void testSetLastApiUseToCurrentTime() {
        System.out.println("setLastApiUseToCurrentTime");
        testUser.setLastApiUseTime(new Timestamp(new Date().getTime()));
        Timestamp expResult = testUser.getLastApiUseTime();
        assertEquals(expResult, testUser.getLastApiUseTime());
    }

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

    @Test
    public void testMutingEmails() {
        System.out.println("setMutedEmails");
        testUser.setMutedEmails(mutedTypes);
        assertEquals(mutedTypes, testUser.getMutedEmails());
    }

    @Test
    public void testMutingNotifications() {
        System.out.println("setMutedNotifications");
        testUser.setMutedNotifications(mutedTypes);
        assertEquals(mutedTypes, testUser.getMutedNotifications());
    }

    @Test
    public void testMutingInJson() {
        testUser.setMutedEmails(mutedTypes);
        testUser.setMutedNotifications(mutedTypes);
        System.out.println("toJson");
        JsonObject jObject = testUser.toJson().build();

        Set<String> mutedEmails = new HashSet<>(jObject.getJsonArray("mutedEmails").getValuesAs(JsonString::getString));
        assertTrue(mutedEmails.size() == 2, "Set contains two elements");
        assertTrue(mutedEmails.contains("REVOKEROLE"), "Set contains REVOKEROLE");
        assertTrue(mutedEmails.contains("ASSIGNROLE"), "Set contains ASSIGNROLE");

        Set<String> mutedNotifications = new HashSet<>(jObject.getJsonArray("mutedNotifications").getValuesAs(JsonString::getString));
        assertTrue(mutedNotifications.size() == 2, "Set contains two elements");
        assertTrue(mutedNotifications.contains("REVOKEROLE"), "Set contains REVOKEROLE");
        assertTrue(mutedNotifications.contains("ASSIGNROLE"), "Set contains ASSIGNROLE");
    }

    @Test
    public void testHasEmailMuted() {
        testUser.setMutedEmails(mutedTypes);
        System.out.println("hasEmailMuted");
        assertEquals(true, testUser.hasEmailMuted(Type.ASSIGNROLE));
        assertEquals(true, testUser.hasEmailMuted(Type.REVOKEROLE));
        assertEquals(false, testUser.hasEmailMuted(Type.CREATEDV));
        assertEquals(false, testUser.hasEmailMuted(null));
    }

    @Test
    public void testHasNotificationsMutedMuted() {
        testUser.setMutedNotifications(mutedTypes);
        System.out.println("hasNotificationMuted");
        assertEquals(true, testUser.hasNotificationMuted(Type.ASSIGNROLE));
        assertEquals(true, testUser.hasNotificationMuted(Type.REVOKEROLE));
        assertEquals(false, testUser.hasNotificationMuted(Type.CREATEDV));
        assertEquals(false, testUser.hasNotificationMuted(null));
    }

    @Test
    public void testTypeTokenizer() {
        final Set<Type> typeSet = Type.tokenizeToSet(
            Type.toStringValue(
                Type.tokenizeToSet(" ASSIGNROLE , CREATEDV,REVOKEROLE  ")
            )
        );
        assertTrue(typeSet.size() == 3, "typeSet contains 3 elements");
        assertTrue(typeSet.contains(Type.ASSIGNROLE), "typeSet contains ASSIGNROLE");
        assertTrue(typeSet.contains(Type.CREATEDV), "typeSet contains CREATEDV");
        assertTrue(typeSet.contains(Type.REVOKEROLE), "typeSet contains REVOKEROLE");
    }

    @Test
    public void testIsEmailMuted() {
        testUser.setMutedEmails(mutedTypes);
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(testUser);
        userNotification.setSendDate(null);
        userNotification.setObjectId(null);
        userNotification.setRequestor(null);
        userNotification.setType(Type.ASSIGNROLE); // muted
        assertTrue(userNotificationService.isEmailMuted(userNotification));
        userNotification.setType(Type.APIGENERATED); // not muted
        assertFalse(userNotificationService.isEmailMuted(userNotification));
    }

    @Test
    public void isNotificationMuted() {
        testUser.setMutedNotifications(mutedTypes);
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(testUser);
        userNotification.setSendDate(null);
        userNotification.setObjectId(null);
        userNotification.setRequestor(null);
        userNotification.setType(Type.ASSIGNROLE); // muted
        assertTrue(userNotificationService.isNotificationMuted(userNotification));
        userNotification.setType(Type.APIGENERATED); // not muted
        assertFalse(userNotificationService.isNotificationMuted(userNotification));
    }

    /**
     * All commented tests below have only been generated / are not complete for
     * AuthenticatedUser.java The tests above should all run fine, due to time
     * constraints on this issue these 1+1=2 type tests weren't all done.
     */

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
