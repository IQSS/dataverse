package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.storageuse.StorageUse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.CascadeType;
import java.util.Optional;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
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
   
    @OneToOne(mappedBy = "dvObjectContainer",cascade={ CascadeType.REMOVE, CascadeType.PERSIST}, orphanRemoval=true)
    private StorageUse storageUse;
    
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
}
