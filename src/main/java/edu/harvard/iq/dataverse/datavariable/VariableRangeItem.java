/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * Largely based on the VariableRangeItem entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */

@Entity
@Table(indexes = {@Index(columnList="datavariable_id")})
public class VariableRangeItem implements Serializable {
    /*
     * Simple constructor: 
     */
    public VariableRangeItem() {
    }
    
    /*
     * Definitions of class properties: 
     */
   
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    /*
     * value: a numeric (BigDecimal) value of tis Range Item.
     */
    private BigDecimal value;

    /**
     * DataVariable for which this range item is defined.
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataVariable dataVariable;

    
    /*
     * Getter and Setter methods:
     */
    
    
    public BigDecimal getValue() {
        return this.value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
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
        if (!(object instanceof VariableRangeItem)) {
            return false;
        }
        VariableRangeItem other = (VariableRangeItem)object;
        // TODO: 
        // Should we instead check if the values of the objects equals()
        // each other? -- L.A., Jan. 2014
        if (this.id != other.id) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }                    
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.VariableRangeItem[ " + this.getValue() + " ]";
    }
}
