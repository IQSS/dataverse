/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.validator.constraints.NotBlank;
import edu.harvard.iq.dataverse.DataTable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 *
 * @author Leonid Andreev
 * 
 * Largely based on the the DataVariable entity from the DVN v2-3;
 * original author: Ellen Kraffmiller.
 *
 */

@Entity
@Table(indexes = {@Index(columnList="datatable_id")})
public class DataVariable implements Serializable {
    
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
    @Column(columnDefinition="TEXT")
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
    

    public enum VariableInterval { DISCRETE, CONTINUOUS, NOMINAL, DICHOTOMOUS }; // former VariableIntervalType
    /*
     * Interval: <FINALIZED>
     * former VariableIntervalType
     */
    //@ManyToOne
    private VariableInterval interval;

    
    public enum VariableType { NUMERIC, CHARACTER }; // former VariableFormatType

    /*
     * Type: <FINALIZED>
     * former VariableFormatType
     */
    //@ManyToOne
    //@JoinColumn(nullable=false)
    private VariableType type;

    /*
     * formatSchema: <FINALIZED, DROPPED>
     * Used for the original format - i.e. RData, SPSS, etc. (??)
     */
    //experimentprivate String formatSchema;

    /*
     * format: <FINALIZED>
     * used for format strings - such as "%D-%Y-%M" for date values, etc. 
     * former formatSchemaName
     */
    private String format;

    /*
     * formatCategory: 
     * <FINALIZED>
     * left as is. 
     * TODO: (?) consider replacing with an enum (?)
     * Used for "time", "date", etc.
     */
    private String formatCategory;
    
    /*
     * recordSegmentNumber: this property is specific to fixed-width data 
     * files.
     */
    private Long recordSegmentNumber;

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
    private String unf = "UNF:pending";
    
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

    /**
     * On ingest, we set "factor" to true only if the format is RData and the
     * variable is a factor in R. See also "orderedFactor" above.
     */
    private boolean factor;

  
    
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
     * number of decimal points, where applicable.
     */
    private Long numberOfDecimalPoints;

    @OneToMany (mappedBy="dataVariable", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private Collection<VariableMetadata> variableMetadatas;
    
    public DataVariable() {
    }
    
    /** Creates a new instance of DataVariable
     * @param order
     * @param table */
    public DataVariable(int order, DataTable table) {
        this.fileOrder = order;
        dataTable =table;
        invalidRanges = new ArrayList<>();
        summaryStatistics=new ArrayList<>();
        categories = new ArrayList<>();
        variableMetadatas = new ArrayList<>();
    }
    
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

    //experimentpublic String getFormatSchema() {
    //    return this.formatSchema;
    //}
    //
    //public void setFormatSchema(String formatSchema) {
    //    this.formatSchema = formatSchema;
    //}

    public String getFormat() {
        return this.format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public VariableInterval getInterval() {
        return this.interval;
    }
    
    public void setInterval(VariableInterval interval) {
        this.interval = interval;
    }

    // Methods for obtaining interval types as strings (labels), 
    // used in the DDI:
    
    public String getIntervalLabel() {
        if (isIntervalDiscrete()) {
            return "discrete";
        }
        if (isIntervalContinuous()) {
            return "contin";
        }
        if (isIntervalNominal()) {
            return "nominal";
        }
        if (isIntervalDichotomous()) {
            return "dichotomous";
        }
        return null; 
    }
    
    public void setIntervalDiscrete() {
        this.interval = VariableInterval.DISCRETE;
    }
    
    public void setIntervalContinuous() {
        this.interval = VariableInterval.CONTINUOUS;
    }
    
    public void setIntervalNominal() {
        this.interval = VariableInterval.NOMINAL;
    }
    
    public void setIntervalDichotomous() {
        this.interval = VariableInterval.DICHOTOMOUS;
    }
    
    public boolean isIntervalDiscrete() {
        return this.interval == VariableInterval.DISCRETE;
    }
    
    public boolean isIntervalContinuous() {
        return this.interval == VariableInterval.CONTINUOUS;
    }
    
    public boolean isIntervalNominal() {
        return this.interval == VariableInterval.NOMINAL;
    }
    
    public boolean isIntervalDichotomous() {
        return this.interval == VariableInterval.DICHOTOMOUS;
    }
    
    public VariableType getType() {
        return this.type;
    }
    
    public void setType(VariableType type) {
        this.type = type;
    }
    
    public void setTypeNumeric() {
        this.type = VariableType.NUMERIC;
    }
    
    public void setTypeCharacter() {
        this.type = VariableType.CHARACTER;
    }
    
    public boolean isTypeNumeric() {
        return this.type == VariableType.NUMERIC;
    }
    
    public boolean isTypeCharacter() {
        return this.type == VariableType.CHARACTER;
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
    
    public void setCategories(List<VariableCategory> categories) {
        this.categories = categories;
    }

    /**
     * In the future, when users can edit variable metadata, they may be able to
     * specify that their variable is categorical. At that point, add `&& isFactor`.
     */
    public boolean isCategorical () {
        return (categories != null && categories.size() > 0);
    }
    
    // Only R supports the concept of ordered categorical.
    public boolean isOrderedCategorical () {
        return isCategorical() && orderedFactor; 
    }
    
    public void setOrderedCategorical (boolean ordered) {
        orderedFactor = ordered; 
    }

    public boolean isFactor() {
        return factor;
    }

    public void setFactor(boolean factor) {
        this.factor = factor;
    }

    /* getter and setter for weightedVariables - not yet implemented!
    public java.util.Collection<WeightedVarRelationship> getWeightedVariables() {
        return this.weightedVariables;
    }

    public void setWeightedVariables(java.util.Collection<edu.harvard.iq.dvn.core.study.WeightedVarRelationship> weightedVariables) {
        this.weightedVariables = weightedVariables;
    }
    */
    
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

    public void setVariableMetadatas(List<VariableMetadata> variableMetadatas) {
        this.variableMetadatas = variableMetadatas;
    }

    public Collection<VariableMetadata> getVariableMetadatas() {
        return variableMetadatas;
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
