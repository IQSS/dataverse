package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.storageuse.StorageUse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.*;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link DvObject} that can contain other {@link DvObject}s.
 * 
 * @author michael
 */
@MappedSuperclass
public abstract class DvObjectContainer extends DvObject {
    
    public static final String UNDEFINED_CODE = "undefined"; //Used in dataverse.xhtml as a non-null selection option value (indicating inheriting the default)
    
    public void setOwner(Dataverse owner) {
        super.setOwner(owner);
    }
	
	@Override
	public Dataverse getOwner() {
		return super.getOwner()!=null ? (Dataverse)super.getOwner() : null;
	}
    
    protected abstract boolean isPermissionRoot();
    
    @Override
    public boolean isEffectivelyPermissionRoot() {
        return isPermissionRoot() || (getOwner() == null);
    }

    private String storageDriver=null;
    
    private String metadataLanguage=null;
    
    private Boolean guestbookAtRequest = null;
    
    private String pidGeneratorSpecs = null;
    
    @Transient
    private PidProvider pidGenerator = null;
   
    @OneToOne(mappedBy = "dvObjectContainer",cascade={ CascadeType.REMOVE, CascadeType.PERSIST}, orphanRemoval=true)
    private StorageUse storageUse;

    @Column( nullable = true )
    private Integer datasetFileCountLimit;
    
    public String getEffectiveStorageDriverId() {
        String id = storageDriver;
        if (StringUtils.isBlank(id)) {
            if (this.getOwner() != null) {
                id = this.getOwner().getEffectiveStorageDriverId();
            } else {
                id = DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER;
            }
        }
        return id;
    }
    
    public String getStorageDriverId() {
        if (storageDriver == null) {
            return DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER;
        }
        return storageDriver;
    }

    public void setStorageDriverId(String storageDriver) {
        if (storageDriver != null && storageDriver.equals(DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER)) {
            this.storageDriver = null;
        } else {
            this.storageDriver = storageDriver;
        }
    }
    
    public String getEffectiveMetadataLanguage() {
        String ml = metadataLanguage;
        if (StringUtils.isBlank(ml)) {
            if (this.getOwner() != null) {
                ml = this.getOwner().getEffectiveMetadataLanguage();
            } else {
                ml = UNDEFINED_CODE;
            }
        }
        return ml;
    }
    
    public String getMetadataLanguage() {
        if (metadataLanguage == null) {
            return UNDEFINED_CODE;
        }
        return metadataLanguage;
    }

    public void setMetadataLanguage(String ml) {
        if (ml != null && ml.equals(UNDEFINED_CODE)) {
            this.metadataLanguage = null;
        } else {
            this.metadataLanguage = ml;
        }
    }
    
    public static boolean isMetadataLanguageSet(String mdLang) {
        return mdLang!=null && !mdLang.equals(UNDEFINED_CODE);
    }
    
    public boolean getEffectiveGuestbookEntryAtRequest() {
        boolean gbAtRequest = false;
        if (guestbookAtRequest==null) {
            if (this.getOwner() != null) {
                gbAtRequest = this.getOwner().getEffectiveGuestbookEntryAtRequest();
            } else {
                Optional<Boolean> opt = JvmSettings.GUESTBOOK_AT_REQUEST.lookupOptional(Boolean.class);
                if (opt.isPresent()) {
                gbAtRequest = opt.get();
                }
            }
        } else {
            gbAtRequest = guestbookAtRequest;
        }
        return gbAtRequest;
    }
    
    public String getGuestbookEntryAtRequest() {
        if(guestbookAtRequest==null) {
            return UNDEFINED_CODE;
        }
        return Boolean.valueOf(guestbookAtRequest).toString();
    }

    public void setGuestbookEntryAtRequest(String gbAtRequest) {
        if (gbAtRequest == null || gbAtRequest.equals(UNDEFINED_CODE)) {
            this.guestbookAtRequest = null;
        } else {
            //Force to true or false
            this.guestbookAtRequest = Boolean.valueOf(Boolean.parseBoolean(gbAtRequest));
        }
    }
    

    /* Dataverse collections can be configured to allow use of Curation labels and have this inheritable value to decide which set of labels to use.
     * This mechanism is similar to that for the storageDriver except that there is an addition option to disable use of labels. 
     */
    private String externalLabelSetName = null;

    public String getEffectiveCurationLabelSetName() {
        String setName = externalLabelSetName;
        if (StringUtils.isBlank(setName) || setName.equals(SystemConfig.DEFAULTCURATIONLABELSET)) {
            if (this.getOwner() != null) {
                setName = this.getOwner().getEffectiveCurationLabelSetName();
            } else {
                setName = SystemConfig.CURATIONLABELSDISABLED;
            }
        }
        return setName;
    }

