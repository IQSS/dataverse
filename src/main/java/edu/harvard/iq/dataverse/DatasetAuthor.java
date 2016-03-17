/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Comparator;


/**
 *
 * @author skraffmiller
 */

public class DatasetAuthor {
       
    public static Comparator<DatasetAuthor> DisplayOrder = new Comparator<DatasetAuthor>(){
        @Override
        public int compare(DatasetAuthor o1, DatasetAuthor o2) {
            return o1.getDisplayOrder()-o2.getDisplayOrder();
        }
    };
    
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }

    //@NotBlank(message = "Please enter an Author Name for your dataset.")
    private DatasetField name;

    public DatasetField getName() {
        return this.name;
    }
    public void setName(DatasetField name) {
        this.name = name;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetField affiliation;
    public DatasetField getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(DatasetField affiliation) {
        this.affiliation = affiliation;
    }
    
    private String idType;

    public String getIdType() {
        if ((this.idType == null || this.idType.isEmpty()) && (this.idValue != null && !this.idValue.isEmpty())){
            return ("ORCID");
        } else {
            return idType;
        }        
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
    }

    public boolean isEmpty() {
        return ( (affiliation==null || affiliation.getValue().trim().equals(""))
            && (name==null || name.getValue().trim().equals(""))
           );
    }
    
}
