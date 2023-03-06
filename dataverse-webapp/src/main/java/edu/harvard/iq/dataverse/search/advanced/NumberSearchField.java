package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * Object responsible for holding numbers such as Integer or Double.
 */
public class NumberSearchField extends SearchField {

    private String minimum;
    private String maximum;

    // -------------------- CONSTRUCTORS --------------------

    public NumberSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.NUMBER, datasetFieldType);
    }

    // -------------------- GETTERS --------------------

    public String getMinimum() {
        return minimum;
    }

    public String getMaximum() {
        return maximum;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<String> getValidatableValues() {
        return Arrays.asList(minimum, maximum);
    }

    // -------------------- SETTERS --------------------

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }
}
