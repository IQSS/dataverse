/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;
import jakarta.persistence.OrderBy;

import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 * 
 * Largely based on the the DataTable entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */

@Entity
@Table(indexes = {@Index(columnList="datafile_id")})
public class DataTable implements Serializable {
    
    /** Creates a new instance of DataTable */
    public DataTable() {
    }
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * unf: the Universal Numeric Signature of the 
     * data table.
     */
    @Column( nullable = false )
    private String unf;
    
    /*
     * caseQuantity: Number of observations
     */    
    private Long caseQuantity; 
    
    
    /*
     * varQuantity: Number of variables
     */
    private Long varQuantity;

    /*
     * recordsPerCase: this property is specific to fixed-field data files
     * in which rows of observations may represented by *multiple* lines.
     * The only known use case (so far): the fixed-width data files from 
     * ICPSR. 
     */
     private Long recordsPerCase;
     
     /*
      * DataFile that stores the data for this DataTable
      */
     @ManyToOne
     @JoinColumn(nullable=false)
     private DataFile dataFile;

     /*
      * DataVariables in this DataTable:
     */
    @OneToMany (mappedBy="dataTable", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    @OrderBy ("fileOrder")
    private List<DataVariable> dataVariables;
    
    /* 
     * originalFileType: the format of the file from which this data table was
     * extracted (STATA, SPSS, R, etc.)
     * Note: this was previously stored in the StudyFile. 
     */
    private String originalFileFormat;
    
    /*
     * originalFormatVersion: the version/release number of the original file
     * format; for example, STATA 9, SPSS 12, etc. 
     */
    private String originalFormatVersion;
    
    /* 
     * Size of the original file:
    */
    
    private Long originalFileSize; 
    
    /**
     * originalFileName: the file name upon upload/ingest
     */
    @Column( nullable = true )
    private String originalFileName;
    
    
    /**
     * The physical tab-delimited file is in storage with the list of variable
     * names saved as the 1st line. This means that we do not need to generate 
     * this line on the fly. (Also means that direct download mechanism can be
     * used for this file!)
     */
    @Column(nullable = false)
    private boolean storedWithVariableHeader = false;  
    
    /*
     * Getter and Setter methods:
     */
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnf() {
        return this.unf;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public Long getCaseQuantity() {
        return this.caseQuantity;
    }    
    
    public void setCaseQuantity(Long caseQuantity) {
        this.caseQuantity = caseQuantity;
    }
    
    public Long getVarQuantity() {
        return this.varQuantity;
    }

    public void setVarQuantity(Long varQuantity) {
        this.varQuantity = varQuantity;
    }   
    
    public Long getRecordsPerCase() {
        return recordsPerCase;
    }

    public void setRecordsPerCase(Long recordsPerCase) {
        this.recordsPerCase = recordsPerCase;
    }
    
    public DataFile getDataFile() {
        return this.dataFile;
    }
    
    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

     
    public List<DataVariable> getDataVariables() {
        return this.dataVariables;
    }

    
    public void setDataVariables(List<DataVariable> dataVariables) {
        this.dataVariables = dataVariables;
    } 
    
    public String getOriginalFileFormat() {
        return originalFileFormat;
    }

    public void setOriginalFileFormat(String originalFileType) {
        this.originalFileFormat = originalFileType;
    }

    public Long getOriginalFileSize() {
        return originalFileSize; 
    }
    
    public void setOriginalFileSize(Long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }
    
    
    public String getOriginalFormatVersion() {
        return originalFormatVersion;
    }

    public void setOriginalFormatVersion(String originalFormatVersion) {
        this.originalFormatVersion = originalFormatVersion;
    }
       
    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public boolean isStoredWithVariableHeader() {
        return storedWithVariableHeader;
    }
    
    public void setStoredWithVariableHeader(boolean storedWithVariableHeader) {
        this.storedWithVariableHeader = storedWithVariableHeader;
    }
    
    /* 
     * Custom overrides for hashCode(), equals() and toString() methods:
     */
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataTable)) {
            return false;
        }
        DataTable other = (DataTable)object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataTable[ id=" + id + " ]";
    }
    
}
