package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class CitationData {

    private boolean direct;

    private List<String> authors = new ArrayList<>();
    private List<String> producers = new ArrayList<>();
    private String title;
    private String fileTitle = null;
    private String year;
    private Date date;
    private GlobalId persistentId;
    private String version;
    private String UNF = null;
    private String publisher;
    private List<String> funders = new ArrayList<>();
    private String seriesTitle;
    private String description;
    private List<String> datesOfCollection = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<String> kindsOfData = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private List<String> spatialCoverages = new ArrayList<>();

    private List<DatasetField> optionalValues = new ArrayList<>();
    private int optionalURLcount = 0;

    // -------------------- GETTERS --------------------

    public boolean isDirect() {
        return direct;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getProducers() {
        return producers;
    }

    public String getTitle() {
        return title;
    }

    public String getFileTitle() {
        return fileTitle;
    }

    public String getYear() {
        return year;
    }

    public Date getDate() {
        return date;
    }

    public GlobalId getPersistentId() {
        return persistentId;
    }

    public String getVersion() {
        return version;
    }

    public String getUNF() {
        return UNF;
    }

    public String getPublisher() {
        return publisher;
    }

    public List<String> getFunders() {
        return funders;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDatesOfCollection() {
        return datesOfCollection;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getKindsOfData() {
        return kindsOfData;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public List<String> getSpatialCoverages() {
        return spatialCoverages;
    }

    public List<DatasetField> getOptionalValues() {
        return optionalValues;
    }

    public int getOptionalURLcount() {
        return optionalURLcount;
    }

    // -------------------- LOGIC --------------------

    public String getAuthorsString() {
        return String.join("; ", authors);
    }

    public Map<String, String> getDataCiteMetadata() {
        String authorString = isNotEmpty(getAuthorsString())
                ? getAuthorsString() : ":unav";

        String producerString = isNotEmpty(getPublisher())
                ? getPublisher() : ":unav";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", getTitle());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", getYear());
        return metadata;
    }

    // -------------------- SETTERS --------------------

    CitationData setDirect(boolean direct) {
        this.direct = direct;
        return this;
    }

    CitationData setTitle(String title) {
        this.title = title;
        return this;
    }

    CitationData setFileTitle(String fileTitle) {
        this.fileTitle = fileTitle;
        return this;
    }

    CitationData setYear(String year) {
        this.year = year;
        return this;
    }

    CitationData setDate(Date date) {
        this.date = date;
        return this;
    }

    CitationData setPersistentId(GlobalId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    CitationData setVersion(String version) {
        this.version = version;
        return this;
    }

    CitationData setUNF(String UNF) {
        this.UNF = UNF;
        return this;
    }

    CitationData setPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    CitationData setFunders(List<String> funders) {
        this.funders = funders;
        return this;
    }

    CitationData setSeriesTitle(String seriesTitle) {
        this.seriesTitle = seriesTitle;
        return this;
    }

    CitationData setDescription(String description) {
        this.description = description;
        return this;
    }

    CitationData setOptionalURLcount(int optionalURLcount) {
        this.optionalURLcount = optionalURLcount;
        return this;
    }
}
