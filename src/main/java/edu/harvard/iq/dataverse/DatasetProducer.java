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

/**
 *
 * @author skraffmiller
 */
@Entity
public class DatasetProducer implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private String name;
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Version
    private Long version;
    public Long getVersion() {
        return this.version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }    

    private String url;
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    private String logo;
    public String getLogo() {
        return this.logo;
    }
    public void setLogo(String logo) {
        this.logo = logo;
    }

    private String abbreviation;
    public String getAbbreviation() {
        return this.abbreviation;
    }
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    private String affiliation;
    public String getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
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
    
    
    public boolean isEmpty() {
        return ((abbreviation==null || abbreviation.trim().equals(""))
            && (affiliation==null || affiliation.trim().equals(""))
            && (logo==null || logo.trim().equals(""))
            && (name==null || name.trim().equals(""))
            && (url==null || url.trim().equals("")));
    }
   public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetProducer)) {
            return false;
        }
        DatasetProducer other = (DatasetProducer)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }
    
}
