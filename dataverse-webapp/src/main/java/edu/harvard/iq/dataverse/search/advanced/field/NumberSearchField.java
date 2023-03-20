package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import org.apache.commons.lang3.StringUtils;

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

    @Override
    public QueryPart getQueryPart() {
        return StringUtils.isNotBlank(minimum) || StringUtils.isNotBlank(maximum)
                ? new QueryPart(QueryPartType.QUERY,
                    String.format("%s:[%s TO %s]", getName(),
                        StringUtils.isBlank(minimum) ? "*" : minimum,
                        StringUtils.isBlank(maximum) ? "*" : maximum))
                : QueryPart.EMPTY;
    }

    // -------------------- SETTERS --------------------

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }
}
