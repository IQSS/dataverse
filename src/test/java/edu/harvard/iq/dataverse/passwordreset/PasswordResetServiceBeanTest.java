package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasswordResetServiceBeanTest {

    private AuthenticationServiceBean mockedAuthenticationServiceBean;
    private BuiltinUserServiceBean mockedBuiltinUserServiceBean;
    private EntityManager mockedEntityManager;
    private MailServiceBean mockedMailServiceBean;
    private PasswordResetServiceBean passwordResetServiceBean;
    private PasswordValidatorServiceBean mockedPasswordValidatorServiceBean;
    private SystemConfig mockedSystemConfig;

    @BeforeEach
    void setup() {
        // mock the services to be injected into the bean
        mockedPasswordValidatorServiceBean = mock(PasswordValidatorServiceBean.class);
        mockedBuiltinUserServiceBean = mock(BuiltinUserServiceBean.class);
        mockedAuthenticationServiceBean = mock(AuthenticationServiceBean.class);
        mockedMailServiceBean = mock(MailServiceBean.class);
        mockedEntityManager = mock(EntityManager.class);
        mockedSystemConfig = mock(SystemConfig.class);

        // setup the service bean under test and inject services
        passwordResetServiceBean = new PasswordResetServiceBean();
        passwordResetServiceBean.authService = mockedAuthenticationServiceBean;
        passwordResetServiceBean.dataverseUserService = mockedBuiltinUserServiceBean;
        passwordResetServiceBean.mailService = mockedMailServiceBean;
        passwordResetServiceBean.passwordValidatorService = mockedPasswordValidatorServiceBean;
        passwordResetServiceBean.em = mockedEntityManager;
        passwordResetServiceBean.systemConfig = mockedSystemConfig;
    }

    @Test
    void testRequestReset_forExistingUser() throws PasswordResetException {
        prepareBuiltinUser();
        prepareAuthenticatedUser();

        mockTypedQuery("PasswordResetData.findAll", Arrays.asList(new PasswordResetData()));
        mockTypedQuery("PasswordResetData.findByUser", Arrays.asList(new PasswordResetData()));

        PasswordResetInitResponse result = passwordResetServiceBean.requestReset("user1@domain.tld");

        // ensure that an email would have been sent
        verify(mockedMailServiceBean, times(1)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        assertTrue(result.isEmailFound());
    }

    @Test
    void testRequestReset_forInexistentUser() throws PasswordResetException {
        prepareAuthenticatedUser();

        mockTypedQuery("PasswordResetData.findAll", Arrays.asList());
        mockTypedQuery("PasswordResetData.findByUser", Arrays.asList());

        PasswordResetInitResponse result = passwordResetServiceBean.requestReset("user1@domain.tld");

        // ensure that an email would not have been sent
        verify(mockedMailServiceBean, times(0)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        assertFalse(result.isEmailFound());
    }

    @Test
    void testAttemptPasswordReset_withNullUser() {
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(null, "newpass", "token");

        assertFalse(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_withNullNewPassword() {
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(new BuiltinUser(), null, "token");

        assertFalse(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_withValidationErrors() {
        when(mockedPasswordValidatorServiceBean.validate(ArgumentMatchers.anyString())).thenReturn(Arrays.asList("error"));

        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(new BuiltinUser(), "newpass", "token");

        assertFalse(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_failedUserSave() {
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(new BuiltinUser(), "newpass", "token");

        assertFalse(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_successfulUserSave() {
        // prepare a BuiltinUser and an AuthenticatedUser
        BuiltinUser builtinUser = prepareBuiltinUser();
        AuthenticatedUser authenticatedUser = prepareAuthenticatedUser();

        // mock the internal entity manager
        mockTypedQuery("PasswordResetData.findByToken", new PasswordResetData());

        // execute the method under test
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(builtinUser, "newpass", "token");

        // ensure that an email would have been sent
        verify(mockedMailServiceBean, times(1)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        // assert that the password would have been changed
        assertTrue(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_successfulUserSaveWithoutToken() {
        // prepare a BuiltinUser and an AuthenticatedUser
        BuiltinUser builtinUser = prepareBuiltinUser();
        AuthenticatedUser authenticatedUser = prepareAuthenticatedUser();

        // mock the internal entity manager
        mockTypedQuery("PasswordResetData.findByToken", new PasswordResetData());
        doThrow(new IllegalArgumentException()).when(mockedEntityManager).remove(ArgumentMatchers.any(PasswordResetData.class));

        // execute the method under test
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(builtinUser, "newpass", null);

        // ensure that an email would have been sent
        verify(mockedMailServiceBean, times(1)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        // assert that the password would have been changed
        assertTrue(passwordChangeAttemptResponse.isChanged());
    }

    private BuiltinUser prepareBuiltinUser() {
        BuiltinUser builtinUser= new BuiltinUser();
        builtinUser.setId(1L);
        builtinUser.setUserName("user1");
        when(mockedBuiltinUserServiceBean.findByUserName(ArgumentMatchers.anyString())).thenReturn(builtinUser);
        when(mockedBuiltinUserServiceBean.save(ArgumentMatchers.any())).thenReturn(builtinUser);
        return builtinUser;
    }

    private AuthenticatedUser prepareAuthenticatedUser() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserIdentifier("x83js");
        authenticatedUser.setFirstName("user");
        authenticatedUser.setLastName("1");
        authenticatedUser.setEmail("user1@domain.tld");
        when(mockedAuthenticationServiceBean.getAuthenticatedUser("user1")).thenReturn(authenticatedUser);
        when(mockedAuthenticationServiceBean.getAuthenticatedUserByEmail("user1@domain.tld")).thenReturn(authenticatedUser);
        return authenticatedUser;
    }

    private void mockTypedQuery(String queryName, List queryResult) {
        TypedQuery mockedQuery = mock(TypedQuery.class);
        when(mockedQuery.getResultList()).thenReturn(queryResult);
        when(mockedEntityManager.createNamedQuery(ArgumentMatchers.eq(queryName), ArgumentMatchers.any())).thenReturn(mockedQuery);
    }

    private void mockTypedQuery(String queryName, Object queryResult) {
        TypedQuery mockedQuery = mock(TypedQuery.class);
        when(mockedQuery.getSingleResult()).thenReturn(queryResult);
        when(mockedEntityManager.createNamedQuery(ArgumentMatchers.eq(queryName), ArgumentMatchers.any())).thenReturn(mockedQuery);
    }
}
