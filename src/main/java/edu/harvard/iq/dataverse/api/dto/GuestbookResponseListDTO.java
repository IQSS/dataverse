package edu.harvard.iq.dataverse.api.dto;

import java.util.Date;

public class GuestbookResponseListDTO {
    private final Long id;
    private final String dataset;
    private final String name;
    private final String institution;
    private final String position;
    private final String type;
    private final Date date;
    private final String file;
    private final String responses;

    // Must match the column order in your @ConstructorResult mapping
    public GuestbookResponseListDTO(Long id, String dataset, String name, String institution, String position, String type, Date date, String file, String responses) {
        this.id = id;
        this.dataset = dataset;
        this.name = name;
        this.institution = institution;
        this.position = position;
        this.type = type;
        this.date = date;
        this.file = file;
        this.responses = responses;
    }

    public Long getId() {
        return id;
    }
    public String getDataset() {
        return dataset;
    }
    public String getName() {
        return name;
    }
    public String getInstitution() {
        return institution;
    }
    public String getPosition() {
        return position;
    }
    public String getType() {
        return type;
    }
    public Date getDate() {
        return date;
    }
    public String getFile() {
        return file;
    }
    public String getResponses() {
        return responses;
    }

    @Override
    public String toString() {
        return "id: " + id + ", dataset: " + dataset + ", name: " + name + ", type: " + type + ", date: " + date  + ", file: " + file +
                responses != null && !responses.isEmpty() ? ", responses: " + responses : "";
    }
}
