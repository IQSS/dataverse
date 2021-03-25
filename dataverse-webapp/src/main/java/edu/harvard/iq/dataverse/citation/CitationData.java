package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.GlobalId;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class CitationData {

    private boolean direct;

    private List<String> authors = new ArrayList<>();
    private List<Producer> producers = new ArrayList<>();
    private List<String> distributors = new ArrayList<>();
    private String productionPlace;
    private String productionDate;
    private String rootDataverseName;
    private String title;
    private String fileTitle = null;
    private String year;
    private Date date;
    private String releaseYear;
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
    private List<String> otherIds = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public boolean isDirect() {
        return direct;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<Producer> getProducers() {
        return producers;
    }

    public List<String> getDistributors() {
        return distributors;
    }

    public String getProductionPlace() {
        return productionPlace;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public String getRootDataverseName() {
        return rootDataverseName;
    }

    public String getReleaseYear() {
        return releaseYear;
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

    public List<String> getOtherIds() {
        return otherIds;
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

    public CitationData setDirect(boolean direct) {
        this.direct = direct;
        return this;
    }

    public CitationData setProductionPlace(String productionPlace) {
        this.productionPlace = productionPlace;
        return this;
    }

    public CitationData setProductionDate(String productionDate) {
        this.productionDate = productionDate;
        return this;
    }

    public CitationData setRootDataverseName(String rootDataverseName) {
        this.rootDataverseName = rootDataverseName;
        return this;
    }

    public CitationData setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
        return this;
    }

    public CitationData setTitle(String title) {
        this.title = title;
        return this;
    }

    public CitationData setFileTitle(String fileTitle) {
        this.fileTitle = fileTitle;
        return this;
    }

    public CitationData setYear(String year) {
        this.year = year;
        return this;
    }

    public CitationData setDate(Date date) {
        this.date = date;
        return this;
    }

    public CitationData setPersistentId(GlobalId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    public CitationData setVersion(String version) {
        this.version = version;
        return this;
    }

    public CitationData setUNF(String UNF) {
        this.UNF = UNF;
        return this;
    }

    public CitationData setPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public CitationData setFunders(List<String> funders) {
        this.funders = funders;
        return this;
    }

    public CitationData setSeriesTitle(String seriesTitle) {
        this.seriesTitle = seriesTitle;
        return this;
    }

    public CitationData setDescription(String description) {
        this.description = description;
        return this;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Producer {
        private final String name;
        private final String affiliation;

        // -------------------- CONSTRUCTORS --------------------

        public Producer(String name, String affiliation) {
            this.name = name;
            this.affiliation = affiliation;
        }

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public String getAffiliation() {
            return affiliation;
        }
    }
}
