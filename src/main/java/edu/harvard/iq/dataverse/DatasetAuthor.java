/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@Entity
public class DatasetAuthor implements java.io.Serializable {
       
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    //@NotBlank(message = "Please enter an Author Name for your dataset.")
    private String name;

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Holds value of property metadata.
     */
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


    private String affiliation;
    public String getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
    
    private String idType;

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
    
    private String idValue;
    
    
    public String getIdValue() {
        return idValue;
    }

    public void setIdValue(String idValue) {
        this.idValue = idValue;
        if (!this.idValue.isEmpty()){
            setIdType("ORCID");
        } else {
            setIdType("");
        }
    }
       
    public boolean isEmpty() {
        return ( (affiliation==null || affiliation.trim().equals(""))
            && (name==null || name.trim().equals(""))
           );
    }
    
     public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetAuthor)) {
            return false;
        }
        DatasetAuthor other = (DatasetAuthor)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }
    
}
