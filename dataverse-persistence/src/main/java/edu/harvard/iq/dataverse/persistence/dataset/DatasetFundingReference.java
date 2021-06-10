package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Comparator;

public class DatasetFundingReference {

    private int displayOrder;
    private DatasetField agency;
    private DatasetField agencyIdentifier;
    private DatasetField programName;
    private DatasetField programIdentifier;

    public DatasetFundingReference() {
    }

    public DatasetFundingReference(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public static Comparator<DatasetFundingReference> DisplayOrder = Comparator.comparingInt(DatasetFundingReference::getDisplayOrder);

    public int getDisplayOrder() {
        return displayOrder;
    }

    public DatasetField getAgency() {
        return agency;
    }

    public DatasetField getAgencyIdentifier() {
        return agencyIdentifier;
    }

    public DatasetField getProgramName() {
        return programName;
    }

    public DatasetField getProgramIdentifier() {
        return programIdentifier;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setAgency(DatasetField agency) {
        this.agency = agency;
    }

    public void setAgencyIdentifier(DatasetField agencyIdentifier) {
        this.agencyIdentifier = agencyIdentifier;
    }

    public void setProgramName(DatasetField programName) {
        this.programName = programName;
    }

    public void setProgramIdentifier(DatasetField programIdentifier) {
        this.programIdentifier = programIdentifier;
    }
}
