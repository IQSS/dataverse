package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
    void testUpdateGroup() {
        // given
        List<MailDomainGroup> db = new ArrayList<>(Arrays.asList(MailDomainGroupTest.genGroup(), MailDomainGroupTest.genGroup(), MailDomainGroupTest.genRegexGroup()));
        mockQuery("MailDomainGroup.findAll", db);
        
        // when
        svc.updateGroups();
        
        // then
        assertEquals(2, svc.simpleGroups.size());
        assertEquals(1, svc.regexGroups.size());
    }
    
    @Test
    void testUpdateGroupMultiRegex() {
        // given
        List<MailDomainGroup> db = new ArrayList<>(Arrays.asList(MailDomainGroupTest.genGroup(), MailDomainGroupTest.genRegexGroup()));
        MailDomainGroup longRegex = MailDomainGroupTest.genRegexGroup();
        longRegex.setEmailDomains(Arrays.asList("example\\.org", ".+\\.example\\.org"));
        db.add(longRegex);
        mockQuery("MailDomainGroup.findAll", db);
        
        // when
        svc.updateGroups();
        
        // then
        assertEquals(1, svc.simpleGroups.size());
        assertEquals(2, svc.regexGroups.size());
        assertEquals("example\\.org|.+\\.example\\.org", svc.regexGroups.get(longRegex).pattern());
    }
    
    private static Stream<Arguments> mailTestExamples() {
        return Stream.of(
            Arguments.of("simpleGroupOnly@foobar.com", Arrays.asList("foobar.com"), false, false),
            Arguments.of("regexButNotActive@example.org", Arrays.asList("^example\\.org$"), false, true),
            Arguments.of("singleRegexMatch@example.org", Arrays.asList("^example\\.org$"), true, false),
            Arguments.of("singleRegexFail-1@hello.example.org", Arrays.asList("^example\\.org$"), true, true),
            Arguments.of("singleRegexFail-2@foobar.com", Arrays.asList("^example\\.org$"), true, true),
            Arguments.of("multiRegexMatch-1@example.org", Arrays.asList("^example\\.org$", "^.+\\.example\\.org$"), true, false),
            Arguments.of("multiRegexMatch-2@hello.example.org", Arrays.asList("^example\\.org$", "^.+\\.example\\.org$"), true, false),
            Arguments.of("multiRegexFail@hfoobar.com", Arrays.asList("^example\\.org$", "^.+\\.example\\.org$"), true, true),
            Arguments.of("invalidRegex@hfoobar.com", Arrays.asList("$foobar.com$"), true, true),
            Arguments.of("noOvermatchingRegex@hfoobar.com.eu", Arrays.asList("bar\\.com"), true, true)
        );
    }
    
    @ParameterizedTest
    @MethodSource("mailTestExamples")
    void testFindWithVerifiedEmail(String email, List<String> domains, boolean isRegex, boolean shouldBeEmpty) {
        // given
        List<MailDomainGroup> db = new ArrayList<>(Arrays.asList(MailDomainGroupTest.genGroup()));
        MailDomainGroup test = MailDomainGroupTest.genGroup();
        test.setEmailDomains(domains);
        test.setIsRegEx(isRegex);
        db.add(test);
        mockQuery("MailDomainGroup.findAll", db);
        svc.updateGroups();
        
        AuthenticatedUser u = new AuthenticatedUser();
        u.setEmail(email);
        when(confirmEmailSvc.hasVerifiedEmail(u)).thenReturn(true);
        
        // when
        Set<MailDomainGroup> result = svc.findAllWithDomain(u);
        
        // then
        if (shouldBeEmpty) {
            assertTrue(result.isEmpty());
        } else {
            Set<MailDomainGroup> expected = new HashSet<>(Arrays.asList(test));
            assertTrue(result.containsAll(expected));
        }
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