    public String getCurationLabelSetName() {
        if (externalLabelSetName == null) {
            return SystemConfig.DEFAULTCURATIONLABELSET;
        }
        return externalLabelSetName;
    }

    public void setCurationLabelSetName(String setName) {
        this.externalLabelSetName = setName;
    }
    
    /**
     * Should only be used in constructors for DvObjectContainers (Datasets and 
     * Collections), to make sure new entries are created and persisted in the 
     * database StorageUse table for every DvObject container we create.
     * @param storageUse 
     */
    public void setStorageUse(StorageUse storageUse) {
        this.storageUse = storageUse;
    }

    
    /* Dataverse collections and dataset can be configured to use different PidProviders as PID generators for contained objects (datasets or data files). 
     * This mechanism is similar to others except that the stored value is a JSON object defining the protocol, authority, shoulder, and, optionally, the separator for the PidProvider. 
     */
        
    public String getPidGeneratorSpecs() {
        return pidGeneratorSpecs;
    }

    public void setPidGeneratorSpecs(String pidGeneratorSpecs) {
        this.pidGeneratorSpecs = pidGeneratorSpecs;
    }

    // Used in JSF when selecting the PidGenerator
    // It only returns an id if this dvObjectContainer has PidGenerator specs set on it, otherwise it returns "default" 
    public String getPidGeneratorId() {
        if (StringUtils.isBlank(getPidGeneratorSpecs())) {
            return "default";
        } else {
            return getEffectivePidGenerator().getId();
        }
    }
   
    //Used in JSF when setting the PidGenerator
    public void setPidGeneratorId(String pidGeneratorId) {
        // Note that the "default" provider will not be found so will result in
        // setPidGenerator(null), which unsets the pidGenerator/Specs as desired
        setPidGenerator(PidUtil.getPidProvider(pidGeneratorId));
    }

    public void setPidGenerator(PidProvider pidGenerator) {
        this.pidGenerator = pidGenerator;
        if (pidGenerator != null) {
            JsonObjectBuilder job = jakarta.json.Json.createObjectBuilder();
            this.pidGeneratorSpecs = job.add("protocol", pidGenerator.getProtocol())
                    .add("authority", pidGenerator.getAuthority()).add("shoulder", pidGenerator.getShoulder())
                    .add("separator", pidGenerator.getSeparator()).build().toString();
        } else {
            this.pidGeneratorSpecs = null;
        }
    }

    public PidProvider getEffectivePidGenerator() {
        if (pidGenerator == null) {
            String specs = getPidGeneratorSpecs();
            if (StringUtils.isBlank(specs)) {
                GlobalId pid = getGlobalId();
                if ((pid != null) && PidUtil.getPidProvider(pid.getProviderId()).canCreatePidsLike(pid)) {
                    pidGenerator = PidUtil.getPidProvider(pid.getProviderId());
                } else {
                    if (getOwner() != null) {
                        pidGenerator = getOwner().getEffectivePidGenerator();
                    }
                }
            } else {
                JsonObject providerSpecs = JsonUtil.getJsonObject(specs);
                if (providerSpecs.containsKey("separator")) {
                    pidGenerator = PidUtil.getPidProvider(providerSpecs.getString("protocol"),
                            providerSpecs.getString("authority"), providerSpecs.getString("shoulder"),
                            providerSpecs.getString("separator"));
                } else {
                    pidGenerator = PidUtil.getPidProvider(providerSpecs.getString("protocol"),
                            providerSpecs.getString("authority"), providerSpecs.getString("shoulder"));
                }
            }
            if(pidGenerator!=null && pidGenerator.canManagePID()) {
                setPidGenerator(pidGenerator);
            } else {
                setPidGenerator(null);
            }
        }
        return pidGenerator;
    }
    public Integer getDatasetFileCountLimit() {
        return datasetFileCountLimit;
    }
    public void setDatasetFileCountLimit(Integer datasetFileCountLimit) {
        // Store as -1 if missing or invalid
        this.datasetFileCountLimit = datasetFileCountLimit != null && datasetFileCountLimit <= 0 ? Integer.valueOf(-1) : datasetFileCountLimit;
    }

    public Integer getEffectiveDatasetFileCountLimit() {
        if (isDatasetFileCountLimitNotSet(getDatasetFileCountLimit()) && getOwner() != null) {
            return getOwner().getEffectiveDatasetFileCountLimit();
        } else if (isDatasetFileCountLimitNotSet(getDatasetFileCountLimit())) {
                Optional<Integer> opt = JvmSettings.DEFAULT_DATASET_FILE_COUNT_LIMIT.lookupOptional(Integer.class);
                return (opt.isPresent()) ? opt.get() : null;
        }
        return getDatasetFileCountLimit();
    }
    public boolean isDatasetFileCountLimitNotSet(Integer datasetFileCountLimit) {
        return datasetFileCountLimit == null || datasetFileCountLimit <= 0 ? true : false;
    }
}
