/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "dataverse_id"),
    @Index(columnList = "linkingDataverse_id")
})
@NamedQueries({
    @NamedQuery(name = "DataverseLinkingDataverse.findByDataverseId",
               query = "select object(o) from DataverseLinkingDataverse as o where o.dataverse.id =:dataverseId order by o.id"),
    @NamedQuery(name = "DataverseLinkingDataverse.findByLinkingDataverseId",
               query = "select object(o) from DataverseLinkingDataverse as o where o.linkingDataverse.id =:linkingDataverseId order by o.id"),      
    @NamedQuery(name = "DataverseLinkingDataverse.findByDataverseIdAndLinkingDataverseId",
               query = "SELECT OBJECT(o) FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId AND o.dataverse.id = :dataverseId"),
    @NamedQuery(name = "DataverseLinkingDataverse.findIdsByLinkingDataverseId",
               query = "SELECT o.dataverse.id FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId")
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
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseLinkedDataverse[ id=" + id + " ]";
    }
    
}
