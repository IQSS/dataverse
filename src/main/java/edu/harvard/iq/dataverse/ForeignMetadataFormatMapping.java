/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 */

@NamedQueries({
    @NamedQuery( name="ForeignMetadataFormatMapping.listAll", query = "SELECT fmfm FROM ForeignMetadataFormatMapping fmfm"),
    @NamedQuery( name="ForeignMetadataFormatMapping.findByName", query = "SELECT fmfm FROM ForeignMetadataFormatMapping fmfm WHERE fmfm.name=:name")
})
@Entity
@Table(indexes = {@Index(columnList="name")})
public class ForeignMetadataFormatMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "foreignMetadataFormatMapping", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<ForeignMetadataFieldMapping> foreignMetadataFieldMappings;
    
    @Column( nullable = false )
    private String name;
    @Column( nullable = false )
    private String displayName;
    private String schemaLocation;
    private String startElement; 
    
    /* getters/setters: */
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public List<ForeignMetadataFieldMapping> getDatasetFieldTypes() {
        return foreignMetadataFieldMappings;
    }
    
    public void setDatasetFieldTypes(List<ForeignMetadataFieldMapping> foreignMetadataFieldMappings) {
        this.foreignMetadataFieldMappings = foreignMetadataFieldMappings;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getSchemaLocation() {
        return schemaLocation ;
    }
    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation; 
    }
    
    public String getStartElement() {
        return startElement;
    }
    public void setStartElement(String startElement) {
        this.startElement = startElement; 
    }
    
    /* overrides: */
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ForeignMetadataFormatMapping)) {
            return false;
        }
        ForeignMetadataFormatMapping other = (ForeignMetadataFormatMapping) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ForeignMetadataFormatMapping[ id=" + id + " ]";
    }
    
}

