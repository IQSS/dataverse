/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;


/**
 *
 * @author skraffmiller
 */

public class DatasetAuthor {
       
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }

    //@NotBlank(message = "Please enter an Author Name for your dataset.")
    private DatasetFieldValue name;

    public DatasetFieldValue getName() {
        return this.name;
    }
    public void setName(DatasetFieldValue name) {
        this.name = name;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetFieldValue affiliation;
    public DatasetFieldValue getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(DatasetFieldValue affiliation) {
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
        return ( (affiliation==null || affiliation.getStrValue().trim().equals(""))
            && (name==null || name.getStrValue().trim().equals(""))
           );
    }
    
}
