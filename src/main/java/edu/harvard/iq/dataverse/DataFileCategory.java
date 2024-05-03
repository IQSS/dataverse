/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 */


@Entity
@Table(indexes = {@Index(columnList="dataset_id")})
public class DataFileCategory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
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
    
    @Expose
    @Column( nullable = false )
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
    @ManyToMany (mappedBy="fileCategories")
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
        if (!(object instanceof DataFileCategory)) {
            return false;
        }
        DataFileCategory other = (DataFileCategory) object;
        
        // Custom code for comparing 2 categories before the 
        // objects have been persisted with the entity manager
        // and assigned database ids: 
        // (will also need to compare datasets for it to work - ?
        /*
        if (this.id == null && other.id == null) {
            if (this.name != null) {
                return this.name.equals(other.name);
             }
            return false; 
        }*/
        
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
