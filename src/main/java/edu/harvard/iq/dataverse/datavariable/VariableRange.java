/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 *    
 * Largely based on the VariableRange entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */
@Entity
@Table(indexes = {@Index(columnList="datavariable_id")})
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

    public enum VariableRangeType { MIN, MAX, MIN_EXCLUSIVE, MAX_EXCLUSIVE, POINT};
    /*
     * beginValueType: type of the value that opens the range.
     */
    
    //@ManyToOne
    private VariableRangeType beginValueType;
    
    /*
     * endValueType: type of the value that closes the range.
     */
    //@ManyToOne
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
    
    public void setBeginValueTypeMin() {
        this.beginValueType = VariableRangeType.MIN;
    }
    
    public void setBeginValueTypeMax() {
        this.beginValueType = VariableRangeType.MAX;
    }
    
    public void setBeginValueTypeMinExcl() {
        this.beginValueType = VariableRangeType.MIN_EXCLUSIVE;
    }
    
    public void setBeginValueTypeMaxExcl() {
        this.beginValueType = VariableRangeType.MAX_EXCLUSIVE;
    }
    
    public void setBeginValueTypePoint() {
        this.beginValueType = VariableRangeType.POINT;
    }
        
    public boolean isBeginValueTypeMin() {
        return this.beginValueType == VariableRangeType.MIN;
    }
    
    public boolean isBeginValueTypeMax() {
        return this.beginValueType == VariableRangeType.MAX;
    }
    
    public boolean isBeginValueTypeMinExcl() {
        return this.beginValueType == VariableRangeType.MIN_EXCLUSIVE;
    }
    
    public boolean isBeginValueTypeMaxExcl() {
        return this.beginValueType == VariableRangeType.MAX_EXCLUSIVE;
    }
    
    public boolean isBeginValueTypePoint() {
        return this.beginValueType == VariableRangeType.POINT;
    }
    
    public VariableRangeType getEndValueType() {
        return this.endValueType;
    }
      
    public void setEndValueType(VariableRangeType endValueType) {
        this.endValueType = endValueType;
    }

    public void setEndValueTypeMin() {
        this.endValueType = VariableRangeType.MIN;
    }
    
    public void setEndValueTypeMax() {
        this.endValueType = VariableRangeType.MAX;
    }
    
    public void setEndValueTypeMinExcl() {
        this.endValueType = VariableRangeType.MIN_EXCLUSIVE;
    }
    
    public void setEndValueTypeMaxExcl() {
        this.endValueType = VariableRangeType.MAX_EXCLUSIVE;
    }
    
    public void setEndValueTypePoint() {
        this.endValueType = VariableRangeType.POINT;
    }
    
    public boolean isEndValueTypeMin() {
        return this.endValueType == VariableRangeType.MIN;
    }
    
    public boolean isEndValueTypeMax() {
        return this.endValueType == VariableRangeType.MAX;
    }
    
    public boolean isEndValueTypeMinExcl() {
        return this.endValueType == VariableRangeType.MIN_EXCLUSIVE;
    }
    
    public boolean isEndValueTypeMaxExcl() {
        return this.endValueType == VariableRangeType.MAX_EXCLUSIVE;
    }
    
    public boolean isEndValueTypePoint() {
        return this.endValueType == VariableRangeType.POINT;
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
