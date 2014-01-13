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

/*
 *
 * @author Leonid Andreev
 *    
 * Largely based on the SummaryStatistic entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 * 
 */

@Entity
public class SummaryStatistic implements Serializable {
    /*
     * Simple constructor: 
     */
    public SummaryStatistic() {
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
     * type of this Summary Statistic value (for ex., "median", "mean", etc.)
     * Note that SummaryStatisticType is itself an entity. 
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private SummaryStatisticType type;

    
    /*
     * value: string representation of this Summary Statistic value. 
     */
    private String value;

    
    /*
     * Getter and Setter methods:
     */
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public SummaryStatisticType getType() {
        return this.type;
    }
    
    public void setType(SummaryStatisticType type) {
        this.type = type;
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
        if (!(object instanceof SummaryStatistic)) {
            return false;
        }
        
        SummaryStatistic other = (SummaryStatistic)object;
        if (this.id != other.id) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }                    
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.SummaryStatistic[ value=" + value + " ]";
    }
    
}
