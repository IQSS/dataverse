/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
/*
 * FileMetadataField.java
 *
 * Taken virtually unchanged from DVN 3.*;
 * Originally created in Feb. 2013
 *
 */
package edu.harvard.iq.dataverse;

/**
 *
 * @author Leonid Andreev
 */

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


@Entity
public class FileMetadataFieldValue implements Serializable {

    public FileMetadataFieldValue () {
    }
    
    public FileMetadataFieldValue(FileMetadataField fmf, DataFile sf, String val) {
        setFileMetadataField(fmf);
        setStudyFile(sf);
        setStrValue(val);    
    }    
    
    
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
     * fileMetadataField, corresponding FileMetadataField
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private FileMetadataField fileMetadataField;

    /**
     * dataFile, corresponding StudyFile
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;

    
    @Column(columnDefinition="TEXT") 
    private String strValue;

    private int displayOrder;
    
    
    /**
     * Getter and Setter methods: 
     */
    
    
    public FileMetadataField getFileMetadataField() {
        return fileMetadataField;
    }

    public void setFileMetadataField(FileMetadataField fileMetadataField) {
        this.fileMetadataField=fileMetadataField;
    }
  
    public DataFile getStudyFile() {
        return dataFile;
    }

    public void setStudyFile(DataFile dataFile) {
        this.dataFile=dataFile;
    }
    
    
    
    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
        
    }
    
    public int getDisplayOrder() { return this.displayOrder;}
    public void setDisplayOrder(int displayOrder) {this.displayOrder = displayOrder;}
    
    /**
     * Class-specific method overrides:
     */
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileMetadataFieldValue)) {
            return false;
        }
        FileMetadataFieldValue other = (FileMetadataFieldValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.FileMetadataFieldValue[ id=" + id + " ]";
    }
    
    /**
     * Helper methods:
     * 
     * @return 
     */
     public boolean isEmpty() {
        return ((strValue==null || strValue.trim().equals("")));
    }
    
        
}
