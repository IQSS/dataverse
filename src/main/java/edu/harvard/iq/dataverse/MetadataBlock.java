/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 *
 * @author skraffmiller
 */

    
@Entity
public class MetadataBlock implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean showOnCreate;
    private String displayName;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    @OneToMany(mappedBy = "metadataBlock", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<DatasetField> datasetFields;
    public Collection<DatasetField> getDatasetFields() {
        return datasetFields;
    }
    public void setDatasetFields(Collection<DatasetField> datasetFields) {
        this.datasetFields = datasetFields;
    }

    public boolean isShowOnCreate() {
        return showOnCreate;
    }
    public void setShowOnCreate(boolean showOnCreate) {
        this.showOnCreate = showOnCreate;      
    }
    
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
