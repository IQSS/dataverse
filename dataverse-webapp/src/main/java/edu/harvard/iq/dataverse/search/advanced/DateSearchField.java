package edu.harvard.iq.dataverse.search.advanced;

/**
 * Class responsible for holding field value represented as Date String.
 */
public class DateSearchField extends SearchField {

    private String lowerLimit;
    private String upperLimit;

    public String getLowerLimit() {
		return lowerLimit;
	}

	public void setLowerLimit(String lowerLimit) {
		this.lowerLimit = lowerLimit;
	}

	public String getUpperLimit() {
		return upperLimit;
	}

	public void setUpperLimit(String upperLimit) {
		this.upperLimit = upperLimit;
	}

	public DateSearchField(String name, String displayName, String description) {
        super(name, displayName, description, SearchFieldType.DATE);
    }

}
