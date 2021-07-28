package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import javax.persistence.MappedSuperclass;
import org.apache.commons.lang.StringUtils;

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
}
