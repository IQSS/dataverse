package edu.harvard.iq.dataverse.export.datacite;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.List;

@JacksonXmlRootElement(localName = "resource")
@JsonPropertyOrder({ "xsiSchemaLocation", "xmlns", "xmlnsXsi", "identifier", "creators", "titles", "publisher",
        "publicationYear", "resourceType", "relatedIdentifiers", "descriptions", "contributors", "fundingReferences" })
public class DataCiteResource {

    @JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
    private String xsiSchemaLocation = "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd";

    @JacksonXmlProperty(isAttribute = true)
    private String xmlns = "http://datacite.org/schema/kernel-4";

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private String xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance";

    private Identifier identifier;

    @JacksonXmlElementWrapper(localName = "creators")
    @JacksonXmlProperty(localName = "creator")
    private List<Creator> creators = null;

    @JacksonXmlElementWrapper(localName = "titles")
    @JacksonXmlProperty(localName = "title")
    private List<String> titles = null;

    private String publisher;

    private String publicationYear;

    private ResourceType resourceType = new ResourceType();

    @JacksonXmlElementWrapper(localName = "relatedIdentifiers")
    @JacksonXmlProperty(localName = "relatedIdentifier")
    private List<RelatedIdentifier> relatedIdentifiers = null;

    @JacksonXmlElementWrapper(localName = "descriptions")
    @JacksonXmlProperty(localName = "description")
    private List<Description> descriptions = null;

    @JacksonXmlElementWrapper(localName = "contributors")
    @JacksonXmlProperty(localName = "contributor")
    private List<Contributor> contributors = null;

    @JacksonXmlElementWrapper(localName = "fundingReferences")
    @JacksonXmlProperty(localName = "fundingReference")
    private List<FundingReference> fundingReferences = null;

    // -------------------- GETTERS --------------------

    public Identifier getIdentifier() {
        return identifier;
    }

    public List<Creator> getCreators() {
        return creators;
    }

