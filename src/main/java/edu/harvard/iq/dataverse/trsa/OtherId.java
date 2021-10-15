/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.trsa;

import java.util.Objects;
import java.util.logging.Logger;

/**
 *
 * @author asone
 */
public class OtherId {

    private static final Logger logger = Logger.getLogger(OtherId.class.getName());
    
    private String idAgency;

    /**
     * Get the value of idAgency
     *
     * @return the value of idAgency
     */
    public String getIdAgency() {
        return idAgency;
    }

    /**
     * Set the value of idAgency
     *
     * @param idAgency new value of idAgency
     */
    public void setIdAgency(String idAgency) {
        this.idAgency = idAgency;
    }

    
        private String IdValue;

    /**
     * Get the value of IdValue
     *
     * @return the value of IdValue
     */
    public String getIdValue() {
        return IdValue;
    }

    /**
     * Set the value of IdValue
     *
     * @param IdValue new value of IdValue
     */
    public void setIdValue(String IdValue) {
        this.IdValue = IdValue;
    }

    public OtherId(String idAgency, String IdValue) {
        this.idAgency = idAgency;
        this.IdValue = IdValue;
    }

    public OtherId() {
    }
    
    
    

    @Override
    public String toString() {
        return "OtherId{" + "idAgency=" + idAgency + ", IdValue=" + IdValue + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.idAgency);
        hash = 79 * hash + Objects.hashCode(this.IdValue);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OtherId other = (OtherId) obj;
        if (!Objects.equals(this.idAgency, other.idAgency)) {
            return false;
        }
        if (!Objects.equals(this.IdValue, other.IdValue)) {
            return false;
        }
        return true;
    }

    
    
}
