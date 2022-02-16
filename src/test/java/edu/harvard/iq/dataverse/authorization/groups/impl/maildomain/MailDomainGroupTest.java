package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class MailDomainGroupTest {
    
    DataverseRequest dvr = Mockito.mock(DataverseRequest.class);
    
    @Test
    void testEmailDomains() {
        // given
        MailDomainGroup mdg = new MailDomainGroup();
        List<String> domains = Arrays.asList("test.de", "foo.com", "bar.com");
        String domainsStr = "test.de;foo.com;bar.com";
        
        // when
        mdg.setEmailDomains(domainsStr);
        // then
        assertEquals(domains, mdg.getEmailDomainsAsList());
        assertEquals(domainsStr, mdg.getEmailDomains());
        
        // when 2
        mdg.setEmailDomains(domains);
        // then 2
        assertEquals(domains, mdg.getEmailDomainsAsList());
        assertEquals(domainsStr, mdg.getEmailDomains());
    }
    
    @Test
    void testUnsupported() {
        // given
        MailDomainGroup a = new MailDomainGroup();
        // when, then
        assertThrows(UnsupportedOperationException.class, () -> { a.isEditable(); } );
        assertThrows(UnsupportedOperationException.class, () -> { a.contains(dvr); } );
    }
    
    @Test
    void testHashCode() {
        // given
        MailDomainGroup a = new MailDomainGroup();
        a.setEmailDomains("test.de;test2.de");
        MailDomainGroup b = new MailDomainGroup();
        a.setEmailDomains("test3.de;test4.de");
        MailDomainGroup c = new MailDomainGroup();
        c.setEmailDomains("test.de;test2.de");
        
        // when & then
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
    }
    
    @Test
    void testEquals() {
        // given
        MailDomainGroup a = new MailDomainGroup();
        a.setEmailDomains("test.de;test2.de");
        MailDomainGroup b = new MailDomainGroup();
        b.setEmailDomains("foo.de;bar.de");
        MailDomainGroup c = new MailDomainGroup();
        c.setEmailDomains("test.de;test2.de");
        
        // when & then
        assertEquals(a, c);
        assertNotEquals(a, b);
    }
    
    static Random rnd = new Random();
    public static MailDomainGroup genGroup() {
        MailDomainGroup t = new MailDomainGroup();
        t.setId(rnd.nextLong());
        t.setPersistedGroupAlias(RandomStringUtils.randomAlphanumeric(4));
        t.setDisplayName(RandomStringUtils.randomAlphanumeric(8));
        t.setDescription(RandomStringUtils.randomAlphanumeric(8));
        t.setEmailDomains(RandomStringUtils.randomAlphanumeric(5)+".com;"+RandomStringUtils.randomAlphanumeric(5)+".co.uk");
        return t;
    }
    
    public static MailDomainGroup genRegexGroup() {
        MailDomainGroup t = genGroup();
        t.setEmailDomains(".+\\.com$");
        t.setIsRegEx(true);
        return t;
    }
}