/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.validator.constraints.NotBlank;
import edu.harvard.iq.dataverse.DataTable;
import javax.persistence.OrderBy;

/**
 *
 * @author Leonid Andreev
 * 
 * Largely based on the the DataVariable entity from the DVN v2-3;
 * original author: Ellen Kraffmiller.
 *
 */

@Entity
public class DataVariable implements Serializable {
    
    /** Creates a new instance of DataVariable */
    public DataVariable() {
    }
    
    /*
     * Class property definitions: 
     */
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /*
     * dataTable: DataTable to which this variable belongs.
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataTable dataTable;

    /*
     * name: Name of the Variable
     */
    @NotBlank
    private String name;

    /*
     * label: Variable Label
     */
    private String label;
    
    /*
     * weighted: indicates if this variable is weighted.
     */
    private boolean weighted;
    
    /*
     * fileStartPosition: this property is specific to fixed-width data; 
     * this is a byte offset where the data column begins.
     */
    private Long fileStartPosition;

    /*
     * fileEndPosition: similarly, byte offset where the variable column 
     * ends in the fixed-width data file.
     */
    private java.lang.Long fileEndPosition;

    /*
     * formatSchema: <REVIEW; ADD COMMENT>
     */
    private String formatSchema;

    /*
     * formatSchemaName: <REVIEW; ADD COMMENT>
     */
    private String formatSchemaName;

    /*
     * variableIntervalType: <REVIEW; ADD COMMENT>
     * Note that VariableIntervalType is itself an entity [is it necessary?]
     */
    @ManyToOne
    private VariableIntervalType variableIntervalType;

    /*
     * variableFormatType: <REVIEW; ADD COMMENT>
     * Note that VariableFormatType is itself an entity [is it necessary?]
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private VariableFormatType variableFormatType;

    /*
     * recordSegmentNumber: this property is specific to fixed-width data 
     * files.
     */
    private java.lang.Long recordSegmentNumber;

