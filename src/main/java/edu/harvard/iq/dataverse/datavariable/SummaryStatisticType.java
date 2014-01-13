/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author Leonid Andreev
 *     
 * Largely based on the SummaryStatisticType entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 * There's not much to this entity: 
 * in addition to the EJB/db id, SummaryStatisticType has one property: name 
 * that defines this type of Summary Statistics. For example: "mean", "median", 
 * etc. The purpose of this entity is to keep these defined names in the 
 * database. 
 * 
 */

@Entity
public class SummaryStatisticType implements Serializable {
    /*
     * Simple constructor: 
     */
    public SummaryStatisticType() {
    }
    
    /*
     * Definitions of class properties: 
     */
   
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /*
     * name: self-explanatory
     */
    private String name;

    
    
    /*
     * Getter and Setter methods:
     */
    
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
    /* 
     * Custom overrides for hashCode(), equals() and toString() methods:
     */
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SummaryStatisticType)) {
            return false;
        }
        SummaryStatisticType other = (SummaryStatisticType)object;
        if (this.id != other.id) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }                    
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.SummaryStatisticType[ id=" + id + " ]";
    }
}