package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class DataverseBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromTime;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date toTime;

    private boolean active;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dataverseBanner")
    private List<DataverseLocalizedBanner> dataverseLocalizedBanner = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Dataverse dataverse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<DataverseLocalizedBanner> getDataverseLocalizedBanner() {
        return dataverseLocalizedBanner;
    }

    public void setDataverseLocalizedBanner(List<DataverseLocalizedBanner> dataverseLocalizedBanner) {
        this.dataverseLocalizedBanner = dataverseLocalizedBanner;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }
}
