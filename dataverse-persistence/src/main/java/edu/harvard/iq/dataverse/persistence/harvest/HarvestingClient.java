package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import io.vavr.control.Option;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author Leonid Andreev
 */

@Table(indexes = {@Index(columnList = "dataverse_id")
        , @Index(columnList = "harvesttype")
        , @Index(columnList = "harveststyle")
        , @Index(columnList = "harvestingurl")})
@Entity
@NamedQueries({
        @NamedQuery(name = "HarvestingClient.findByNickname", query = "SELECT hc FROM HarvestingClient hc WHERE LOWER(hc.name)=:nickName")
})
public class HarvestingClient implements Serializable, JpaEntity<Long> {
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

    public static final String SCHEDULE_PERIOD_DAILY = "daily";
    public static final String SCHEDULE_PERIOD_WEEKLY = "weekly";

    public HarvestingClient() {
        this.harvestType = HarvestType.OAI; // default harvestType
        this.harvestStyle = HarvestStyle.DATAVERSE; // default harvestStyle
    }

    @ManyToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    @OneToMany(mappedBy = "harvestedFrom", cascade = {CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    private List<Dataset> harvestedDatasets;

    public List<Dataset> getHarvestedDatasets() {
        return this.harvestedDatasets;
    }

    public void setHarvestedDatasets(List<Dataset> harvestedDatasets) {
        this.harvestedDatasets = harvestedDatasets;
    }

    @NotBlank(message = "{user.enterNickname}")
    @Column(nullable = false, unique = true)
    @Size(max = 30, message = "{user.nicknameLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}"),
            @Pattern(regexp = ".*\\D.*", message = "{user.nicknameNotnumber}")})
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    private HarvestType harvestType;

    public HarvestType getHarvestType() {
        return harvestType;
    }

    public void setHarvestType(HarvestType harvestType) {
        this.harvestType = harvestType;
    }

    @Enumerated(EnumType.STRING)
    private HarvestStyle harvestStyle;

    public HarvestStyle getHarvestStyle() {
        return harvestStyle;
    }

    public void setHarvestStyle(HarvestStyle harvestStyle) {
        this.harvestStyle = harvestStyle;
    }

    private String harvestingUrl;

    public String getHarvestingUrl() {
        return this.harvestingUrl;
    }

    public void setHarvestingUrl(String harvestingUrl) {
        this.harvestingUrl = Option.of(harvestingUrl).map(String::trim).getOrNull();
    }

    private String archiveUrl;

    public String getArchiveUrl() {
        return this.archiveUrl;
    }

    public void setArchiveUrl(String archiveUrl) {
        this.archiveUrl = archiveUrl;
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

    // TODO: do we need "orphanRemoval=true"? -- L.A. 4.4
    // TODO: should it be @OrderBy("startTime")? -- L.A. 4.4
    @OneToMany(mappedBy = "harvestingClient", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("id")
    private List<ClientHarvestRun> harvestHistory;

    public List<ClientHarvestRun> getRunHistory() {
        return harvestHistory;
    }

    public void setRunHistory(List<ClientHarvestRun> harvestHistory) {
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
        if (lastHarvest != null) {
            return lastHarvest.getStartTime();
        }
        return null;
    }

    public Date getLastSuccessfulHarvestTime() {
        ClientHarvestRun lastSuccessfulHarvest = getLastSuccessfulRun();
        if (lastSuccessfulHarvest != null) {
            return lastSuccessfulHarvest.getStartTime();
        }
        return null;
    }

    public Date getLastNonEmptyHarvestTime() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if (lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getStartTime();
        }
        return null;
    }

    public Long getLastHarvestedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if (lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getHarvestedDatasetCount();
        }
        return null;
    }

    public Long getLastFailedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if (lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getFailedDatasetCount();
        }
        return null;
    }

    public Long getLastDeletedDatasetCount() {
        ClientHarvestRun lastNonEmptyHarvest = getLastNonEmptyRun();
        if (lastNonEmptyHarvest != null) {
            return lastNonEmptyHarvest.getDeletedDatasetCount();
        }
        return null;
    }
    
    /* move the fields below to the new HarvestingClientRun class: 
    private String harvestResult;
    
    public String getResult() {
        return harvestResult;
    }

    public void setResult(String harvestResult) {
        this.harvestResult = harvestResult;
    }
    
    // "Last Harvest Time" is the last time we *attempted* to harvest 
    // from this remote resource. 
    // It wasn't necessarily a successful attempt!
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastHarvestTime;

    public Date getLastHarvestTime() {
        return lastHarvestTime;
    }

    public void setLastHarvestTime(Date lastHarvestTime) {
        this.lastHarvestTime = lastHarvestTime;
    }
    
    // This is the last "successful harvest" - i.e., the last time we 
    // tried to harvest, and got a response from the remote server. 
    // We may not have necessarily harvested any useful content though; 
    // the result may have been a "no content" or "no changes since the last harvest"
    // response. 
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastSuccessfulHarvestTime; 
    
    public Date getLastSuccessfulHarvestTime() {
        return lastSuccessfulHarvestTime;
    }

    public void setLastSuccessfulHarvestTime(Date lastSuccessfulHarvestTime) {
        this.lastSuccessfulHarvestTime = lastSuccessfulHarvestTime;
    }
    
    // Finally, this is the time stamp from the last "non-empty" harvest. 
    // I.e. the last time we ran a harvest that actually resulted in 
    // some Datasets created, updated or deleted:
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastNonEmptyHarvestTime;
    
    public Date getLastNonEmptyHarvestTime() {
        return lastNonEmptyHarvestTime;
    }

    public void setLastNonEmptyHarvestTime(Date lastNonEmptyHarvestTime) {
        this.lastNonEmptyHarvestTime = lastNonEmptyHarvestTime;
    }
    
    // And these are the Dataset counts from that last "non-empty" harvest:
    private Long harvestedDatasetCount;
    private Long failedDatasetCount;
    private Long deletedDatasetCount;
    
    public Long getLastHarvestedDatasetCount() {
        return harvestedDatasetCount;
    }

    public void setHarvestedDatasetCount(Long harvestedDatasetCount) {
        this.harvestedDatasetCount = harvestedDatasetCount;
    }
    
    public Long getLastFailedDatasetCount() {
        return failedDatasetCount;
    }

    public void setFailedDatasetCount(Long failedDatasetCount) {
        this.failedDatasetCount = failedDatasetCount;
    }
    
    public Long getLastDeletedDatasetCount() {
        return deletedDatasetCount;
    }

    public void setDeletedDatasetCount(Long deletedDatasetCount) {
        this.deletedDatasetCount = deletedDatasetCount;
    }
    */

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
        SimpleDateFormat dailyFormat = new SimpleDateFormat(" h a ");
        String desc = "Not Scheduled";
        if (schedulePeriod != null && schedulePeriod != "") {
            cal.set(Calendar.HOUR_OF_DAY, scheduleHourOfDay);
            if (schedulePeriod.equals(SCHEDULE_PERIOD_WEEKLY)) {
                cal.set(Calendar.DAY_OF_WEEK, scheduleDayOfWeek);
                desc = "Weekly, " + weeklyFormat.format(cal.getTime());
            } else {
                desc = "Daily, " + dailyFormat.format(cal.getTime());
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
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "HarvestingClient[ id=" + id + " ]";
    }

}
