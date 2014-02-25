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
public class DatasetDistributor {
        
    /** Creates a new instance of StudyDistributor */
    public DatasetDistributor() {
    }

    /**
     * Holds value of property id.
     */
    
    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetFieldValue name;
    public DatasetFieldValue getName() {
        return this.name;
    }
    public void setName(DatasetFieldValue name) {
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

    private DatasetFieldValue url;
    public DatasetFieldValue getUrl() {
        return this.url;
    }
    public void setUrl(DatasetFieldValue url) {
        this.url = url;
    }
    
    private DatasetFieldValue logo;
    public DatasetFieldValue getLogo() {
        return this.logo;
    }
    public void setLogo(DatasetFieldValue logo) {
        this.logo = logo;
    }
    
    private DatasetFieldValue affiliation;
    public DatasetFieldValue getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(DatasetFieldValue affiliation) {
        this.affiliation = affiliation;
    }

    private DatasetFieldValue abbreviation;
    public DatasetFieldValue getAbbreviation() {
        return this.abbreviation;
    }
    public void setAbbreviation(DatasetFieldValue abbreviation) {
        this.abbreviation = abbreviation;
    }
    
      public boolean isEmpty() {
        return ((abbreviation==null || abbreviation.getStrValue().trim().equals(""))
            && (affiliation==null || affiliation.getStrValue().trim().equals(""))
            && (logo==null || logo.getStrValue().trim().equals(""))
            && (name==null || name.getStrValue().trim().equals(""))
            && (url==null || url.getStrValue().trim().equals("")));
    }
      
    
}
