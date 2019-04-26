package edu.harvard.iq.dataverse.passwordreset;

import com.beust.jcommander.Parameter;
import com.sun.mail.iap.Argument;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Matchers;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasswordResetServiceBeanTest {

    private AuthenticationServiceBean mockedAuthenticationServiceBean;
    private BuiltinUserServiceBean mockedBuiltinUserServiceBean;
    private MailServiceBean mockedMailServiceBean;
    private PasswordResetServiceBean passwordResetServiceBean;
    private PasswordValidatorServiceBean mockedPasswordValidatorServiceBean;

    @BeforeEach
    void setup() {
        // mock the services to be injected into the bean
        mockedPasswordValidatorServiceBean = mock(PasswordValidatorServiceBean.class);
        mockedBuiltinUserServiceBean = mock(BuiltinUserServiceBean.class);
        mockedAuthenticationServiceBean = mock(AuthenticationServiceBean.class);
        mockedMailServiceBean = mock(MailServiceBean.class);

        // setup the service bean under test and inject services
        passwordResetServiceBean = new PasswordResetServiceBean();
        passwordResetServiceBean.authService = mockedAuthenticationServiceBean;
        passwordResetServiceBean.dataverseUserService = mockedBuiltinUserServiceBean;
        passwordResetServiceBean.mailService = mockedMailServiceBean;
        passwordResetServiceBean.passwordValidatorService = mockedPasswordValidatorServiceBean;
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
        when(mockedPasswordValidatorServiceBean.validate(Matchers.anyString())).thenReturn(Arrays.asList("error"));

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
        mockUserEntities();

        // mock the internal entity manager
        mockEntityManager(false);

        // execute the method under test
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(new BuiltinUser(), "newpass", "token");

        // ensure that an email would have been sent
        verify(mockedMailServiceBean, times(1)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        // assert that the password would have been changed
        assertTrue(passwordChangeAttemptResponse.isChanged());
    }

    @Test
    void testAttemptPasswordReset_successfulUserSaveWithoutToken() {
        // prepare a BuiltinUser and an AuthenticatedUser
        mockUserEntities();

        // mock the internal entity manager
        mockEntityManager(true);

        // execute the method under test
        PasswordChangeAttemptResponse passwordChangeAttemptResponse = passwordResetServiceBean.attemptPasswordReset(new BuiltinUser(), "newpass", null);

        // ensure that an email would have been sent
        verify(mockedMailServiceBean, times(1)).sendSystemEmail(ArgumentMatchers.eq("user1@domain.tld"), ArgumentMatchers.any(), ArgumentMatchers.any());

        // assert that the password would have been changed
        assertTrue(passwordChangeAttemptResponse.isChanged());
    }

    private void mockUserEntities() {
        BuiltinUser builtinUser= new BuiltinUser();
        builtinUser.setUserName("user1");
        when(mockedBuiltinUserServiceBean.save(ArgumentMatchers.any())).thenReturn(builtinUser);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("user");
        authenticatedUser.setLastName("1");
        authenticatedUser.setEmail("user1@domain.tld");
        when(mockedAuthenticationServiceBean.getAuthenticatedUser(ArgumentMatchers.any())).thenReturn(authenticatedUser);
    }

    private void mockEntityManager(boolean throwOnRemoval) {
        EntityManager mockedEntityManager = mock(EntityManager.class);
        TypedQuery mockedQuery = mock(TypedQuery.class);
        when(mockedEntityManager.createNamedQuery(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(mockedQuery);
        when(mockedQuery.getSingleResult()).thenReturn(new PasswordResetData());

        if (throwOnRemoval) {
            doThrow(new IllegalArgumentException()).when(mockedEntityManager).remove(ArgumentMatchers.any(PasswordResetData.class));
        }

        passwordResetServiceBean.em = mockedEntityManager;
    }
}
