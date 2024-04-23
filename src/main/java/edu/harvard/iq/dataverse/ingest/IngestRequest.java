/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 */
@Entity
@Table(indexes = {@Index(columnList="datafile_id")})
public class IngestRequest implements Serializable {
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

    @OneToOne(cascade={CascadeType.MERGE,CascadeType.PERSIST})
    @JoinColumn(name="datafile_id")
    private DataFile dataFile;
    
    private String textEncoding; 
    
    private String controlCard;
    
    private String labelsFile; 
    
    private Boolean forceTypeCheck;
    
    public IngestRequest() {
    }
    
    public IngestRequest(DataFile dataFile) {
        this.dataFile = dataFile;
    }
    
    public DataFile getDataFile() {
        return dataFile;
    }
    
    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile; 
    }
    
    public String getTextEncoding() {
        return textEncoding;
    }
    
    public void setTextEncoding(String textEncoding) {
        this.textEncoding = textEncoding; 
    }

    public String getControlCard() {
        return controlCard;
    }
    
    public void setControlCard(String controlCard) {
        this.controlCard = controlCard; 
    }
    
    public String getLabelsFile() {
        return labelsFile;
    }
    
    public void setLabelsFile(String labelsFile) {
        this.labelsFile = labelsFile; 
    }
    
    public void setForceTypeCheck(boolean forceTypeCheck) {
        this.forceTypeCheck = forceTypeCheck;
    }
    
    public boolean isForceTypeCheck() {
        if (forceTypeCheck != null) {
            return forceTypeCheck;
        }
        return false;
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
        if (!(object instanceof IngestRequest)) {
            return false;
        }
        IngestRequest other = (IngestRequest) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ingest.IngestRequest[ id=" + id + " ]";
    }
    
}
