package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailDomainGroupServiceBeanTest {
    
    @Mock
    ConfirmEmailServiceBean confirmEmailSvc;
    @Mock
    EntityManager em;
    
    MailDomainGroupServiceBean svc;
    
    @BeforeEach
    void setup() {
        svc = new MailDomainGroupServiceBean();
        svc.em = em;
        svc.confirmEmailSvc = confirmEmailSvc;
    }
    
    @Test
    void testFindWithVerifiedEmail() {
        // given
        List<MailDomainGroup> db = new ArrayList<>(Arrays.asList(MailDomainGroupTest.genGroup(),MailDomainGroupTest.genGroup(),MailDomainGroupTest.genGroup()));
        MailDomainGroup test = MailDomainGroupTest.genGroup();
        test.setEmailDomains("foobar.com");
        db.add(test);
        mockQuery("MailDomainGroup.findAll", db);
        
        AuthenticatedUser u = new AuthenticatedUser();
        u.setEmail("test@foobar.com");
        when(confirmEmailSvc.hasVerifiedEmail(u)).thenReturn(true);
    
        // when
        Set<MailDomainGroup> result = svc.findAllWithDomain(u);
        
        // then
        Set<MailDomainGroup> expected = new HashSet<>(Arrays.asList(test));
        assertEquals(expected, result);
    }
    
    @Test
    void testFindWithUnverifiedEmail() {
        // given
        AuthenticatedUser u = new AuthenticatedUser();
        u.setEmail("test@foobar.com");
        when(confirmEmailSvc.hasVerifiedEmail(u)).thenReturn(false);
        
        // when & then
        assertEquals(Collections.emptySet(), svc.findAllWithDomain(u));
    }
    
    // however this case might ever happen... its a branch in the function, we should test it.
    @Test
    void testFindWithInvalidEmail() {
        // given
        AuthenticatedUser u = new AuthenticatedUser();
        u.setEmail("testfoobar.com");
        when(confirmEmailSvc.hasVerifiedEmail(u)).thenReturn(true);
        
        // when & then
        assertEquals(Collections.emptySet(), svc.findAllWithDomain(u));
    }
    
    @Test
    void findAll() {
        // given
        List<MailDomainGroup> db = Arrays.asList(MailDomainGroupTest.genGroup(),MailDomainGroupTest.genGroup(),MailDomainGroupTest.genGroup());
        mockQuery("MailDomainGroup.findAll", db);
        
        // when & then
        assertEquals(db, svc.findAll());
    }
    
    @Test
    void findByAlias() {
        // given
        MailDomainGroup mg = MailDomainGroupTest.genGroup();
        mockQuery("MailDomainGroup.findByPersistedGroupAlias", mg, "persistedGroupAlias", mg.getPersistedGroupAlias());
        
        // when & then
        assertEquals(Optional.of(mg), svc.findByAlias(mg.getPersistedGroupAlias()));
    }
    
    private static Stream<Arguments> mailExamples() {
        return Stream.of(
            Arguments.of("foo@bar.com", Optional.of("bar.com")),
            Arguments.of("foo@foo@bar.com", Optional.of("bar.com")),
            Arguments.of("foobar.com", Optional.empty()));
    }
    
    @ParameterizedTest
    @MethodSource("mailExamples")
    void getDomainFromMail(String mail, Optional<String> expected) {
        assertEquals(expected, MailDomainGroupServiceBean.getDomainFromMail(mail));
    }
    
    void mockQuery(String name, List<MailDomainGroup> results) {
        TypedQuery mockedQuery = mock(TypedQuery.class);
        when(mockedQuery.getResultList()).thenReturn(results);
        when(this.svc.em.createNamedQuery(name, MailDomainGroup.class)).thenReturn(mockedQuery);
    }
    
    void mockQuery(String name, MailDomainGroup result, String parameter, String value) {
        TypedQuery mockedQuery = mock(TypedQuery.class);
        when(mockedQuery.getSingleResult()).thenReturn(result);
        when(mockedQuery.setParameter(parameter, value)).thenReturn(mockedQuery);
        when(this.svc.em.createNamedQuery(name, MailDomainGroup.class)).thenReturn(mockedQuery);
    }
}