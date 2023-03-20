package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import org.apache.commons.lang3.StringUtils;

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

    @Override
    public QueryPart getQueryPart() {
        return StringUtils.isNotEmpty(lowerLimit) || StringUtils.isNotEmpty(upperLimit)
                ? new QueryPart(QueryPartType.QUERY,
                    String.format("%s:[%s TO %s]", getName(),
                        StringUtils.isEmpty(lowerLimit) ? "*" : lowerLimit,
                        StringUtils.isEmpty(upperLimit) ? "*" : upperLimit))
                : QueryPart.EMPTY;
    }

// -------------------- SETTERS --------------------

    public void setLowerLimit(String lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public void setUpperLimit(String upperLimit) {
        this.upperLimit = upperLimit;
    }

}
