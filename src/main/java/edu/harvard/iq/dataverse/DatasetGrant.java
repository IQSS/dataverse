/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

/**
 *
 * @author skraffmiller
 */
@Entity
public class DatasetGrant implements java.io.Serializable  {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String agency;
    public String getAgency() {
        return this.agency;
    }
    public void setAgency(String agency) {
        this.agency = agency;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ManyToOne
    @JoinColumn(nullable=false)
    private Metadata metadata;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @Version
    private Long version;
    public Long getVersion() {
        return this.version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }

    @Column(columnDefinition="TEXT")
    private String number;
    public String getNumber() {
        return this.number;
    }
    public void setNumber(String number) {
        this.number = number;
    }  
    
    public boolean isEmpty() {
        return ((agency==null || agency.trim().equals(""))
            && (number==null || number.trim().equals("")));
    }
  public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetGrant)) {
            return false;
        }
        DatasetGrant other = (DatasetGrant)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }    
    
}
