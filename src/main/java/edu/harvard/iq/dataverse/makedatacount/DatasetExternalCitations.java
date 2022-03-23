/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 *
 * @author skraffmi
 */
@Entity
public class DatasetExternalCitations implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    
    @NotNull
    @JoinColumn(nullable = false)
    @ManyToOne
    private Dataset dataset;
    
    @Column(nullable = false)
    private String citedByUrl;
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    public String getCitedByUrl() {
        return citedByUrl;
    }

    public void setCitedByUrl(String citedByUrl) {
        this.citedByUrl = citedByUrl;
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
        if (!(object instanceof DatasetExternalCitations)) {
            return false;
        }
        DatasetExternalCitations other = (DatasetExternalCitations) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations[ id=" + id + " ]";
    }
    
}
