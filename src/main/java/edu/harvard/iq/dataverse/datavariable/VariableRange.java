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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author Leonid Andreev
 *    
 * Largely based on the VariableRange entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */
@Entity
public class VariableRange implements Serializable {
    
    /*
     * Simple constructor: 
     */
    public VariableRange() {
    }
    
    /*
     * Definitions of class properties: 
     */
   
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /*
     * DataVariable for which this range is defined.
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataVariable dataVariable;
    
    /*
     * beginValue: represents the beginning of the range.
     */
    private String beginValue;

    /*
     * endValue: represents the end of the range.
     */
    private String endValue;

    /*
     * beginValueType: type of the value that opens the range.
     * Note that VariableRangeType is itself an entity. [IS THIS NECESSARY?]
     */
    
    @ManyToOne
    private VariableRangeType beginValueType;
    
    /*
     * endValueType: type of the value that closes the range.
     * Note that VariableRangeType is itself an entity. [IS THIS NECESSARY?]
     */
    @ManyToOne
    private VariableRangeType endValueType;

    
    /*
     * Getter and Setter methods:
     */
    
    
    public String getBeginValue() {
        return this.beginValue;
    }
    
    public void setBeginValue(String beginValue) {
        this.beginValue = beginValue;
    }
    
    public String getEndValue() {
        return this.endValue;
    }
    
    public void setEndValue(String endValue) {
        this.endValue = endValue;
    }

    public VariableRangeType getBeginValueType() {
        return this.beginValueType;
    }
    
    public void setBeginValueType(VariableRangeType beginValueType) {
        this.beginValueType = beginValueType;
    }
    
    public VariableRangeType getEndValueType() {
        return this.endValueType;
    }
    
    public void setEndValueType(VariableRangeType endValueType) {
        this.endValueType = endValueType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
 
    public DataVariable getDataVariable() {
        return this.dataVariable;
    }

    public void setDataVariable(DataVariable dataVariable) {
        this.dataVariable = dataVariable;
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
        if (!(object instanceof VariableRange)) {
            return false;
        }
        VariableRange other = (VariableRange)object;
        if (this.id != other.id) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }                    
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.VariableRange[ id=" + id + " ]";
    }
    
}
