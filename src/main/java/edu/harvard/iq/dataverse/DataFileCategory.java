/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 *
 * @author Leonid Andreev
 */

@Entity
public class DataFileCategory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /*
     * Dataset to which this file category belongs:
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataset dataset;

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    private String name;
    
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /* 
     * DataFiles which belong to this category: 
     */
    @ManyToMany (mappedBy="fileCategories", cascade = {CascadeType.REMOVE})
    private Collection<FileMetadata> fileMetadatas = null; 
    
    public Collection<FileMetadata> getFileMetadatas() {
        return fileMetadatas; 
    }
    
    public void setFileMetadatas(Collection<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas; 
    }
    
    public void addFileMetadata (FileMetadata fileMetadata) {
        if (fileMetadatas == null) {
            fileMetadatas = new ArrayList<>();
        }
        fileMetadatas.add(fileMetadata);
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
        if (!(object instanceof DataFileCategory)) {
            return false;
        }
        DataFileCategory other = (DataFileCategory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataFileCategory[ id=" + id + " ]";
    }
}
