/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;


/**
 * This is a non persistent superclass to be used by entities in Dataverse
 * for any shared non persistent properties; for example "mergeable" which should
 * be set to false, when an entity is manually loaded through native queries
 */
public abstract class DataverseEntity {
    
    private boolean mergeable = true;

    public boolean isMergeable() {
        return mergeable;
    }

    public void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }
    

}
