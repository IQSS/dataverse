package edu.harvard.iq.dataverse.persistence.dataverse.link;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "dataverse_id"),
        @Index(columnList = "linkingDataverse_id")
})
public class DataverseLinkingDataverse implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false)
    private Dataverse dataverse;

    @OneToOne
    @JoinColumn(nullable = false)
    private Dataverse linkingDataverse;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date linkCreateTime;

    public Long getId() {
        return id;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public Date getLinkCreateTime() {
        return linkCreateTime;
    }

    public void setLinkCreateTime(Date linkCreateTime) {
        this.linkCreateTime = linkCreateTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DataverseLinkingDataverse)) {
            return false;
        }
        DataverseLinkingDataverse other = (DataverseLinkingDataverse) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "DataverseLinkedDataverse[ id=" + id + " ]";
    }

}
