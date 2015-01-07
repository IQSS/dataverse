package edu.harvard.iq.dataverse.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ellenk
 */
public class DataVariableDTO {
    private String name;
    private String label;
    private boolean weighted;
    private Long fileStartPosition;
    private Long fileEndPosition;
    private String formatSchema;
    private String formatSchemaName;
    private String variableIntervalType;  // use an enum for this?
    private String variableFormatType;  // use an enum for this?
    private Long recordSegmentNumber;
    private List<BigDecimal> variableRangeItems;
    private Map<String, String> summaryStatistics;  // key = summaryStatisticType, value = statistic value
    private String unf;
    private List<VariableCategoryDTO> variableCategories;
    private boolean orderedFactor;
    private String universe;
    private int fileOrder;
    private String formatCategory;
    private Long numberOfDecimalPoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isWeighted() {
        return weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public Long getFileStartPosition() {
        return fileStartPosition;
    }

    public void setFileStartPosition(Long fileStartPosition) {
        this.fileStartPosition = fileStartPosition;
    }

    public Long getFileEndPosition() {
        return fileEndPosition;
    }

    public void setFileEndPosition(Long fileEndPosition) {
        this.fileEndPosition = fileEndPosition;
    }

    public String getFormatSchema() {
        return formatSchema;
    }

    public void setFormatSchema(String formatSchema) {
        this.formatSchema = formatSchema;
    }

    public String getFormatSchemaName() {
        return formatSchemaName;
    }

    public void setFormatSchemaName(String formatSchemaName) {
        this.formatSchemaName = formatSchemaName;
    }

    public String getVariableIntervalType() {
        return variableIntervalType;
    }

    public void setVariableIntervalType(String variableIntervalType) {
        this.variableIntervalType = variableIntervalType;
    }

    public String getVariableFormatType() {
        return variableFormatType;
    }

    public void setVariableFormatType(String variableFormatType) {
        this.variableFormatType = variableFormatType;
    }

    public Long getRecordSegmentNumber() {
        return recordSegmentNumber;
    }

    public void setRecordSegmentNumber(Long recordSegmentNumber) {
        this.recordSegmentNumber = recordSegmentNumber;
    }

    public List<BigDecimal> getVariableRangeItems() {
        return variableRangeItems;
    }

    public void setVariableRangeItems(List<BigDecimal> variableRangeItems) {
        this.variableRangeItems = variableRangeItems;
    }

    public Map<String, String> getSummaryStatistics() {
        return summaryStatistics;
    }

    public void setSummaryStatistics(Map<String, String> summaryStatistics) {
        this.summaryStatistics = summaryStatistics;
    }

    public String getUnf() {
        return unf;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public List<VariableCategoryDTO> getVariableCategories() {
        return variableCategories;
    }

    public void setVariableCategories(List<VariableCategoryDTO> variableCategories) {
        this.variableCategories = variableCategories;
    }

    public boolean isOrderedFactor() {
        return orderedFactor;
    }

    public void setOrderedFactor(boolean orderedFactor) {
        this.orderedFactor = orderedFactor;
    }

    public String getUniverse() {
        return universe;
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
    
    
}