    /*
     * invalidRanges: value ranges that are defined as "invalid" for this
     * variable. 
     * Note that VariableRange is itself an entity.
     */
    @OneToMany (mappedBy="dataVariable", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<VariableRange> invalidRanges;
    
    /*
     * invalidRangeItems: a collection of individual value range items defined 
     * as "invalid" for this variable.
     * Note that VariableRangeItem is itself an entity. 
     */
     @OneToMany (mappedBy="dataVariable", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<VariableRangeItem> invalidRangeItems;
      
    /*
     * Summary Statistics for this variable.
     * Note that SummaryStatistic is itself an entity.
     */
    @OneToMany (mappedBy="dataVariable", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private Collection<SummaryStatistic> summaryStatistics;
    
    /*
     * unf: printable representation of the UNF, Universal Numeric Fingerprint
     * of this variable.
     */
    private String unf;
    
    /*
     * Variable Categories, for categorical variables.
     * VariableCategory is itself an entity. 
     */
    @OneToMany (mappedBy="dataVariable", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    @OrderBy("catOrder")
    private Collection<VariableCategory> categories;
    
    /*
     * The boolean "ordered": identifies ordered categorical variables ("ordinals"). 
     */
    private boolean orderedFactor = false; 
    
    /*
     * the "Universe" of the variable. (see the DDI documentation for the 
     * explanation)
     */
    private String universe;

  
    
    /*
     * weightedVariables: <NOT YET IMPLEMENTED!>
     * Note that WeightedVarRelationship is a custom entity, with a custom
     * @IdClass.
     */
    /*
        @OneToMany (mappedBy="dataVariable")
        private java.util.Collection<WeightedVarRelationship> weightedVariables;
    */
    
    /* 
     * fileOrder: the numeric order in which this variable occurs in the 
     * physical file. 
     */
    private int fileOrder;
    
    /*
     * formatCategory: name of the Format Category of this variable
     * <TODO: REVIEW; ADD COMMENT>
     */
    private String formatCategory;

    /*
     * number of decimal points, where applicable.
     */
    private Long numberOfDecimalPoints;

    
    /*
     * Getter and Setter functions: 
     */
    
     
    public DataTable getDataTable() {
        return this.dataTable;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }
    
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isWeighted() {
        return this.weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    
    public java.lang.Long getFileStartPosition() {
        return this.fileStartPosition;
    }

    public void setFileStartPosition(java.lang.Long fileStartPosition) {
        this.fileStartPosition = fileStartPosition;
    }

    public java.lang.Long getFileEndPosition() {
        return this.fileEndPosition;
    }
    
    public void setFileEndPosition(java.lang.Long fileEndPosition) {
        this.fileEndPosition = fileEndPosition;
    }

    public String getFormatSchema() {
        return this.formatSchema;
    }
    
    public void setFormatSchema(String formatSchema) {
        this.formatSchema = formatSchema;
    }

    public String getFormatSchemaName() {
        return this.formatSchemaName;
    }
    
    public void setFormatSchemaName(String formatSchemaName) {
        this.formatSchemaName = formatSchemaName;
    }
    
    public VariableIntervalType getVariableIntervalType() {
        return this.variableIntervalType;
    }
    
    public void setVariableIntervalType(VariableIntervalType variableIntervalType) {
        this.variableIntervalType = variableIntervalType;
    }

    public VariableFormatType getVariableFormatType() {
        return this.variableFormatType;
    }
    
    public void setVariableFormatType(VariableFormatType variableFormatType) {
        this.variableFormatType = variableFormatType;
    }

    public java.lang.Long getRecordSegmentNumber() {
        return this.recordSegmentNumber;
    }

    
    public void setRecordSegmentNumber(java.lang.Long recordSegmentNumber) {
        this.recordSegmentNumber = recordSegmentNumber;
    }
    
    public Collection<VariableRange> getInvalidRanges() {
        return this.invalidRanges;
    }
    
    public void setInvalidRanges(Collection<VariableRange> invalidRanges) {
        this.invalidRanges = invalidRanges;
    }


    public Collection<VariableRangeItem> getInvalidRangeItems() {
        return this.invalidRangeItems;
    }
    
    public void setInvalidRangeItems(java.util.Collection<VariableRangeItem> invalidRangeItems) {
        this.invalidRangeItems = invalidRangeItems;
    }
    
    public Collection<SummaryStatistic> getSummaryStatistics() {
        return this.summaryStatistics;
    }
    
    public void setSummaryStatistics(Collection<SummaryStatistic> summaryStatistics) {
        this.summaryStatistics = summaryStatistics;
    }
    
    public String getUnf() {
        return this.unf;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }
    
    public Collection<VariableCategory> getCategories() {
        return this.categories;
    }
    
    public void setCategories(Collection<VariableCategory> categories) {
        this.categories = categories;
    }

    public boolean isCategorical () {
        return (categories != null && categories.size() > 0);
    }
    
    public boolean isOrderedCategorical () {
        return isCategorical() && orderedFactor; 
    }
    
    public void setOrderedCategorical (boolean ordered) {
        orderedFactor = ordered; 
    }
    
    /* getter and setter for weightedVariables - not yet implemented!
    public java.util.Collection<WeightedVarRelationship> getWeightedVariables() {
        return this.weightedVariables;
    }

    public void setWeightedVariables(java.util.Collection<edu.harvard.iq.dvn.core.study.WeightedVarRelationship> weightedVariables) {
        this.weightedVariables = weightedVariables;
    }
    */
    
    public String getUniverse() {
        return this.universe;
    }
    
    public void setUniverse(String universe) {
        this.universe = universe;
    }
    
    public int getFileOrder() {
        return fileOrder;
    }

    public void setFileOrder(int fileOrder) {
        this.fileOrder = fileOrder;
    }
    
     public String getFormatCategory() {
        return formatCategory;
    }

    public void setFormatCategory(String formatCategory) {
        this.formatCategory = formatCategory;
    }
    
    public Long getNumberOfDecimalPoints() {
        return numberOfDecimalPoints;
    }

    public void setNumberOfDecimalPoints(Long numberOfDecimalPoints) {
        this.numberOfDecimalPoints = numberOfDecimalPoints;
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
        if (!(object instanceof DataVariable)) {
            return false;
        }
        DataVariable other = (DataVariable)object;
        if (this.id != other.id ) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataVariable[ id=" + id + " ]";
    }
    
    
}
