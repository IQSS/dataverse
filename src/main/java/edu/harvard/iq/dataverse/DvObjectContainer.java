package edu.harvard.iq.dataverse;

import javax.persistence.Entity;

/**
 * A {@link DvObject} that can contain other {@link DvObject}s.
 * 
 * @author michael
 */
@Entity
public abstract class DvObjectContainer extends DvObject {
    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    protected boolean permissionRoot;
	
	
    public void setOwner(Dataverse owner) {
        super.setOwner(owner);
    }
	
	@Override
	public Dataverse getOwner() {
		return super.getOwner()!=null ? (Dataverse)super.getOwner() : null;
	}

    @Override
    public boolean isPermissionRoot() {
        return permissionRoot;
    }

    public void setPermissionRoot(boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

}
