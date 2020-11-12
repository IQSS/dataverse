package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.persistence.DvObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Event that should be fired when permissions on {@link DvObject}s
 * are changed.
 * 
 * @author madryk
 */
public class PermissionReindexEvent implements Serializable {

    private static final long serialVersionUID = -7582055912874892985L;
    
    private List<DvObject> dvObjects;

    // -------------------- CONSTRUCTORS --------------------

    public PermissionReindexEvent(DvObject... dvObjects) {
        this.dvObjects = Arrays.asList(dvObjects);
    }

    public PermissionReindexEvent(List<DvObject> dvObjects) {
        this.dvObjects = dvObjects;
    }

    // -------------------- GETTERS --------------------

    public List<DvObject> getDvObjects() {
        return dvObjects;
    }
}
