/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Comparator;
import jakarta.persistence.Version;

/**
 *
 * @author skraffmiller
 */
public class DatasetDistributor {
    
    public static Comparator<DatasetDistributor> DisplayOrder = new Comparator<DatasetDistributor>() {
        @Override
        public int compare(DatasetDistributor o1, DatasetDistributor o2) {
            return o1.getDisplayOrder()-o2.getDisplayOrder();
        }
    };
    
    /** Creates a new instance of DatasetDistributor */
    public DatasetDistributor() {
    }


    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetField name;
    public DatasetField getName() {
        return this.name;
    }
    public void setName(DatasetField name) {
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

    private DatasetField url;
    public DatasetField getUrl() {
        return this.url;
    }
    public void setUrl(DatasetField url) {
        this.url = url;
    }
    
    private DatasetField logo;
    public DatasetField getLogo() {
        return this.logo;
    }
    public void setLogo(DatasetField logo) {
        this.logo = logo;
    }
    
    private DatasetField affiliation;
    public DatasetField getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(DatasetField affiliation) {
        this.affiliation = affiliation;
    }

    private DatasetField abbreviation;
    public DatasetField getAbbreviation() {
        return this.abbreviation;
    }
    public void setAbbreviation(DatasetField abbreviation) {
        this.abbreviation = abbreviation;
    }
    
      public boolean isEmpty() {
        return ((abbreviation==null || abbreviation.getValue().trim().equals(""))
            && (affiliation==null || affiliation.getValue().trim().equals(""))
            && (logo==null || logo.getValue().trim().equals(""))
            && (name==null || name.getValue().trim().equals(""))
            && (url==null || url.getValue().trim().equals("")));
    }
      
    
}
