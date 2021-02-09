package edu.harvard.iq.dataverse.persistence.group;



import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Objects;

@Entity
public class MailDomainItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private MailDomainGroup owner;

    private String domain;

    @Enumerated(EnumType.STRING)
    private MailDomainProcessingType processingType;

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainItem() { }

    public MailDomainItem(String domain, MailDomainProcessingType processingType, MailDomainGroup owner) {
        this.owner = owner;
        this.domain = domain;
        this.processingType = processingType;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public MailDomainGroup getOwner() {
        return owner;
    }

    /**
     * Contains mail domain that will be used for matching.
     * Could be either full domain or part of it (but only
     * from place started with a dot up to the end – in that
     * case property value must start with a dot).
     */
    public String getDomain() {
        return domain;
    }

    public MailDomainProcessingType getProcessingType() {
        return processingType;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setOwner(MailDomainGroup owner) {
        this.owner = owner;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setProcessingType(MailDomainProcessingType processingType) {
        this.processingType = processingType;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailDomainItem that = (MailDomainItem) o;
        return Objects.equals(owner, that.owner) &&
                domain.equals(that.domain) &&
                processingType == that.processingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, domain, processingType);
    }
}
