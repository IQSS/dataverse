package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.authorization.groups.impl.PersistedGlobalGroup;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotEmpty;
;

/**
 * A group that explicitly lists email address domains that a user might have to belong to this group.
 */
@NamedQueries({
    @NamedQuery(name="MailDomainGroup.findAll",
        query="SELECT g FROM MailDomainGroup g"),
    @NamedQuery(name="MailDomainGroup.findByPersistedGroupAlias",
        query="SELECT g FROM MailDomainGroup g WHERE g.persistedGroupAlias=:persistedGroupAlias"),
})
@Entity
public class MailDomainGroup extends PersistedGlobalGroup {
    
    /**
     * All the email address domains that make users belong
     * to this group, concatenated with ";" (thus avoiding another relation)
     */
    @NotEmpty
    private String emailDomains;
    
    private boolean isRegEx = false;
    
    @Transient
    private MailDomainGroupProvider provider;
    
    /**
     * Empty Constructor for JPA.
     */
    public MailDomainGroup() {}
    
    public void setEmailDomains(String domains) {
        this.emailDomains = domains;
    }
    public void setEmailDomains(List<String> domains) {
        this.emailDomains = String.join(";", domains);
    }
    
    public String getEmailDomains() {
        return this.emailDomains;
    }
    public List<String> getEmailDomainsAsList() {
        return Arrays.asList(this.emailDomains.split(";"));
    }
    
    public boolean isRegEx() {
        return isRegEx;
    }
    public void setIsRegEx(boolean isRegEx) {
        this.isRegEx = isRegEx;
    }
    
    @Override
    public MailDomainGroupProvider getGroupProvider() {
        return provider;
    }
    public void setGroupProvider(MailDomainGroupProvider pvd) {
        this.provider = pvd;
    }
    
    /**
     * This will throw an UnsupportOperationException if called.
     * Due to the necessity of checking if the mail adress is confirmed,
     * this cannot be decided in the entity class, but has to happen in the
     * provider.
     * @param aRequest The request whose inclusion we test
     */
    @Override
    public boolean contains(DataverseRequest aRequest) {
        throw new UnsupportedOperationException("This cannot be supported.");
    }
    
    /**
     * This will throw an UnsupportOperationException if called.
     * We will not allow edits of this via UI due to it's static nature. Can be changed via API.
     */
    @Override
    public boolean isEditable() {
        throw new UnsupportedOperationException("This is not supported.");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.getId());
        hash = 53 * hash + Objects.hashCode(this.emailDomains);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof MailDomainGroup)) {
            return false;
        }
        final MailDomainGroup other = (MailDomainGroup) obj;
        if ( this.getId() != null && other.getId() != null) {
            return Objects.equals(this.getId(), other.getId());
        } else {
            return Objects.equals(this.emailDomains, other.getEmailDomains());
        }
    }
    
    @Override
    public String toString() {
        return "[MailDomainGroup " + this.getPersistedGroupAlias() + ": id=" + this.getId() + " domains="+this.emailDomains+" ]";
    }
    
    public MailDomainGroup update(MailDomainGroup src) {
        setPersistedGroupAlias(src.getPersistedGroupAlias());
        setDisplayName(src.getDisplayName());
        setDescription(src.getDescription());
        setEmailDomains(src.getEmailDomains());
        setGroupProvider(src.getGroupProvider());
        return this;
    }
}
