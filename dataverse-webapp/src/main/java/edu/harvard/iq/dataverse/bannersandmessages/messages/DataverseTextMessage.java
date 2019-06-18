package edu.harvard.iq.dataverse.bannersandmessages.messages;

import edu.harvard.iq.dataverse.Dataverse;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author tjanek
 */
@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")})
public class DataverseTextMessage implements Serializable {

    private static final Logger logger = Logger.getLogger(DataverseTextMessage.class.getCanonicalName());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private UUID uuid = UUID.randomUUID();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dataverseTextMessage")
    private Set<DataverseLocalizedMessage> dataverseLocalizedMessages = new HashSet<>();

    private boolean active;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromTime;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date toTime;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    private Dataverse dataverse;

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public Set<DataverseLocalizedMessage> getDataverseLocalizedMessages() {
        return dataverseLocalizedMessages;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseLocalizedMessages(Set<DataverseLocalizedMessage> dataverseLocalizedMessages) {
        this.dataverseLocalizedMessages = dataverseLocalizedMessages;
    }

    public void addLocalizedMessage(String locale, String message) {
        DataverseLocalizedMessage localizedMessage = new DataverseLocalizedMessage();

        localizedMessage.setDataverseTextMessage(this);
        localizedMessage.setLocale(locale);
        localizedMessage.setMessage(message);

        this.dataverseLocalizedMessages.add(localizedMessage);
    }

    public void deactivate() {
        this.active = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataverseTextMessage that = (DataverseTextMessage) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