    public List<String> getTitles() {
        return titles;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public List<RelatedIdentifier> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public List<Description> getDescriptions() {
        return descriptions;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public List<FundingReference> getFundingReferences() {
        return fundingReferences;
    }

    // -------------------- SETTERS --------------------

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public void setCreators(List<Creator> creators) {
        this.creators = creators;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setRelatedIdentifiers(List<RelatedIdentifier> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public void setDescriptions(List<Description> descriptions) {
        this.descriptions = descriptions;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public void setFundingReferences(List<FundingReference> fundingReferences) {
        this.fundingReferences = fundingReferences;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Identifier {
        @JacksonXmlProperty(isAttribute = true)
        private String identifierType = "DOI";

        @JacksonXmlText
        private String value;

        // -------------------- CONSTRUCTORS --------------------

        public Identifier(String value) {
            this.value = value;
        }

        // -------------------- GETTERS --------------------

        public String getIdentifierType() {
            return identifierType;
        }

        public String getValue() {
            return value;
        }

        // -------------------- SETTERS --------------------

        public void setIdentifierType(String identifierType) {
            this.identifierType = identifierType;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Creator {

        private String creatorName;

        private NameIdentifier nameIdentifier;

        private Affiliation affiliation;

        // -------------------- CONSTRUCTORS --------------------

        public Creator(String creatorName) {
            this.creatorName = creatorName;
        }

        // -------------------- GETTERS --------------------

        public String getCreatorName() {
            return creatorName;
        }

        public NameIdentifier getNameIdentifier() {
            return nameIdentifier;
        }

        public Affiliation getAffiliation() {
            return affiliation;
        }

        // -------------------- SETTERS --------------------

        public void setCreatorName(String creatorName) {
            this.creatorName = creatorName;
        }

        public void setNameIdentifier(NameIdentifier nameIdentifier) {
            this.nameIdentifier = nameIdentifier;
        }

        public void setAffiliation(Affiliation affiliation) {
            this.affiliation = affiliation;
        }
    }

    public static class NameIdentifier {
        @JacksonXmlProperty(isAttribute = true)
        private String schemeURI;

        @JacksonXmlProperty(isAttribute = true)
        private String nameIdentifierScheme;

        @JacksonXmlText
        private String value;

        // -------------------- CONSTRUCTORS --------------------

        public NameIdentifier(String value, String schemeURI, String nameIdentifierScheme) {
            this.value = value;
            this.schemeURI = schemeURI;
            this.nameIdentifierScheme = nameIdentifierScheme;
        }

        // -------------------- GETTERS --------------------

        public String getSchemeURI() {
            return schemeURI;
        }

        public String getNameIdentifierScheme() {
            return nameIdentifierScheme;
        }

        public String getValue() {
            return value;
        }

        public void setNameIdentifierScheme(String nameIdentifierScheme) {
            this.nameIdentifierScheme = nameIdentifierScheme;
        }

        // -------------------- SETTERS --------------------

        public void setSchemeURI(String schemeURI) {
            this.schemeURI = schemeURI;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Affiliation {
        @JacksonXmlProperty(isAttribute = true)
        private String affiliationIdentifier;

        @JacksonXmlProperty(isAttribute = true)
        private String affiliationIdentifierScheme;

        @JacksonXmlText
        private String value;

        // -------------------- CONSTRUCTORS --------------------

        public Affiliation(String value) {
            this.value = value;
        }

        // -------------------- GETTERS --------------------

        public String getAffiliationIdentifier() {
            return affiliationIdentifier;
        }

        public String getAffiliationIdentifierScheme() {
            return affiliationIdentifierScheme;
        }

        public String getValue() {
            return value;
        }

        // -------------------- SETTERS --------------------

        public void setAffiliationIdentifier(String affiliationIdentifier) {
            this.affiliationIdentifier = affiliationIdentifier;
        }

        public void setAffiliationIdentifierScheme(String affiliationIdentifierScheme) {
            this.affiliationIdentifierScheme = affiliationIdentifierScheme;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class ResourceType {
        @JacksonXmlProperty(isAttribute = true)
        private String resourceTypeGeneral = "Dataset";

        public String getResourceTypeGeneral() {
            return resourceTypeGeneral;
        }

        public void setResourceTypeGeneral(String resourceTypeGeneral) {
            this.resourceTypeGeneral = resourceTypeGeneral;
        }
    }

    public static class RelatedIdentifier {
        @JacksonXmlProperty(isAttribute = true)
        private String relatedIdentifierType;

        @JacksonXmlProperty(isAttribute = true)
        private String relationType;

        @JacksonXmlText
        private String value;

        // -------------------- CONSTRUCTORS --------------------

        public RelatedIdentifier(String value, String relationType) {
            this(value, relationType, "DOI");
        }

        public RelatedIdentifier(String value, String relationType, String relatedIdentifierType) {
            this.value = value;
            this.relationType = relationType;
            this.relatedIdentifierType = relatedIdentifierType;
        }

        // -------------------- GETTERS --------------------

        public String getRelatedIdentifierType() {
            return relatedIdentifierType;
        }

        public String getRelationType() {
            return relationType;
        }

        public String getValue() {
            return value;
        }

        // -------------------- SETTERS --------------------

        public void setRelationType(String relationType) {
            this.relationType = relationType;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Description {
        @JacksonXmlProperty(isAttribute = true)
        private String descriptionType = "Abstract";

        @JacksonXmlText
        private String value;

        public Description(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Contributor {
        @JacksonXmlProperty(isAttribute = true)
        private ContributorType contributorType;

        private String contributorName;

        private Affiliation affiliation;

        // -------------------- CONSTRUCTORS --------------------

        public Contributor(ContributorType contributorType, String contributorName, Affiliation affiliation) {
            this.contributorType = contributorType;
            this.contributorName = contributorName;
            this.affiliation = affiliation;
        }

        // -------------------- GETTERS --------------------

        public ContributorType getContributorType() {
            return contributorType;
        }

        public String getContributorName() {
            return contributorName;
        }

        public Affiliation getAffiliation() {
            return affiliation;
        }

        // -------------------- SETTERS --------------------

        public void setContributorType(ContributorType contributorType) {
            this.contributorType = contributorType;
        }

        public void setContributorName(String contributorName) {
            this.contributorName = contributorName;
        }

        public void setAffiliation(Affiliation affiliation) {
            this.affiliation = affiliation;
        }
    }

    public static class FundingReference {
        private String funderName;

        private FunderIdentifier funderIdentifier;

        private String awardNumber;

        // -------------------- GETTERS --------------------

        public String getFunderName() {
            return funderName;
        }

        public FunderIdentifier getFunderIdentifier() {
            return funderIdentifier;
        }

        public String getAwardNumber() {
            return awardNumber;
        }

        // -------------------- SETTERS --------------------

        public void setFunderName(String funderName) {
            this.funderName = funderName;
        }

        public void setFunderIdentifier(FunderIdentifier funderIdentifier) {
            this.funderIdentifier = funderIdentifier;
        }

        public void setAwardNumber(String awardNumber) {
            this.awardNumber = awardNumber;
        }
    }

    public static class FunderIdentifier {
        @JacksonXmlProperty(isAttribute = true)
        private String funderIdentifierType = "ROR";

        @JacksonXmlText
        private String value;

        public FunderIdentifier(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public enum ContributorType {
        ContactPerson,
        Producer

        // All possible values: http://schema.datacite.org/meta/kernel-4.1/include/datacite-contributorType-v4.xsd
    }
}
