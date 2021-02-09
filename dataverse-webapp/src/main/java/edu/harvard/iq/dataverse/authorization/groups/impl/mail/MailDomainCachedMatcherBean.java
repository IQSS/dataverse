package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class MailDomainCachedMatcherBean {

    private Map<String, Set<MailDomainGroup>> matchIndex = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainCachedMatcherBean() { }

    // -------------------- LOGIC --------------------

    @Lock
    public Set<MailDomainGroup> matchGroupsForDomain(String domain) {
        Set<String> allDomainSubstrings = createAllDomainSubstrings(domain);

        // First look for allowed groups
        Set<MailDomainGroup> allowedGroups = allDomainSubstrings.stream()
                .map(s -> matchIndex.getOrDefault(s, Collections.emptySet()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // Then check if we should exclude user from one of found groups
        Set<MailDomainGroup> toDisallow = allowedGroups.stream()
                .flatMap(MailDomainGroup::getExclusionsStream)
                .filter(i -> allDomainSubstrings.contains(i.getDomain()))
                .map(MailDomainItem::getOwner)
                .collect(Collectors.toSet());

        allowedGroups.removeAll(toDisallow);
        return allowedGroups;
    }

    @Lock(LockType.WRITE)
    public void rebuildIndex(Collection<MailDomainGroup> groups) {
        matchIndex = groups.stream()
                .flatMap(MailDomainGroup::getInclusionsStream)
                .collect(Collectors.groupingBy(
                        MailDomainItem::getDomain,
                        Collectors.mapping(MailDomainItem::getOwner, Collectors.toSet())));
    }

    // -------------------- PRIVATE --------------------

    /**
     * Creates a set consisting of the given domain and subdomain strings
     * contained in it.
     * <br>
     * E.g. for icm.uw.edu.pl produces [icm.uw.edu.pl, .uw.edu.pl, .edu.pl, .pl]
     */
    private Set<String> createAllDomainSubstrings(String domain) {
        Set<String> domainSubstrings = new HashSet<>();
        domainSubstrings.add(domain);
        for (int i = domain.indexOf(".");
             i >= 0 && i < domain.length();
             i = domain.indexOf(".", i + 1)) {
            domainSubstrings.add(domain.substring(i));
        }
        return domainSubstrings;
    }
}
