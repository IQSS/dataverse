/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import org.hibernate.validator.constraints.NotBlank;
//import org.hibernate.validator.Pattern;
import javax.validation.constraints.Pattern;


/**
 *
 * @author skraffmiller
 */
@Entity
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = Logger.getLogger(FileMetadata.class.getCanonicalName());

    //@NotBlank(message = "Please specify a file name.")
    @Pattern(regexp="^[^:<>;#/\"\\*\\|\\?\\\\]*$", message = "File Name cannot contain any of the following characters: \\ / : * ? \" < > | ; # .")    
    @NotBlank(message = "Please specify a file name.")
    private String label = "";
    @Column(columnDefinition = "TEXT")
    private String description = "";
    @Column(columnDefinition="TEXT")
    private String category = ""; // TODO: remove! -- L.A. 4.0 beta 10

    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetVersion datasetVersion;
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;

    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // TODO: remove the following 2 methods: -- L.A. beta 10
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /* 
     * File Categories to which this version of the DataFile belongs: 
     */
    @ManyToMany (cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileCategory> fileCategories;
    
    public List<DataFileCategory> getCategories() {
        return fileCategories;
    }
    
    public void setCategories(List<DataFileCategory> fileCategories) {
        this.fileCategories = fileCategories; 
    }
    
    public void addCategory(DataFileCategory category) {
        if (fileCategories == null) {
            fileCategories = new ArrayList<>();
        }
        fileCategories.add(category);
    }

    public List<String> getCategoriesByName() {
        ArrayList<String> ret = new ArrayList<>();
        if (fileCategories != null) {
            for (int i = 0; i < fileCategories.size(); i++) {
                ret.add(fileCategories.get(i).getName());
            }
        }
        return ret;
    }
    
    // alternative, experimental method: 

    public void setCategoriesByName(List<String> newCategoryNames) {
        setCategories(null); // ?? TODO: investigate! 

        if (newCategoryNames != null) {

            for (int i = 0; i < newCategoryNames.size(); i++) {
                // Dataset.getCategoryByName() will check if such a category 
                // already exists for the parent dataset; it will be created 
                // if not. The method will return null if the supplied 
                // category name is null or empty. -- L.A. 4.0 beta 10
                DataFileCategory fileCategory = null;
                try {
                    // Using "try {}" to catch any null pointer exceptions, 
                    // just in case: 
                    fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryNames.get(i));
                } catch (Exception ex) {
                    fileCategory = null;
                }
                if (fileCategory != null) {
                    this.addCategory(fileCategory);
                    fileCategory.addFileMetadata(this);
                }
            }
        }
    }
    
    /* 
        note that this version only *adds* new categories, but does not 
        remove the ones that has been unchecked!
    public void setCategoriesByName(List<String> newCategoryNames) {
        if (newCategoryNames != null) {
            Collection<String> oldCategoryNames = getCategoriesByName();
            
            
            for (int i = 0; i < newCategoryNames.size(); i++) {
                if (!oldCategoryNames.contains(newCategoryNames.get(i))) {
                    // Dataset.getCategoryByName() will check if such a category 
                    // already exists for the parent dataset; it will be created 
                    // if not. The method will return null if the supplied 
                    // category name is null or empty. -- L.A. 4.0 beta 10
                    DataFileCategory fileCategory = null; 
                    try { 
                        // Using "try {}" to catch any null pointer exceptions, 
                        // just in case: 
                        fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryNames.get(i));
                    } catch (Exception ex) {
                        fileCategory = null; 
                    }
                    if (fileCategory != null) { 
                        this.addCategory(fileCategory);
                        fileCategory.addFileMetadata(this);
                    }
                } 
            }
        }
    }
    */
    
    public void addCategoryByName(String newCategoryName) {
        if (newCategoryName != null && !newCategoryName.equals("")) {
            Collection<String> oldCategoryNames = getCategoriesByName();
            if (!oldCategoryNames.contains(newCategoryName)) {
                DataFileCategory fileCategory = null;
                // Dataset.getCategoryByName() will check if such a category 
                // already exists for the parent dataset; it will be created 
                // if not. The method will return null if the supplied 
                // category name is null or empty. -- L.A. 4.0 beta 10
                try {
                    // Using "try {}" to catch any null pointer exceptions, 
                    // just in case: 
                    fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryName);
                } catch (Exception ex) {
                    fileCategory = null;
                }

                
                if (fileCategory != null) {
                    logger.info("Found file category for "+newCategoryName);

                    this.addCategory(fileCategory);
                    fileCategory.addFileMetadata(this);
                } else {
                    logger.info("Could not find file category for "+newCategoryName);
                }
            } else {
                // don't do anything - this file metadata already belongs to
                // this category.
            }
        }
    }
    
  
    
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }



    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Getter for property id.
     * @return Value of property id.
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     * @param id New value of property id.
     */
    public void setId(Long id) {
        this.id = id;
    }


    @Version
    private Long version;

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(Long version) {
        this.version = version;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FileMetadata)) {
            return false;
        }
        FileMetadata other = (FileMetadata) object;
        
        /* experimental comparison logic for 2 filemetadatas that 
           haven't been persisted with the entity manager yet, 
           and thus don't have db ids:
           compare by the dataset versions and the file md5s, 
           if available.
           
           (for it to work, comparison of dataset versions need to be
            added; and new equals() method needs to be defined in the 
            DatasetVersion class as well - that similarly does a better
            work comparing DatasetVersions with null ids) 
        
        if (this.id == null && other.id == null) {
            if (this.getDataFile() != null
                    && this.getDataFile().getmd5() != null
                    && other.getDataFile() != null) {
                return this.getDataFile().getmd5().equals(other.getDataFile().getmd5());
                
            } 
            return false; 
        }
        */
        
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.study.FileMetadata[id=" + id + "]";
    }
    
}
