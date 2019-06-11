/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.validator.constraints.NotBlank;

/**
 *  File metadata: specifically WorldMap layer information for a specific DataFile 
 * 
 * @author raprasad
 */
@NamedQueries({
    @NamedQuery(name = "MapLayerMetadata.findAll",
            query = "SELECT mlm FROM MapLayerMetadata mlm"),})
@Entity
@Table(indexes = {@Index(columnList="dataset_id")})
public class MapLayerMetadata implements Serializable {

      
    @Transient
    public final static String dataType = "MapLayerMetadata";
    
    @Transient
    public final static List<String> MANDATORY_JSON_FIELDS = Arrays.asList("layerName", "layerLink", "embedMapLink", "worldmapUsername");

   
    private static final long serialVersionUID = 1L;
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ForeignKey to DataFile
    //x@ManyToOne
    // For now, make this unique:  Each DataFile may only have one map
    @JoinColumn(nullable=false, unique=true)
    private DataFile dataFile;

    // ForeignKey to Dataset.
    // This is always reachable via the datafile.getOwner();
    // However, save the Dataset itself to potentially save an extra step
    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;

    @Column(nullable=false)
    @NotBlank(message = "{map.layername}")
    private String layerName;
    
    @Column(nullable=false)
    @NotBlank(message = "{map.layerlink}")
    private String layerLink;
    
    @Column(nullable=false)
    @NotBlank(message = "{map.link}")
    private String embedMapLink;
    
    @Column(nullable=true)
    @NotBlank(message = "{map.imagelink}")
    private String mapImageLink;
    
    @Column(nullable=false)
    @NotBlank(message = "{map.username}")
    private String worldmapUsername;
   

    /**
     * Was this layer created by joining a tabular file
     * to an existing file?
     */
    private boolean isJoinLayer;
    
    /**
     * Description if this was created via a tabular join, 
     */
    @Column(columnDefinition = "TEXT")
    private String joinDescription;
            
    /**
     * Links to alternative representations of the map
     *  in JSON format
     */
    @Column(columnDefinition = "TEXT")
    private String mapLayerLinks;

    /**
     * The HTTP Status code (200, 404, etc.) returned when you check to see if
     * the map/layer exists on the WorldMap side.
     */
    @Column(nullable=true)
    private int lastVerifiedStatus;

    /**
     * The time that lastVerifiedStatus was last recorded.
     */
    @Column(nullable=true)
    private Timestamp lastVerifiedTime;

    /**
     * Get property layerName.
     * @return value of property layerName.
     */
    public String getLayerName() {
            return this.layerName;
    }

    /**
     * Set property layerName.
     * @param layerName new value of property layerName.
     */
    public void setLayerName(String layerName) {
            this.layerName = layerName;
    }

    /**
     * Get property layerLink.
     * @return value of property layerLink.
     */
    public String getLayerLink() {
            return this.layerLink;
    }

    /**
     * Set property layerLink.
     * @param layerLink new value of property layerLink.
     */
    public void setLayerLink(String layerLink) {
            this.layerLink = layerLink;
    }
    
        /**
     * Get property mapImageLink.
     * @return value of property mapImageLink.
     */
    public String getMapImageLink() {
            return this.mapImageLink;
    }

    /**
     * Set property mapImageLink.
     * @param mapImageLink new value of property layerLink.
     */
    public void setMapImageLink(String mapImageLink) {
            this.mapImageLink = mapImageLink;
    }
    
    
    /**
     * Get property embedMapLink.
     * @return value of property embedMapLink.
     */
    public String getEmbedMapLink() {
            return this.embedMapLink;
    }

    /**
     * Set property embedMapLink.
     * @param embedMapLink new value of property embedMapLink.
     */
    public void setEmbedMapLink(String embedMapLink) {
            this.embedMapLink = embedMapLink;
    }

    /**
     * Get property worldmapUsername.
     * @return value of property worldmapUsername.
     */
    public String getWorldmapUsername() {
            return this.worldmapUsername;
    }

    /**
     * Set property worldmapUsername.
     * @param worldmapUsername new value of property worldmapUsername.
     */
    public void setWorldmapUsername(String worldmapUsername) {
            this.worldmapUsername = worldmapUsername;
    }

    
    /**
     * Get property isJoinLayer.
     * @return value of property isJoinLayer.
     */
    public boolean isJoinLayer(){
            return this.isJoinLayer;
    }

    /**
     * Set property isJoinLayer.
     * @param bool new value of property isJoinLayer.
     */
    public void setIsJoinLayer(boolean bool) {
            this.isJoinLayer = bool;
    }
    
    /**
     * Get property joinDescription.
     * @return value of property joinDescription.
     */
    public String getJoinDescription() {
            return this.joinDescription;
    }

    /**
     * Set property joinDescription.
     * @param joinDescription new value of property joinDescription.
     */
    public void setJoinDescription(String joinDescription) {
            this.joinDescription = joinDescription;
    }
    
    /**
     * Get property mapLayerLinks
     * @return value of property joinDescription.
     */
    public String getMapLayerLinks() {
            return this.mapLayerLinks;
    }

    /**
     * Set property joinDescription.
     * @param joinDescription new value of property joinDescription.
     */
    public void setMapLayerLinks(String mapLayerLinks) {
            this.mapLayerLinks = mapLayerLinks;
    }
    
    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

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

    public Timestamp getLastVerifiedTime() {
        return lastVerifiedTime;
    }

    public void setLastVerifiedTime(Timestamp lastVerifiedTime) {
        this.lastVerifiedTime = lastVerifiedTime;
    }

    public int getLastVerifiedStatus() {
        return lastVerifiedStatus;
    }

    public void setLastVerifiedStatus(int lastVerifiedStatus) {
        this.lastVerifiedStatus = lastVerifiedStatus;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.MaplayerMetadata[id=" + this.id + "]";

        //return "WorldMap Layer: " + this.layerName + " for DataFile: " + this.dataFile.toString();
    }

}
