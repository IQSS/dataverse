package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.persistence.MappedSuperclass;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link DvObject} that can contain other {@link DvObject}s.
 * 
 * @author michael
 */
@MappedSuperclass
public abstract class DvObjectContainer extends DvObject {
	
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
    

    /* Dataverse collections can be configured to allow use of Curation labels and have this inheritable value to decide which set of labels to use.
     * This mechanism is similar to that for the storageDriver except that there is an addition option to disable use of labels. 
     */
    private String curationLabelSetName = null;

    public String getEffectiveCurationLabelSetName() {
        String setName = curationLabelSetName;
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
        if (curationLabelSetName == null) {
            return SystemConfig.DEFAULTCURATIONLABELSET;
        }
        return curationLabelSetName;
    }

    public void setCurationLabelSetName(String setName) {
        this.curationLabelSetName = setName;
    }

}
