/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 *
 * @author skraffmiller
 */
@Entity
public class DefaultValueSet implements Serializable{
        
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    @Column( nullable = false )
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
   @OneToMany(mappedBy="defaultValueSet", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetFieldDefaultValue> datasetFieldDefaultValues; 

    public List<DatasetFieldDefaultValue> getDatasetFieldDefaultValues() {
        return datasetFieldDefaultValues;
    }

    public void setDatasetFieldDefaultValues(List<DatasetFieldDefaultValue> datasetFieldDefaultValues) {
        this.datasetFieldDefaultValues = datasetFieldDefaultValues;
    }
    
}
