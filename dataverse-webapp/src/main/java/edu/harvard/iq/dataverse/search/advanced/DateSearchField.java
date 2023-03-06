package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * Class responsible for holding field value represented as Date String.
 */
public class DateSearchField extends SearchField {

    private String lowerLimit;
    private String upperLimit;

    // -------------------- CONSTRUCTORS --------------------

    public DateSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.DATE, datasetFieldType);
    }
	// -------------------- GETTERS --------------------

    public String getLowerLimit() {
        return lowerLimit;
    }

    public String getUpperLimit() {
        return upperLimit;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<String> getValidatableValues() {
        return Arrays.asList(lowerLimit, upperLimit);
    }

    // -------------------- SETTERS --------------------

    public void setLowerLimit(String lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public void setUpperLimit(String upperLimit) {
        this.upperLimit = upperLimit;
    }

}
