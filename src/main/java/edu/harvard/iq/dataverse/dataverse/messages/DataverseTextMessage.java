package edu.harvard.iq.dataverse.dataverse.messages;

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
import javax.persistence.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tjanek
 */
@Entity
@Table(indexes = {@Index(columnList="dataverse_id")})
public class DataverseTextMessage implements Serializable {

    private static final Logger logger = Logger.getLogger(DataverseTextMessage.class.getCanonicalName());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dataverseTextMessage")
    private Set<DataverseLocalizedMessage> dataverseLocalizedMessages = new HashSet<>();

    private boolean active;

    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime fromTime;

    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime toTime;

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

    public LocalDateTime getFromTime() {
        return fromTime;
    }

    public LocalDateTime getToTime() {
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

    public void setFromTime(LocalDateTime fromTime) {
        this.fromTime = fromTime;
    }

    public void setToTime(LocalDateTime toTime) {
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
}
