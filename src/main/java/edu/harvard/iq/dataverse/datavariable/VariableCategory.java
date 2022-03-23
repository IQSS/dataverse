/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import edu.harvard.iq.dataverse.util.AlphaNumericComparator;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 *
 * @author Ellen Kraffmiller
 * @author Leonid Andreev
 *    
 * Largely based on the VariableCategory entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */
@Entity
@Table(indexes = {@Index(columnList="datavariable_id")})
public class VariableCategory  implements Comparable, Serializable {
    /*
     * Simple constructor: 
     */
    public VariableCategory() {
    }
    
    private static AlphaNumericComparator alphaNumericComparator = new AlphaNumericComparator();
    
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
     * Category Value: 
     */
    private String value;

    /*
     * Category Label:  
     */
    private String label;
    
    /*
     * Is this a missing category?
     */
    private boolean missing;
    
    /*
     * If this is an "Ordered Categorical Variable", aka an "Ordinal", it 
     * has an explicitly assigned order value:
     */
    private int catOrder;
    
    /*
     * Frequency of this category:
     */
    private Double frequency;
    
    
    /*
     * Getter and Setter methods:
     */
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public DataVariable getDataVariable() {
        return this.dataVariable;
    }

    public void setDataVariable(DataVariable dataVariable) {
        this.dataVariable = dataVariable;
    }
    
    public boolean isMissing() {
        return this.missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }
    
    public int getOrder() {
        return catOrder; 
    }
    
    public void setOrder(int order) {
        this.catOrder = order; 
    }
    
    public Double getFrequency() {
        return this.frequency;
    }
    
    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    
    /* 
     * Helper methods: 
     */
    
    
    // helper for html display  
    // [TODO: double-check if we still need this method in 4.0; -- L.A., jan. 2014] 
    private transient List charList;

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
        if (!(object instanceof VariableCategory)) {
            return false;
        }
        
        // TODO: 
        // We should probably compare the values instead, similarly 
        // to comareTo() below. -- L.A., Jan. 2014
        VariableCategory other = (VariableCategory)object;
        if (this.id != other.id) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }                    
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.VariableCategory[ value=" + value + " ]";
    }
    
    @Override
    public int compareTo(Object obj) {
        VariableCategory ss = (VariableCategory)obj;     
        return alphaNumericComparator.compare(this.getValue(),ss.getValue());
        
    }
    
}
