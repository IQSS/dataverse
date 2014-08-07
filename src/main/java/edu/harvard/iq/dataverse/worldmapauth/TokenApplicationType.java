/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.DataverseUser;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

/**
 *
 * @author rmp553
 */
@Entity
@Table(indexes = {@Index(name = "application_name",  columnList="name", unique = true)})
public class TokenApplicationType implements java.io.Serializable {    

    @Id
    private Long id;
  
    @Column(nullable=false)
    private String name;
    
    private String contactEmail;
    
    @Column(nullable=true)
    private String hostname;
    
    @Column(nullable=true)
    private String ipAddress;
    
    
    @Column(nullable=false)
    private String mapitLink;

    //api_permissions = models.ManyToManyField(APIPermission, blank=True, null=True)
    @Column(nullable=false, name="timeLimitMinutes", columnDefinition="int default 30")    
    private int timeLimitMinutes;// = models.IntegerField(default=30, help_text='in minutes')
    
    @Column(nullable=false, name="timeLimitSeconds", columnDefinition="bigint default 1800")
    private long timeLimitSeconds;
    
    @Column(nullable=false)
    private String md5;
    
    @Column(nullable=false)
    private Timestamp created;
    
    @Column(nullable=false)
    private Timestamp modified;

        /**
     * Get property id.
     * @return Long, value of property id.
     */
    public Long getId() {
            return this.id;
    }

    /**
     * Set property id.
     * @param id new value of property id.
     */
    public void setId(Long id) {
            this.id = id;
    }


    /**
     * Get property name.
     * @return String, value of property name.
     */
    public String getName() {
            return this.name;
    }

    /**
     * Set property name.
     * @param name new value of property name.
     */
    public void setName(String name) {
            this.name = name;
    }


    /**
     * Get property contactEmail.
     * @return String, value of property contactEmail.
     */
    public String getContactEmail() {
            return this.contactEmail;
    }

    /**
     * Set property contactEmail.
     * @param contactEmail new value of property contactEmail.
     */
    public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
    }


    /**
     * Get property hostname.
     * @return String, value of property hostname.
     */
    public String getHostname() {
            return this.hostname;
    }

    /**
     * Set property hostname.
     * @param hostname new value of property hostname.
     */
    public void setHostname(String hostname) {
            this.hostname = hostname;
    }


    /**
     * Get property ipAddress.
     * @return String, value of property ipAddress.
     */
    public String getIpAddress() {
            return this.ipAddress;
    }

    /**
     * Set property ipAddress.
     * @param ipAddress new value of property ipAddress.
     */
    public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
    }


    /**
     * Get property mapitLink.
     * @return String, value of property mapitLink.
     */
    public String getMapitLink() {
            return this.mapitLink;
    }

    /**
     * Set property mapitLink.
     * @param mapitLink new value of property mapitLink.
     */
    public void setMapitLink(String mapitLink) {
            this.mapitLink = mapitLink;
    }


    /**
     * Get property timeLimitMinutes.
     * @return int, value of property timeLimitMinutes.
     */
    public int getTimeLimitMinutes() {
            return this.timeLimitMinutes;
    }

    /**
     * Set property timeLimitMinutes.
     * @param timeLimitMinutes new value of property timeLimitMinutes.
     */
    public void setTimeLimitMinutes(int timeLimitMinutes) {
            this.timeLimitMinutes = timeLimitMinutes;
    }


    /**
     * Get property timeLimitSeconds.
     * @return long, value of property timeLimitSeconds.
     */
    public long getTimeLimitSeconds() {
            return this.timeLimitSeconds;
    }

    /**
     * Set property timeLimitSeconds.
     * @param timeLimitSeconds new value of property timeLimitSeconds.
     */
    public void setTimeLimitSeconds(long timeLimitSeconds) {
            this.timeLimitSeconds = timeLimitSeconds;
    }




    /**
     * Get property md5.
     * @return String, value of property md5.
     */
    public String getMd5() {
            return this.md5;
    }

    /**
     * Set property md5.
     * @param md5 new value of property md5.
     */
    public void setMd5(String md5) {
            this.md5 = md5;
    }


    /**
     * Get property modified.
     * @return Timestamp, value of property modified.
     */
    public Timestamp getModified() {
            return this.modified;
    }

    /**
     * Set property modified.
     */
    public void setModified() {
        this.modified = new Timestamp(new Date().getTime());
    }


    /**
     * Get property created.
     * @return Timestamp, value of property created.
     */
    public Timestamp getCreated() {
            return this.created;
    }

    /**
     * Set property created.
     */
    public void setCreated() {
            this.created = new Timestamp(new Date().getTime());
    }

    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.worldmapauth.TokenApplicationType[id=" + this.id + "]";

        //return "WorldMap Layer: " + this.layerName + " for DataFile: " + this.dataFile.toString();
    }

}
