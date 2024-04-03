/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author Leonid Andreev
 */

@Table(indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="harvesttype")
		, @Index(columnList="harveststyle")
		, @Index(columnList="harvestingurl")})
@Entity
@NamedQueries({
    @NamedQuery(name = "HarvestingClient.findByNickname", query="SELECT hc FROM HarvestingClient hc WHERE LOWER(hc.name)=:nickName")
})
public class HarvestingClient implements Serializable {
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
    
    public static final String HARVEST_TYPE_OAI="oai";
    public static final String HARVEST_TYPE_NESSTAR="nesstar";
    
    
    /* 
     * Different harvesting "styles". These define how we format and 
     * display meatada harvested from various remote resources. 
    */
    public static final String HARVEST_STYLE_DATAVERSE="dataverse";
    // pre-4.0 remote Dataverse:
    public static final String HARVEST_STYLE_VDC="vdc";
    public static final String HARVEST_STYLE_ICPSR="icpsr";
    public static final String HARVEST_STYLE_NESSTAR="nesstar";
    public static final String HARVEST_STYLE_ROPER="roper";
    public static final String HARVEST_STYLE_HGL="hgl";
    public static final String HARVEST_STYLE_DEFAULT="default";
    
    public static final String HARVEST_STYLE_DESCRIPTION_DATAVERSE="Dataverse v4+";
    // pre-4.0 remote Dataverse:
    public static final String HARVEST_STYLE_DESCRIPTION_VDC="DVN, v2-3";
    public static final String HARVEST_STYLE_DESCRIPTION_ICPSR="ICPSR";
    public static final String HARVEST_STYLE_DESCRIPTION_NESSTAR="Nesstar archive";
    public static final String HARVEST_STYLE_DESCRIPTION_ROPER="Roper Archive";
    public static final String HARVEST_STYLE_DESCRIPTION_HGL="HGL";
    public static final String HARVEST_STYLE_DESCRIPTION_DEFAULT="Generic OAI archive";
    
    
    public static final List<String> HARVEST_STYLE_LIST = Arrays.asList(HARVEST_STYLE_DATAVERSE, HARVEST_STYLE_VDC, HARVEST_STYLE_ICPSR, HARVEST_STYLE_NESSTAR, HARVEST_STYLE_ROPER, HARVEST_STYLE_HGL, HARVEST_STYLE_DEFAULT);
    public static final List<String> HARVEST_STYLE_DESCRIPTION_LIST = Arrays.asList(HARVEST_STYLE_DESCRIPTION_DATAVERSE, HARVEST_STYLE_DESCRIPTION_VDC, HARVEST_STYLE_DESCRIPTION_ICPSR, HARVEST_STYLE_DESCRIPTION_NESSTAR, HARVEST_STYLE_DESCRIPTION_ROPER, HARVEST_STYLE_DESCRIPTION_HGL, HARVEST_STYLE_DESCRIPTION_DEFAULT);
    
    public static final Map<String, String> HARVEST_STYLE_INFOMAP = new LinkedHashMap<String, String>();
    
    static {
        for (int i=0; i< HARVEST_STYLE_LIST.size(); i++){
            HARVEST_STYLE_INFOMAP.put(HARVEST_STYLE_LIST.get(i), HARVEST_STYLE_DESCRIPTION_LIST.get(i));
        }
    }
    

    
    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATAVERSE="dataverse";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATASET="dataset";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_FILE="file";
    
    public static final String SCHEDULE_PERIOD_DAILY="daily";
    public static final String SCHEDULE_PERIOD_WEEKLY="weekly";
    
