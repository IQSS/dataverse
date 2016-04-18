/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
    
    public static final String SCHEDULE_PERIOD_DAILY="daily";
    public static final String SCHEDULE_PERIOD_WEEKLY="weekly";
    
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

    public boolean isOai() {
        return HARVEST_TYPE_OAI.equals(harvestType);
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
    
    private String metadataPrefix;
    
    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }
    
    private String harvestResult;
    
    public String getHarvestResult() {
        return harvestResult;
    }

    public void setHarvestResult(String harvestResult) {
        this.harvestResult = harvestResult;
    }
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastHarvestTime;

    public Date getLastHarvestTime() {
        return lastHarvestTime;
    }

    public void setLastHarvestTime(Date lastHarvestTime) {
        this.lastHarvestTime = lastHarvestTime;
    }
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastSuccessfulHarvestTime; 
    
    public Date getLastSuccessfulHarvestTime() {
        return lastSuccessfulHarvestTime;
    }

    public void setLastSuccessfulHarvestTime(Date lastSuccessfulHarvestTime) {
        this.lastSuccessfulHarvestTime = lastSuccessfulHarvestTime;
    }
    
    private Long harvestedDatasetCount;
    private Long failedDatasetCount;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastSuccessfulNonEmptyHarvestTime;
    private Long lastNonEmptyHarvestedDatasetCount;
    private Long lastNonEmptyFailedDatasetCount;
    
    private boolean scheduled;

    public boolean isScheduled() {
        return this.scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }
    
    private String schedulePeriod;
    
    public String getSchedulePeriod() {
        return schedulePeriod;
    }

    public void setSchedulePeriod(String schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }
    
    private Integer scheduleHourOfDay;

    public Integer getScheduleHourOfDay() {
        return scheduleHourOfDay;
    }

    public void setScheduleHourOfDay(Integer scheduleHourOfDay) {
        this.scheduleHourOfDay = scheduleHourOfDay;
    }

    private Integer scheduleDayOfWeek;
    
    public Integer getScheduleDayOfWeek() {
        return scheduleDayOfWeek;
    }

    public void setScheduleDayOfWeek(Integer scheduleDayOfWeek) {
        this.scheduleDayOfWeek = scheduleDayOfWeek;
    }
    
    public String getScheduleDescription() {
        Date date = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        SimpleDateFormat weeklyFormat = new SimpleDateFormat(" E h a ");
        SimpleDateFormat  dailyFormat = new SimpleDateFormat(" h a ");
        String desc = "Not Scheduled";
        if (schedulePeriod!=null && schedulePeriod!="") {
            cal.set(Calendar.HOUR_OF_DAY, scheduleHourOfDay);
            if (schedulePeriod.equals(this.SCHEDULE_PERIOD_WEEKLY)) {
                cal.set(Calendar.DAY_OF_WEEK,scheduleDayOfWeek);
                desc="Weekly, "+weeklyFormat.format(cal.getTime());
            } else {
                desc="Daily, "+dailyFormat.format(cal.getTime());
            }
        }
        return desc;
    }
    
    private boolean harvestingNow;

    public boolean isHarvestingNow() {
        return this.harvestingNow;
    }

    public void setHarvestingNow(boolean harvestingNow) {
        this.harvestingNow = harvestingNow;
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
