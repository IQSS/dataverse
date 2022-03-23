/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 */
@Entity
@Table(indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="harvesttype")
		, @Index(columnList="harveststyle")
		, @Index(columnList="harvestingurl")})
public class HarvestingDataverseConfig implements Serializable {
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
    
    public static final String HARVEST_TYPE_OAI="oai";
    public static final String HARVEST_TYPE_NESSTAR="nesstar";
    
    public static final String HARVEST_STYLE_DATAVERSE="dataverse";
    // pre-4.0 remote Dataverse:
    public static final String HARVEST_STYLE_VDC="vdc";
    public static final String HARVEST_STYLE_ICPSR="icpsr";
    public static final String HARVEST_STYLE_NESSTAR="nesstar";
    public static final String HARVEST_STYLE_ROPER="roper";
    public static final String HARVEST_STYLE_HGL="hgl";
    public static final String HARVEST_STYLE_DEFAULT="default";

    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATAVERSE="dataverse";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATASET="dataset";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_FILE="file";
    
    public HarvestingDataverseConfig() {
        this.harvestType = HARVEST_TYPE_OAI; // default harvestType
        this.harvestStyle = HARVEST_STYLE_DATAVERSE; // default harvestStyle
    }

    
    @OneToOne (cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST })
    @JoinColumn(name="dataverse_id")
    private  Dataverse dataverse;

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    String harvestType;

    public String getHarvestType() {
        return harvestType;
    }

    public void setHarvestType(String harvestType) {
        this.harvestType = harvestType;
    }

    String harvestStyle;

    public String getHarvestStyle() {
        return harvestStyle;
    }

    public void setHarvestStyle(String harvestStyle) {
        this.harvestStyle = harvestStyle;
    }
    
    private String harvestingUrl;

    public String getHarvestingUrl() {
        return this.harvestingUrl;
    }

    public void setHarvestingUrl(String harvestingUrl) {
        this.harvestingUrl = harvestingUrl.trim();
    }
    
    private String archiveUrl; 
    
    public String getArchiveUrl() {
        return this.archiveUrl;
    }
    
    public void setArchiveUrl(String archiveUrl) {
        this.archiveUrl = archiveUrl; 
    }

    @Column(columnDefinition="TEXT")
    private String archiveDescription; 
    
    public String getArchiveDescription() {
        return this.archiveDescription;
    }
    
    public void setArchiveDescription(String archiveDescription) {
        this.archiveDescription = archiveDescription; 
    }
    
    private String harvestingSet;

    public String getHarvestingSet() {
        return this.harvestingSet;
    }

    public void setHarvestingSet(String harvestingSet) {
        this.harvestingSet = harvestingSet;
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
        if (!(object instanceof HarvestingDataverseConfig)) {
            return false;
        }
        HarvestingDataverseConfig other = (HarvestingDataverseConfig) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.HarvestingDataverse[ id=" + id + " ]";
    }
    
}
