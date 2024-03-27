package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailDomainGroupProviderTest {
    
    MailDomainGroupServiceBean svc = Mockito.mock(MailDomainGroupServiceBean.class);
    
    @Test
    void testUserMatched() {
        // given
        MailDomainGroupProvider pvd = new MailDomainGroupProvider(svc);
        
        MailDomainGroup t = new MailDomainGroup();
        t.setEmailDomains("foobar.com");
        Set<MailDomainGroup> set = new HashSet<>();
        set.add(t);
        
        AuthenticatedUser u = new AuthenticatedUser();
        when(svc.findAllWithDomain(u)).thenReturn(set);
        
        DataverseRequest req = new DataverseRequest(u, new IPv4Address(192,168,0,1));
        
        // when
        Set<MailDomainGroup> result = pvd.groupsFor(req);
        
        // then
        assertTrue(result.contains(t));
        assertEquals(pvd, t.getGroupProvider());
    }
    
    @Test
    void testUserNotPresent() {
        // given
        MailDomainGroupProvider pvd = new MailDomainGroupProvider(svc);
        AuthenticatedUser u = null;
        DataverseRequest req = new DataverseRequest(u, new IPv4Address(192,168,0,1));
        
        // when & then
        assertEquals(Collections.emptySet(), pvd.groupsFor(req));
    }
    
    @Test
    void testContextIgnored() {
        // given
        MailDomainGroupProvider pvd = Mockito.spy(new MailDomainGroupProvider(svc));
        AuthenticatedUser u = null;
        DataverseRequest req = new DataverseRequest(u, new IPv4Address(192,168,0,1));
        Dataset a = new Dataset();
        
        // when
        pvd.groupsFor(req, a);
        // then
        verify(pvd, times(1)).groupsFor(req);
    }
    
    @Test
    void testUnsupportedAsEmpty() {
        // given
        MailDomainGroupProvider pvd = new MailDomainGroupProvider(svc);
        
        // when & then
        assertEquals(Collections.emptySet(), pvd.groupsFor(new AuthenticatedUser(), new Dataset()));
        assertEquals(Collections.emptySet(), pvd.groupsFor(new AuthenticatedUser()));
    }
    
    @Test
    void testUpdateProvider() {
        // given
        MailDomainGroupProvider pvd = new MailDomainGroupProvider(svc);
        // include a null value to ensure null safety of the functions
        List<MailDomainGroup> test = Arrays.asList(new MailDomainGroup(), new MailDomainGroup(), null);
        for (MailDomainGroup mdg : test) {
            if ( mdg != null ) {
                assertNull(mdg.getGroupProvider());
            }
        }
        
        // when
        pvd.updateProvider(test);
        
        // then
        for (MailDomainGroup mdg : test) {
            if ( mdg != null ) {
                assertEquals(pvd, mdg.getGroupProvider());
            }
        }
        
    }
}