package edu.harvard.iq.dataverse;

import javax.persistence.Entity;

/**
 * A {@link DvObject} that can contain other {@link DvObject}s.
 * 
 * @author michael
 */
@Entity
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

}
