package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;

import java.util.List;

public class DataverseDTO {
    private String alias;
    private String name;
    private String description;
    private String affiliation;
    private List<DataverseContact> dataverseContacts;
    private Dataverse.DataverseType dataverseType;
    private Integer datasetFileCountLimit;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public Integer getDatasetFileCountLimit() {
        return datasetFileCountLimit;
    }

    public void setDatasetFileCountLimit(Integer datasetFileCountLimit) {
        this.datasetFileCountLimit = datasetFileCountLimit;
    }

    public List<DataverseContact> getDataverseContacts() {
        return dataverseContacts;
    }

    public void setDataverseContacts(List<DataverseContact> dataverseContacts) {
        this.dataverseContacts = dataverseContacts;
    }

    public Dataverse.DataverseType getDataverseType() {
        return dataverseType;
    }

    public void setDataverseType(Dataverse.DataverseType dataverseType) {
        this.dataverseType = dataverseType;
    }
}
