package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("data")
public class DataCiteFindDoiResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("attributes")
    private Attributes attributes = new Attributes();

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public int getCitationCount() {
        return attributes.getCitationCount();
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCitationCount(int citationCount) {
        attributes.setCitationCount(citationCount);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "DataCiteFindDoiResponse [id=" + id + ", citationCount=" + attributes.getCitationCount() + "]";
    }

    // -------------------- INNER CLASSES --------------------

    public static class Attributes {

        @JsonProperty("citationCount")
        private int citationCount;

        @JsonProperty
        private String prefix;

        @JsonProperty
        private String suffix;

        @JsonProperty
        private String publisher;

        @JsonProperty
        private Integer publicationYear;

        @JsonProperty
        private List<ResourceTitle> titles;

        @JsonProperty
        private List<ResourceDate> dates;

        @JsonProperty
        private ResourceType types;

        @JsonProperty
        private List<Creator> creators;

        @JsonProperty
        private List<Contributor> contributors;

        @JsonProperty
        private List<Description> descriptions;

        @JsonProperty
        private List<FundingReference> fundingReferences;

        @JsonProperty
        private List<RelatedIdentifier> relatedIdentifiers;

        // -------------------- GETTERS --------------------

        public int getCitationCount() {
            return citationCount;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getPublisher() {
            return publisher;
        }

        public Integer getPublicationYear() {
            return publicationYear;
        }

        public List<ResourceTitle> getTitles() {
            return titles;
        }

        public List<ResourceDate> getDates() {
            return dates;
        }

        public ResourceType getTypes() {
            return types;
        }

        public List<Creator> getCreators() {
            return creators;
        }

        public List<Contributor> getContributors() {
            return contributors;
        }

        public List<Description> getDescriptions() {
            return descriptions;
        }

        public List<FundingReference> getFundingReferences() {
            return fundingReferences;
        }

        public List<RelatedIdentifier> getRelatedIdentifiers() {
            return relatedIdentifiers;
        }

        // -------------------- SETTERS --------------------

        public void setCitationCount(int citationCount) {
            this.citationCount = citationCount;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public void setPublicationYear(Integer publicationYear) {
            this.publicationYear = publicationYear;
        }

        public void setTitles(List<ResourceTitle> titles) {
            this.titles = titles;
        }

        public void setDates(List<ResourceDate> dates) {
            this.dates = dates;
        }

        public void setTypes(ResourceType types) {
            this.types = types;
        }

        public void setCreators(List<Creator> creators) {
            this.creators = creators;
        }

        public void setContributors(List<Contributor> contributors) {
            this.contributors = contributors;
        }

        public void setDescriptions(List<Description> descriptions) {
            this.descriptions = descriptions;
        }

        public void setFundingReferences(List<FundingReference> fundingReferences) {
            this.fundingReferences = fundingReferences;
        }

        public void setRelatedIdentifiers(List<RelatedIdentifier> relatedIdentifiers) {
            this.relatedIdentifiers = relatedIdentifiers;
        }
    }

    public static class ResourceDate {

        @JsonProperty
        private String date;

        @JsonProperty
        private String dateType;

        // -------------------- GETTERS --------------------

        public String getDate() {
            return date;
        }

        public String getDateType() {
            return dateType;
        }
    }

    public static class Creator {

        @JsonProperty
        private String name;

        @JsonProperty
        private List<NameIdentifier> nameIdentifiers;

        @JsonProperty
        private List<String> affiliation;

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public List<NameIdentifier> getNameIdentifiers() {
            return nameIdentifiers;
        }

        public List<String> getAffiliation() {
            return affiliation;
        }
    }

    public static class NameIdentifier {

        @JsonProperty
        private String schemeUri;

        @JsonProperty
        private String nameIdentifier;

        @JsonProperty
        private String nameIdentifierScheme;

        // -------------------- GETTERS --------------------

        public String getSchemeUri() {
            return schemeUri;
        }

        public String getNameIdentifier() {
            return nameIdentifier;
        }

        public String getNameIdentifierScheme() {
            return nameIdentifierScheme;
        }
    }

    public static class Contributor {

        @JsonProperty
        private String name;

        @JsonProperty
        private String nameType;

        @JsonProperty
        private String givenName;

        @JsonProperty
        private String familyName;

        @JsonProperty
        private ContributorType contributorType;

        @JsonProperty
        private List<NameIdentifier> nameIdentifiers;

        @JsonProperty
        private List<String> affiliation;

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public String getNameType() {
            return nameType;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public ContributorType getContributorType() {
            return contributorType;
        }

        public List<NameIdentifier> getNameIdentifiers() {
            return nameIdentifiers;
        }

        public List<String> getAffiliation() {
            return affiliation;
        }
    }

    public static class Description {

        @JsonProperty
        private String description;

        @JsonProperty
        private String descriptionType;

        @JsonProperty
        private String lang;

        // -------------------- GETTERS --------------------

        public String getDescription() {
            return description;
        }

        public String getDescriptionType() {
            return descriptionType;
        }

        public String getLang() {
            return lang;
        }
    }

    public static class FundingReference {

        @JsonProperty
        private String funderName;

        @JsonProperty
        private String funderIdentifier;

        @JsonProperty
        private String funderIdentifierType;

        @JsonProperty
        private String awardNumber;

        @JsonProperty
        private String awardTitle;

        // -------------------- GETTERS --------------------

        public String getFunderName() {
            return funderName;
        }

        public String getFunderIdentifier() {
            return funderIdentifier;
        }

        public String getFunderIdentifierType() {
            return funderIdentifierType;
        }

        public String getAwardNumber() {
            return awardNumber;
        }

        public String getAwardTitle() {
            return awardTitle;
        }
    }

    public static class ResourceTitle {

        @JsonProperty
        private String title;

        @JsonProperty
        private TitleType titleType;

        @JsonProperty
        private String lang;

        // -------------------- GETTERS --------------------

        public String getTitle() {
            return title;
        }

        public TitleType getTitleType() {
            return titleType;
        }

        public String getLang() {
            return lang;
        }
    }

    public static class ResourceType {

        @JsonProperty
        private String resourceTypeGeneral;

        // -------------------- GETTERS --------------------

        public String getResourceTypeGeneral() {
            return resourceTypeGeneral;
        }
    }

    public static class RelatedIdentifier {

        @JsonProperty
        private String relationType;

        @JsonProperty
        private String relatedIdentifier;

        @JsonProperty
        private String relatedIdentifierType;

        // -------------------- GETTERS --------------------

        public String getRelationType() {
            return relationType;
        }

        public String getRelatedIdentifier() {
            return relatedIdentifier;
        }

        public String getRelatedIdentifierType() {
            return relatedIdentifierType;
        }
    }

    public enum ContributorType {
        ContactPerson,
        DataCollector,
        DataCurator,
        DataManager,
        Distributor,
        Editor,
        HostingInstitution,
        Producer,
        ProjectLeader,
        ProjectManager,
        ProjectMember,
        RegistrationAgency,
        RegistrationAuthority,
        RelatedPerson,
        Researcher,
        ResearchGroup,
        RightsHolder,
        Sponsor,
        Supervisor,
        WorkPackageLeader,
        Other
    }

    public enum TitleType {
        AlternativeTitle,
        Subtitle,
        TranslatedTitle,
        Other
    }
}
