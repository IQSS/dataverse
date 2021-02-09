package edu.harvard.iq.dataverse.persistence.group;


import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Entity
public class MailDomainGroup extends PersistedGlobalGroup implements JpaEntity<Long> {

    public final static String GROUP_TYPE = "mail";

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MailDomainItem> domainItems = new HashSet<>();

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainGroup() {
        super(GROUP_TYPE);
    }

    // -------------------- LOGIC --------------------

    @Override
    public boolean isEditable() {
        return false;
    }

    public Stream<MailDomainItem> getInclusionsStream() {
        return domainItems.stream()
                .filter(i -> i.getProcessingType() == MailDomainProcessingType.INCLUDE);
    }

    public Stream<MailDomainItem> getExclusionsStream() {
        return domainItems.stream()
                .filter(i -> i.getProcessingType() == MailDomainProcessingType.EXCLUDE);
    }

    // -------------------- GETTERS --------------------

    public Set<MailDomainItem> getDomainItems() {
        return domainItems;
    }

    // -------------------- SETTERS --------------------

    public void setDomainItems(Set<MailDomainItem> domainItems) {
        this.domainItems = domainItems;
    }
}