    public HarvestingClient() {
        this.harvestType = HARVEST_TYPE_OAI; // default harvestType
        this.harvestStyle = HARVEST_STYLE_DATAVERSE; // default harvestStyle
    }

    
    @ManyToOne
    @JoinColumn(name="dataverse_id")
    private  Dataverse dataverse;

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    @OneToMany (mappedBy="harvestedFrom", cascade={CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval=true)
    private List<Dataset> harvestedDatasets;

    public List<Dataset> getHarvestedDatasets() {
        return this.harvestedDatasets;
    }

    public void setHarvestedDatasets(List<Dataset> harvestedDatasets) {
        this.harvestedDatasets = harvestedDatasets;
    }
    
    @NotBlank(message = "{user.enterNickname}")
    @Column(nullable = false, unique=true)
    @Size(max = 30, message = "{user.nicknameLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}"),
        @Pattern(regexp=".*\\D.*", message="{user.nicknameNotnumber}")})
    private String name; 
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name; 
    }
    
    private String harvestType;

    public String getHarvestType() {
        return harvestType;
    }

    public void setHarvestType(String harvestType) {
        this.harvestType = harvestType;
    }

    public boolean isOai() {
        return HARVEST_TYPE_OAI.equals(harvestType);
    }
    
    private String harvestStyle;

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
        if (harvestingUrl != null) {
            this.harvestingUrl = harvestingUrl.trim();
        }
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
    
    private String customHttpHeaders; 
    
    public String getCustomHttpHeaders() {
        return customHttpHeaders;
    }
    
    public void setCustomHttpHeaders(String customHttpHeaders) {
        this.customHttpHeaders = customHttpHeaders;
    }

    private boolean allowHarvestingMissingCVV;
    public boolean getAllowHarvestingMissingCVV() {
        return allowHarvestingMissingCVV;
    }
    public void setAllowHarvestingMissingCVV(boolean allowHarvestingMissingCVV) {
        this.allowHarvestingMissingCVV = allowHarvestingMissingCVV;
    }
    
    // TODO: do we need "orphanRemoval=true"? -- L.A. 4.4
    // TODO: should it be @OrderBy("startTime")? -- L.A. 4.4
    @OneToMany(mappedBy="harvestingClient", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("id")
    private List<ClientHarvestRun> harvestHistory;

    List<ClientHarvestRun> getRunHistory() {
        return harvestHistory;
    }
    
    void setRunHistory(List<ClientHarvestRun> harvestHistory) {
        this.harvestHistory = harvestHistory;
    }
    
    public String getLastResult() {
        if (harvestHistory == null || harvestHistory.size() == 0) {
            return null; 
        }
        return harvestHistory.get(harvestHistory.size() - 1).getResultLabel();
    }
    
    public ClientHarvestRun getLastRun() {
        if (harvestHistory == null || harvestHistory.size() == 0) {
            return null; 
        }
        
        return harvestHistory.get(harvestHistory.size() - 1);
    }
    
    public ClientHarvestRun getLastSuccessfulRun() {
        if (harvestHistory == null || harvestHistory.size() == 0) {
            return null; 
        }
        
        int i = harvestHistory.size() - 1;
        
        while (i > -1) {
            if (harvestHistory.get(i).isSuccess()) {
                return harvestHistory.get(i);
            }
            i--;
        }
        
        return null; 
    }
    
    ClientHarvestRun getLastNonEmptyRun() {
        if (harvestHistory == null || harvestHistory.size() == 0) {
            return null; 
        }
        
        int i = harvestHistory.size() - 1;
        
        while (i > -1) {
            if (harvestHistory.get(i).isSuccess()) {
                if (harvestHistory.get(i).getHarvestedDatasetCount().longValue() > 0 ||
                    harvestHistory.get(i).getDeletedDatasetCount().longValue() > 0) {
                    return harvestHistory.get(i);
                }
            }
            i--;
        }
        return null; 
    }
    
    public Date getLastHarvestTime() {
        ClientHarvestRun lastHarvest = getLastRun();
        if ( lastHarvest != null) {
            return lastHarvest.getStartTime();
        }
        return null;
    }
    
    public Date getLastSuccessfulHarvestTime() {
        ClientHarvestRun lastSuccessfulHarvest = getLastSuccessfulRun();
        if ( lastSuccessfulHarvest != null) {
            return lastSuccessfulHarvest.getStartTime();
        }
        return null;
    }
    
    public Date getLastNonEmptyHarvestTime() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if ( lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getStartTime();
        }
        return null;
    }
    
    public Long getLastHarvestedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if ( lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getHarvestedDatasetCount();
        }
        return null;
    }
    
    public Long getLastFailedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if ( lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getFailedDatasetCount();
        }
        return null;
    }
    
    public Long getLastDeletedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if ( lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getDeletedDatasetCount();
        }
        return null;
    }   
    
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
    
    private boolean deleted;

    
    public boolean isDeleteInProgress() {
        return this.deleted;
    }

    public void setDeleteInProgress(boolean deleteInProgress) {
        this.deleted = deleteInProgress; 
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
        if (!(object instanceof HarvestingClient)) {
            return false;
        }
        HarvestingClient other = (HarvestingClient) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.harvest.client.HarvestingClient[ id=" + id + " ]";
    }
    
}
