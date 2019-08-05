package edu.harvard.iq.dataverse.persistence.datafile.datavariable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author Leonid Andreev
 * <p>
 * Largely based on the VariableRangeItem entity from the DVN v2-3;
 * original author: Ellen Kraffmiller (2006).
 */

@Entity
@Table(indexes = {@Index(columnList = "datavariable_id")})
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
    @JoinColumn(nullable = false)
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
        VariableRangeItem other = (VariableRangeItem) object;
        // TODO: 
        // Should we instead check if the values of the objects equals()
        // each other? -- L.A., Jan. 2014
        if (this.id != other.id) {
            return this.id != null && this.id.equals(other.id);
        }
        return true;
    }

    @Override
    public String toString() {
        return "VariableRangeItem[ " + this.getValue() + " ]";
    }
}
