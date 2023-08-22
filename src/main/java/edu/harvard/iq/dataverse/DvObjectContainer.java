package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link DvObject} that can contain other {@link DvObject}s.
 * 
 * @author michael
 */
@MappedSuperclass
public abstract class DvObjectContainer extends DvObject {
	
    
    public static final String UNDEFINED_METADATA_LANGUAGE_CODE = "undefined"; //Used in dataverse.xhtml as a non-null selection option value (indicating inheriting the default)
    
    
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
                ml = UNDEFINED_METADATA_LANGUAGE_CODE;
            }
        }
        return ml;
    }
    
    public String getMetadataLanguage() {
        if (metadataLanguage == null) {
            return UNDEFINED_METADATA_LANGUAGE_CODE;
        }
        return metadataLanguage;
    }

    public void setMetadataLanguage(String ml) {
        if (ml != null && ml.equals(UNDEFINED_METADATA_LANGUAGE_CODE)) {
            this.metadataLanguage = null;
        } else {
            this.metadataLanguage = ml;
        }
    }
    
    public static boolean isMetadataLanguageSet(String mdLang) {
        return mdLang!=null && !mdLang.equals(UNDEFINED_METADATA_LANGUAGE_CODE);
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

}
