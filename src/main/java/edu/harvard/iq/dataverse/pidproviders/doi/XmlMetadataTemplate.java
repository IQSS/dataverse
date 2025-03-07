package edu.harvard.iq.dataverse.pidproviders.doi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.ocpsoft.common.util.Strings;

import edu.harvard.iq.dataverse.AlternativePersistentIdentifier;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetRelPublication;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.ExternalIdentifier;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.PersonOrOrgUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import edu.harvard.iq.dataverse.util.xml.XmlWriterUtil;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonObject;

public class XmlMetadataTemplate {

    private static final Logger logger = Logger.getLogger(XmlMetadataTemplate.class.getName());

    public static final String XML_NAMESPACE = "http://datacite.org/schema/kernel-4";
    public static final String XML_SCHEMA_LOCATION = "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.5/metadata.xsd";
    public static final String XML_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XML_SCHEMA_VERSION = "4.5";

    private DoiMetadata doiMetadata;

    public XmlMetadataTemplate() {
    }

    public XmlMetadataTemplate(DoiMetadata doiMetadata) {
        this.doiMetadata = doiMetadata;
    }

    public String generateXML(DvObject dvObject) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            generateXML(dvObject, outputStream);

            String xml = outputStream.toString();
            logger.fine(xml);
            return XmlPrinter.prettyPrintXml(xml);
        } catch (XMLStreamException | IOException e) {
            logger.severe("Unable to generate DataCite XML for DOI: " + dvObject.getGlobalId().asString() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void generateXML(DvObject dvObject, OutputStream outputStream) throws XMLStreamException {
        // Could/should use dataset metadata language for metadata from DvObject itself?
        String language = null; // machine locale? e.g. for Publisher which is global
        String metadataLanguage = null; // when set, otherwise = language?
        
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("resource");
        boolean deaccessioned=false;
        if(dvObject instanceof Dataset d) {
            deaccessioned=d.isDeaccessioned();
        } else if (dvObject instanceof DataFile df) {
            deaccessioned = df.isDeaccessioned();
        }
        xmlw.writeDefaultNamespace(XML_NAMESPACE);
        xmlw.writeAttribute("xmlns:xsi", XML_XSI);
        xmlw.writeAttribute("xsi:schemaLocation", XML_SCHEMA_LOCATION);

        writeIdentifier(xmlw, dvObject);
        writeCreators(xmlw, doiMetadata.getAuthors(), deaccessioned);
        writeTitles(xmlw, dvObject, language, deaccessioned);
        writePublisher(xmlw, dvObject, deaccessioned);
        writePublicationYear(xmlw, dvObject, deaccessioned);
        if (!deaccessioned) {
            writeSubjects(xmlw, dvObject);
            writeContributors(xmlw, dvObject);
            writeDates(xmlw, dvObject);
            writeLanguage(xmlw, dvObject);
        }
        writeResourceType(xmlw, dvObject);
        if (!deaccessioned) {
            writeAlternateIdentifiers(xmlw, dvObject);
            writeRelatedIdentifiers(xmlw, dvObject);
            writeSize(xmlw, dvObject);
            writeFormats(xmlw, dvObject);
            writeVersion(xmlw, dvObject);
            writeAccessRights(xmlw, dvObject);
        }
        writeDescriptions(xmlw, dvObject, deaccessioned);
        if (!deaccessioned) {
            writeGeoLocations(xmlw, dvObject);
            writeFundingReferences(xmlw, dvObject);
        }
        xmlw.writeEndElement();
        xmlw.flush();
    }

    /**
     * 3, Title(s) (with optional type sub-properties) (M)
     *
     * @param xmlw
     *            The Stream writer
     * @param dvObject
     *            The dataset/file
     * @param language
     *            the metadata language
     * @return
     * @throws XMLStreamException
     */
    private void writeTitles(XMLStreamWriter xmlw, DvObject dvObject, String language, boolean deaccessioned) throws XMLStreamException {
        String title = null;
        String subTitle = null;
        List<String> altTitles = new ArrayList<>();

        if (!deaccessioned) {
            title = doiMetadata.getTitle();

            // Only Datasets can have a subtitle or alternative titles
            if (dvObject instanceof Dataset d) {
                DatasetVersion dv = d.getLatestVersionForCopy();
                Optional<DatasetField> subTitleField = dv.getDatasetFields().stream().filter(f -> f.getDatasetFieldType().getName().equals(DatasetFieldConstant.subTitle)).findFirst();
                if (subTitleField.isPresent()) {
                    subTitle = subTitleField.get().getValue();
                }
                Optional<DatasetField> altTitleField = dv.getDatasetFields().stream().filter(f -> f.getDatasetFieldType().getName().equals(DatasetFieldConstant.alternativeTitle)).findFirst();
                if (altTitleField.isPresent()) {
                    altTitles = altTitleField.get().getValues();
                }
            }
        } else {
            title = AbstractDOIProvider.UNAVAILABLE;
        }
        if (StringUtils.isNotBlank(title) || StringUtils.isNotBlank(subTitle) || (altTitles != null && !String.join("", altTitles).isBlank())) {
            xmlw.writeStartElement("titles");
            if (StringUtils.isNotBlank(title)) {
                XmlWriterUtil.writeFullElement(xmlw, "title", title, language);
            }
            Map<String, String> attributes = new HashMap<String, String>();

            if (StringUtils.isNotBlank(subTitle)) {
                attributes.put("titleType", "Subtitle");
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "title", attributes, subTitle);
            }
            if ((altTitles != null && !String.join("", altTitles).isBlank())) {
                attributes.clear();
                attributes.put("titleType", "AlternativeTitle");
                for (String altTitle : altTitles) {
                    XmlWriterUtil.writeFullElementWithAttributes(xmlw, "title", attributes, altTitle);
                }
            }
            xmlw.writeEndElement();
        }
    }

    /**
     * 1, Identifier (with mandatory type sub-property) (M) Note DataCite expects
     * identifierType="DOI" but OpenAire allows several others (see
     * https://guidelines.readthedocs.io/en/latest/data/field_identifier.html#d-identifiertype)
     * Dataverse is currently only capable of creating DOI, Handle, or URL types
     * from the OpenAire list (the last from PermaLinks) ToDo - If we add,e.g., an
     * ARK or PURL provider, this code has to change or we'll need to refactor so
     * that the identifiertype and id value can be sent via the JSON/ORE
     * 
     * @param xmlw
     *            The Steam writer
     * @param dvObject
     *            The dataset or file with the PID
     * @throws XMLStreamException
     */
    private void writeIdentifier(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        GlobalId pid = dvObject.getGlobalId();
        String identifierType = null;
        String identifier = null;
        switch (pid.getProtocol()) {
        case AbstractDOIProvider.DOI_PROTOCOL:
            identifierType = AbstractDOIProvider.DOI_PROTOCOL.toUpperCase();
            identifier = pid.asRawIdentifier();
            break;
        case HandlePidProvider.HDL_PROTOCOL:
            identifierType = "Handle";
            identifier = pid.asRawIdentifier();
            break;
        case PermaLinkPidProvider.PERMA_PROTOCOL:
            identifierType = "URL";
            identifier = pid.asURL();
            break;
        }
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("identifierType", identifierType);
        XmlWriterUtil.writeFullElementWithAttributes(xmlw, "identifier", attributeMap, identifier);
    }

    /**
     * 2, Creator (with optional given name, family name, name identifier and
     * affiliation sub-properties) (M)
     *
     * @param xmlw
     *            The stream writer
     * @param authorList
     *            - the list of authors
     * @throws XMLStreamException
     */
    public void writeCreators(XMLStreamWriter xmlw, List<DatasetAuthor> authorList, boolean deaccessioned) throws XMLStreamException {
        // creators -> creator -> creatorName with nameType attribute, givenName,
        // familyName, nameIdentifier
        // write all creators
        xmlw.writeStartElement("creators"); // <creators>
        if(deaccessioned) {
            //skip the loop below
            authorList = null;
        }
        boolean nothingWritten = true;
        if (authorList != null && !authorList.isEmpty()) {
            for (DatasetAuthor author : authorList) {
                String creatorName = author.getName().getDisplayValue();
                String affiliation = null;
                if (author.getAffiliation() != null && !author.getAffiliation().getValue().isEmpty()) {
                    affiliation = author.getAffiliation().getValue();
                }
                String nameIdentifier = null;
                String nameIdentifierScheme = null;
                if (StringUtils.isNotBlank(author.getIdValue()) && StringUtils.isNotBlank(author.getIdType())) {
                    nameIdentifier = author.getIdValue();
                    if (nameIdentifier != null) {
                        // Normalizes to the URL form of the identifier, returns null if the identifier
                        // is not valid given the type
                        nameIdentifier = author.getIdentifierAsUrl();
                    }
                    nameIdentifierScheme = author.getIdType();
                }

                if (StringUtils.isNotBlank(creatorName)) {
                    JsonObject creatorObj = PersonOrOrgUtil.getPersonOrOrganization(creatorName, false,
                            StringUtils.containsIgnoreCase(nameIdentifierScheme, "orcid"));
                    nothingWritten = false;
                    writeEntityElements(xmlw, "creator", null, creatorObj, affiliation, nameIdentifier, nameIdentifierScheme);
                }

                
            }
        }
        if (nothingWritten) {
            // Authors unavailable
            xmlw.writeStartElement("creator");
            XmlWriterUtil.writeFullElement(xmlw, "creatorName", AbstractPidProvider.UNAVAILABLE);
            xmlw.writeEndElement();
        }
        xmlw.writeEndElement(); // </creators>
    }

    private void writePublisher(XMLStreamWriter xmlw, DvObject dvObject, boolean deaccessioned) throws XMLStreamException {
        // publisher should already be non null - :unav if it wasn't available
        if(deaccessioned) {
            doiMetadata.setPublisher(AbstractPidProvider.UNAVAILABLE);
        }
        XmlWriterUtil.writeFullElement(xmlw, "publisher", doiMetadata.getPublisher());
    }

    private void writePublicationYear(XMLStreamWriter xmlw, DvObject dvObject, boolean deaccessioned) throws XMLStreamException {
        // Can't use "UNKNOWN" here because DataCite will respond with "[facet
        // 'pattern'] the value 'unknown' is not accepted by the pattern '[\d]{4}'"
        String pubYear = "9999";
        // FIXME: Investigate why this.publisherYear is sometimes null now that pull
        // request #4606 has been merged.
        if (! deaccessioned && (doiMetadata.getPublisherYear() != null)) {
            // Added to prevent a NullPointerException when trying to destroy datasets when
            // using DataCite rather than EZID.
            pubYear = doiMetadata.getPublisherYear();
        }
        XmlWriterUtil.writeFullElement(xmlw, "publicationYear", String.valueOf(pubYear));
    }

    /**
     * 6, Subject (with scheme sub-property) R
     *
     * @param xmlw
     *            The Steam writer
     * @param dvObject
     *            The Dataset/DataFile
     * @throws XMLStreamException
     */
    private void writeSubjects(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // subjects -> subject with subjectScheme and schemeURI attributes when
        // available
        boolean subjectsCreated = false;
        List<String> subjects = new ArrayList<String>();
        List<DatasetFieldCompoundValue> compoundKeywords = new ArrayList<DatasetFieldCompoundValue>();
        List<DatasetFieldCompoundValue> compoundTopics = new ArrayList<DatasetFieldCompoundValue>();
        // Dataset Subject= Dataverse subject, keyword, and/or topic classification
        // fields
        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            for (DatasetField dsf : dv.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                    subjects.addAll(dsf.getValues());
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.keyword)) {
                    compoundKeywords = dsf.getDatasetFieldCompoundValues();
                } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.topicClassification)) {
                    compoundTopics = dsf.getDatasetFieldCompoundValues();
                }
            }

        } else if (dvObject instanceof DataFile df) {
            subjects = df.getTagLabels();
        }
        for (String subject : subjects) {
            if (StringUtils.isNotBlank(subject)) {
                subjectsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "subjects", subjectsCreated);
                XmlWriterUtil.writeFullElement(xmlw, "subject", StringEscapeUtils.escapeXml10(subject));
            }
        }
        for (DatasetFieldCompoundValue keywordFieldValue : compoundKeywords) {
            String keyword = null;
            String scheme = null;
            String schemeUri = null;

            for (DatasetField subField : keywordFieldValue.getChildDatasetFields()) {
                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.keywordValue:
                    keyword = subField.getValue();
                    break;
                case DatasetFieldConstant.keywordVocab:
                    scheme = subField.getValue();
                    break;
                case DatasetFieldConstant.keywordVocabURI:
                    schemeUri = subField.getValue();
                    break;
                }
            }
            if (StringUtils.isNotBlank(keyword)) {
                Map<String, String> attributesMap = new HashMap<String, String>();
                if (StringUtils.isNotBlank(scheme)) {
                    attributesMap.put("subjectScheme", scheme);
                }
                if (StringUtils.isNotBlank(schemeUri)) {
                    attributesMap.put("schemeURI", schemeUri);
                }
                subjectsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "subjects", subjectsCreated);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "subject", attributesMap, StringEscapeUtils.escapeXml10(keyword));
            }
        }
        for (DatasetFieldCompoundValue topicFieldValue : compoundTopics) {
            String topic = null;
            String scheme = null;
            String schemeUri = null;

            for (DatasetField subField : topicFieldValue.getChildDatasetFields()) {

                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.topicClassValue:
                    topic = subField.getValue();
                    break;
                case DatasetFieldConstant.topicClassVocab:
                    scheme = subField.getValue();
                    break;
                case DatasetFieldConstant.topicClassVocabURI:
                    schemeUri = subField.getValue();
                    break;
                }
            }
            if (StringUtils.isNotBlank(topic)) {
                Map<String, String> attributesMap = new HashMap<String, String>();
                if (StringUtils.isNotBlank(scheme)) {
                    attributesMap.put("subjectScheme", scheme);
                }
                if (StringUtils.isNotBlank(schemeUri)) {
                    attributesMap.put("schemeURI", schemeUri);
                }
                subjectsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "subjects", subjectsCreated);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "subject", attributesMap, StringEscapeUtils.escapeXml10(topic));
            }
        }
        if (subjectsCreated) {
            xmlw.writeEndElement();
        }
    }

    /**
     * 7, Contributor (with optional given name, family name, name identifier and
     * affiliation sub-properties)
     *
     * @see #writeContributorElement(javax.xml.stream.XMLStreamWriter,
     *      java.lang.String, java.lang.String, java.lang.String)
     *
     * @param xmlw
     *            The stream writer
     * @param dvObject
     *            The Dataset/DataFile
     * @throws XMLStreamException
     */
    private void writeContributors(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        boolean contributorsCreated = false;
        List<DatasetFieldCompoundValue> compoundProducers = new ArrayList<DatasetFieldCompoundValue>();
        List<DatasetFieldCompoundValue> compoundDistributors = new ArrayList<DatasetFieldCompoundValue>();
        List<DatasetFieldCompoundValue> compoundContacts = new ArrayList<DatasetFieldCompoundValue>();
        List<DatasetFieldCompoundValue> compoundContributors = new ArrayList<DatasetFieldCompoundValue>();
        // Dataset Subject= Dataverse subject, keyword, and/or topic classification
        // fields
        // ToDo Include for files?
        /*
         * if(dvObject instanceof DataFile df) { dvObject = df.getOwner(); }
         */

        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            for (DatasetField dsf : dv.getDatasetFields()) {
                switch (dsf.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.producer:
                    compoundProducers = dsf.getDatasetFieldCompoundValues();
                    break;
                case DatasetFieldConstant.distributor:
                    compoundDistributors = dsf.getDatasetFieldCompoundValues();
                    break;
                case DatasetFieldConstant.datasetContact:
                    compoundContacts = dsf.getDatasetFieldCompoundValues();
                    break;
                case DatasetFieldConstant.contributor:
                    compoundContributors = dsf.getDatasetFieldCompoundValues();
                }
            }
        }

        for (DatasetFieldCompoundValue producerFieldValue : compoundProducers) {
            String producer = null;
            String affiliation = null;

            for (DatasetField subField : producerFieldValue.getChildDatasetFields()) {

                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.producerName:
                    producer = subField.getValue();
                    break;
                case DatasetFieldConstant.producerAffiliation:
                    affiliation = subField.getValue();
                    break;
                }
            }
            if (StringUtils.isNotBlank(producer)) {
                contributorsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "contributors", contributorsCreated);
                JsonObject entityObject = PersonOrOrgUtil.getPersonOrOrganization(producer, false, false);
                writeEntityElements(xmlw, "contributor", "Producer", entityObject, affiliation, null, null);
            }

        }

        for (DatasetFieldCompoundValue distributorFieldValue : compoundDistributors) {
            String distributor = null;
            String affiliation = null;

            for (DatasetField subField : distributorFieldValue.getChildDatasetFields()) {

                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.distributorName:
                    distributor = subField.getValue();
                    break;
                case DatasetFieldConstant.distributorAffiliation:
                    affiliation = subField.getValue();
                    break;
                }
            }
            if (StringUtils.isNotBlank(distributor)) {
                contributorsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "contributors", contributorsCreated);
                JsonObject entityObject = PersonOrOrgUtil.getPersonOrOrganization(distributor, false, false);
                writeEntityElements(xmlw, "contributor", "Distributor", entityObject, affiliation, null, null);
            }

        }
        for (DatasetFieldCompoundValue contactFieldValue : compoundContacts) {
            String contact = null;
            String affiliation = null;

            for (DatasetField subField : contactFieldValue.getChildDatasetFields()) {

                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.datasetContactName:
                    contact = subField.getValue();
                    break;
                case DatasetFieldConstant.datasetContactAffiliation:
                    affiliation = subField.getValue();
                    break;
                }
            }
            if (StringUtils.isNotBlank(contact)) {
                contributorsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "contributors", contributorsCreated);
                JsonObject entityObject = PersonOrOrgUtil.getPersonOrOrganization(contact, false, false);
                writeEntityElements(xmlw, "contributor", "ContactPerson", entityObject, affiliation, null, null);
            }

        }
        for (DatasetFieldCompoundValue contributorFieldValue : compoundContributors) {
            String contributor = null;
            String contributorType = null;

            for (DatasetField subField : contributorFieldValue.getChildDatasetFields()) {

                switch (subField.getDatasetFieldType().getName()) {
                    case DatasetFieldConstant.contributorName:
                        contributor = subField.getValue();
                        break;
                    case DatasetFieldConstant.contributorType:
                        contributorType = subField.getValue();
                        if (contributorType != null) {
                            contributorType = contributorType.replace(" ", "");
                        }
                        break;
                }
            }
            // QDR - doesn't have Funder in the contributor type list.
            // Using a string isn't i18n
            if (StringUtils.isNotBlank(contributor) && !StringUtils.equalsIgnoreCase("Funder", contributorType)) {
                contributorType = getCanonicalContributorType(contributorType);
                contributorsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "contributors", contributorsCreated);
                JsonObject entityObject = PersonOrOrgUtil.getPersonOrOrganization(contributor, false, false);
                writeEntityElements(xmlw, "contributor", contributorType, entityObject, null, null, null);
            }

        }

        if (contributorsCreated) {
            xmlw.writeEndElement();
        }
    }

    //List from https://schema.datacite.org/meta/kernel-4/include/datacite-contributorType-v4.xsd
    private Set<String> contributorTypes = new HashSet<>(Arrays.asList("ContactPerson", "DataCollector", "DataCurator", "DataManager", "Distributor", "Editor", 
                "HostingInstitution", "Other", "Producer", "ProjectLeader", "ProjectManager", "ProjectMember", "RegistrationAgency", "RegistrationAuthority", 
                "RelatedPerson", "ResearchGroup", "RightsHolder", "Researcher", "Sponsor", "Supervisor", "WorkPackageLeader"));

    private String getCanonicalContributorType(String contributorType) {
        if(StringUtils.isBlank(contributorType) || !contributorTypes.contains(contributorType)) {
            return "Other";
        }
        return contributorType;
    }

    private void writeEntityElements(XMLStreamWriter xmlw, String elementName, String type, JsonObject entityObject, String affiliation, String nameIdentifier, String nameIdentifierScheme) throws XMLStreamException {
        xmlw.writeStartElement(elementName);
        Map<String, String> attributeMap = new HashMap<String, String>();
        if (StringUtils.isNotBlank(type)) {
            xmlw.writeAttribute("contributorType", type);
        }
        // person name=<FamilyName>, <FirstName>
        if (entityObject.getBoolean("isPerson")) {
            attributeMap.put("nameType", "Personal");
        } else {
            attributeMap.put("nameType", "Organizational");
        }
        XmlWriterUtil.writeFullElementWithAttributes(xmlw, elementName + "Name", attributeMap,
                StringEscapeUtils.escapeXml10(entityObject.getString("fullName")));
        if (entityObject.containsKey("givenName")) {
            XmlWriterUtil.writeFullElement(xmlw, "givenName", StringEscapeUtils.escapeXml10(entityObject.getString("givenName")));
        }
        if (entityObject.containsKey("familyName")) {
            XmlWriterUtil.writeFullElement(xmlw, "familyName", StringEscapeUtils.escapeXml10(entityObject.getString("familyName")));
        }

        if (nameIdentifier != null) {
            attributeMap.clear();
            URL url;
            try {
                url = new URL(nameIdentifier);
                String protocol = url.getProtocol();
                String authority = url.getAuthority();
                String site = String.format("%s://%s", protocol, authority);
                attributeMap.put("schemeURI", site);
                attributeMap.put("nameIdentifierScheme", nameIdentifierScheme);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "nameIdentifier", attributeMap, nameIdentifier);
            } catch (MalformedURLException e) {
                logger.warning("DatasetAuthor.getIdentifierAsUrl returned a Malformed URL: " + nameIdentifier);
            }
        }

        if (StringUtils.isNotBlank(affiliation)) {
            attributeMap.clear();
            boolean isROR=false;
            String orgName = affiliation;
            ExternalIdentifier externalIdentifier = ExternalIdentifier.ROR;
            if (externalIdentifier.isValidIdentifier(orgName)) {
                isROR = true;
                JsonObject jo = getExternalVocabularyValue(orgName);
                if (jo != null) {
                    orgName = jo.getString("termName");
                }
            }
          
            if (isROR) {

                attributeMap.put("schemeURI", "https://ror.org");
                attributeMap.put("affiliationIdentifierScheme", "ROR");
                attributeMap.put("affiliationIdentifier", affiliation);
            }

            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "affiliation", attributeMap, StringEscapeUtils.escapeXml10(orgName));
        }
        xmlw.writeEndElement();
    }

    private JsonObject getExternalVocabularyValue(String id) {
        return CDI.current().select(DatasetFieldServiceBean.class).get().getExternalVocabularyValue(id);
    }

    /**
     * 8, Date (with type sub-property) (R)
     *
     * @param xmlw
     *            The Steam writer
     * @param dvObject
     *            The dataset/datafile
     * @throws XMLStreamException
     */
    private void writeDates(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        boolean datesWritten = false;
        String dateOfDistribution = null;
        String dateOfProduction = null;
        String dateOfDeposit = null;
        Date releaseDate = null;
        String publicationDate = null;
        boolean isAnUpdate=false;
        List<DatasetFieldCompoundValue> datesOfCollection = new ArrayList<DatasetFieldCompoundValue>();
        List<DatasetFieldCompoundValue> timePeriods = new ArrayList<DatasetFieldCompoundValue>();

        if (dvObject instanceof DataFile df) {
            // Find the first released version the file is in to give a published date
            List<FileMetadata> fmds = df.getFileMetadatas();
            DatasetVersion initialVersion = null;
            for (FileMetadata fmd : fmds) {
                DatasetVersion dv = fmd.getDatasetVersion();
                if (dv.isReleased()) {
                    initialVersion = dv;
                    publicationDate = Util.getDateFormat().format(dv.getReleaseTime());
                    break;
                }
            }
            // And the last update is the most recent
            for (int i = fmds.size() - 1; i >= 0; i--) {
                DatasetVersion dv = fmds.get(i).getDatasetVersion();
                if (dv.isReleased() && !dv.equals(initialVersion)) {
                    releaseDate = dv.getReleaseTime();
                    isAnUpdate=true;
                    break;
                }
            }
        } else if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            Long versionNumber = dv.getVersionNumber();
            if (versionNumber != null && !(versionNumber.equals(1L) && dv.getMinorVersionNumber().equals(0L))) {
                isAnUpdate = true;
            }
            releaseDate = dv.getReleaseTime();
            publicationDate = d.getPublicationDateFormattedYYYYMMDD();
            for (DatasetField dsf : dv.getDatasetFields()) {
                switch (dsf.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.distributionDate:
                    dateOfDistribution = dsf.getValue();
                    break;
                case DatasetFieldConstant.productionDate:
                    dateOfProduction = dsf.getValue();
                    break;
                case DatasetFieldConstant.dateOfDeposit:
                    dateOfDeposit = dsf.getValue();
                    break;
                case DatasetFieldConstant.dateOfCollection:
                    datesOfCollection = dsf.getDatasetFieldCompoundValues();
                    break;
                case DatasetFieldConstant.timePeriodCovered:
                    timePeriods = dsf.getDatasetFieldCompoundValues();
                    break;
                }
            }
        }
        Map<String, String> attributes = new HashMap<String, String>();
        if (StringUtils.isNotBlank(dateOfDistribution)) {
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
            attributes.put("dateType", "Issued");
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, dateOfDistribution);
        }
        // dates -> date with dateType attribute

        if (StringUtils.isNotBlank(dateOfProduction)) {
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
            attributes.put("dateType", "Created");
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, dateOfProduction);
        }
        if (StringUtils.isNotBlank(dateOfDeposit)) {
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
            attributes.put("dateType", "Submitted");
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, dateOfDeposit);
        }

        if (publicationDate != null) {
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);

            attributes.put("dateType", "Available");
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, publicationDate);
        }
        if (isAnUpdate) {
            String date = Util.getDateFormat().format(releaseDate);
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);

            attributes.put("dateType", "Updated");
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, date);
        }
        if (datesOfCollection != null) {
            for (DatasetFieldCompoundValue collectionDateFieldValue : datesOfCollection) {
                String startDate = null;
                String endDate = null;

                for (DatasetField subField : collectionDateFieldValue.getChildDatasetFields()) {
                    switch (subField.getDatasetFieldType().getName()) {
                    case DatasetFieldConstant.dateOfCollectionStart:
                        startDate = subField.getValue();
                        break;
                    case DatasetFieldConstant.dateOfCollectionEnd:
                        endDate = subField.getValue();
                        break;
                    }
                }
                // Minimal clean-up - useful? Parse/format would remove unused chars, and an
                // exception would clear the date so we don't send nonsense
                startDate = cleanUpDate(startDate);
                endDate = cleanUpDate(endDate);
                if (StringUtils.isNotBlank(startDate) || StringUtils.isNotBlank(endDate)) {
                    datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
                    attributes.put("dateType", "Collected");
                    XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, (startDate + "/" + endDate).trim());
                }
            }
        }
        if (timePeriods != null) {
            for (DatasetFieldCompoundValue timePeriodFieldValue : timePeriods) {
                String startDate = null;
                String endDate = null;

                for (DatasetField subField : timePeriodFieldValue.getChildDatasetFields()) {
                    switch (subField.getDatasetFieldType().getName()) {
                    case DatasetFieldConstant.timePeriodCoveredStart:
                        startDate = subField.getValue();
                        break;
                    case DatasetFieldConstant.timePeriodCoveredEnd:
                        endDate = subField.getValue();
                        break;
                    }
                }
                // Minimal clean-up - useful? Parse/format would remove unused chars, and an
                // exception would clear the date so we don't send nonsense
                startDate = cleanUpDate(startDate);
                endDate = cleanUpDate(endDate);
                if (StringUtils.isNotBlank(startDate) || StringUtils.isNotBlank(endDate)) {
                    datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
                    attributes.put("dateType", "Other");
                    attributes.put("dateInformation", "Time period covered by the data");
                    XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, (startDate + "/" + endDate).trim());
                }
            }
        }
        if (datesWritten) {
            xmlw.writeEndElement();
        }
    }

    private String cleanUpDate(String date) {
        String newDate = null;
        if (!StringUtils.isBlank(date)) {
            try {
                SimpleDateFormat sdf = Util.getDateFormat();
                Date start = sdf.parse(date);
                newDate = sdf.format(start);
            } catch (ParseException e) {
                logger.warning("Could not parse date: " + date);
            }
        }
        return newDate;
    }

    // 9, Language (MA), language
    private void writeLanguage(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // Currently not supported. Spec indicates one 'primary' language. Could send
        // the first entry in DatasetFieldConstant.language or send iff there is only
        // one entry, and/or default to the machine's default lang, or the dataverse metadatalang?
        return;
    }

    // 10, ResourceType (with mandatory general type
    // description sub- property) (M)
    private void writeResourceType(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        List<String> kindOfDataValues = new ArrayList<String>();
        Map<String, String> attributes = new HashMap<String, String>();
        String resourceType = "Dataset";
        if (dvObject instanceof Dataset dataset) {
            String datasetTypeName = dataset.getDatasetType().getName();
            resourceType = switch (datasetTypeName) {
            case DatasetType.DATASET_TYPE_DATASET -> "Dataset";
            case DatasetType.DATASET_TYPE_SOFTWARE -> "Software";
            case DatasetType.DATASET_TYPE_WORKFLOW -> "Workflow";
            default -> "Dataset";
            };
        }
        attributes.put("resourceTypeGeneral", resourceType);
        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            for (DatasetField dsf : dv.getDatasetFields()) {
                switch (dsf.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.kindOfData:
                    List<String> vals = dsf.getValues();
                    for(String val: vals) {
                        if(StringUtils.isNotBlank(val)) {
                            kindOfDataValues.add(val);
                        }
                    }
                    break;
                }
            }
        }
        if (!kindOfDataValues.isEmpty()) {
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "resourceType", attributes, String.join(";", kindOfDataValues));

        } else {
            // Write an attribute only element if there are no kindOfData values.
            xmlw.writeStartElement("resourceType");
            xmlw.writeAttribute("resourceTypeGeneral", attributes.get("resourceTypeGeneral"));
            xmlw.writeEndElement();
        }

    }

    /**
     * 11 AlternateIdentifier (with type sub-property) (O)
     *
     * @param xmlw
     *            The Steam writer
     * @param dvObject
     *            The dataset/datafile
     * @throws XMLStreamException
     */
    private void writeAlternateIdentifiers(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        List<DatasetFieldCompoundValue> otherIdentifiers = new ArrayList<DatasetFieldCompoundValue>();
        Set<AlternativePersistentIdentifier> altPids = dvObject.getAlternativePersistentIndentifiers();

        boolean alternatesWritten = false;

        Map<String, String> attributes = new HashMap<String, String>();
        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            for (DatasetField dsf : dv.getDatasetFields()) {
                if (DatasetFieldConstant.otherId.equals(dsf.getDatasetFieldType().getName())) {
                    otherIdentifiers = dsf.getDatasetFieldCompoundValues();
                    break;
                }
            }
        }

        if (altPids != null && !altPids.isEmpty()) {
            alternatesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "alternateIdentifiers", alternatesWritten);
            for (AlternativePersistentIdentifier altPid : altPids) {
                String identifierType = null;
                String identifier = null;
                switch (altPid.getProtocol()) {
                case AbstractDOIProvider.DOI_PROTOCOL:
                    identifierType = AbstractDOIProvider.DOI_PROTOCOL.toUpperCase();
                    identifier = altPid.getAuthority() + "/" + altPid.getIdentifier();
                    break;
                case HandlePidProvider.HDL_PROTOCOL:
                    identifierType = "Handle";
                    identifier = altPid.getAuthority() + "/" + altPid.getIdentifier();
                    break;
                default:
                    // The AlternativePersistentIdentifier class isn't really ready for anything but
                    // doi or handle pids, but will add this as a default.
                    identifierType = ":unav";
                    identifier = altPid.getAuthority() + altPid.getIdentifier();
                    break;
                }
                attributes.put("alternateIdentifierType", identifierType);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "alternateIdentifier", attributes, identifier);

            }
        }

        for (DatasetFieldCompoundValue otherIdentifier : otherIdentifiers) {
            String identifierType = null;
            String identifier = null;
            for (DatasetField subField : otherIdentifier.getChildDatasetFields()) {
                identifierType = ":unav";
                switch (subField.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.otherIdAgency:
                    identifierType = subField.getValue();
                    break;
                case DatasetFieldConstant.otherIdValue:
                    identifier = subField.getValue();
                    break;
                }
            }
            attributes.put("alternateIdentifierType", identifierType);
            if (!StringUtils.isBlank(identifier)) {
                alternatesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "alternateIdentifiers", alternatesWritten);

                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "alternateIdentifier", attributes, identifier);
            }
        }
        if (alternatesWritten) {
            xmlw.writeEndElement();
        }
    }

    /**
     * 12, RelatedIdentifier (with type and relation type sub-properties) (R)
     *
     * @param xmlw
     *            The Steam writer
     * @param dvObject
     *            the dataset/datafile
     * @throws XMLStreamException
     */
    private void writeRelatedIdentifiers(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {

        boolean relatedIdentifiersWritten = false;

        Map<String, String> attributes = new HashMap<String, String>();

        if (dvObject instanceof Dataset dataset) {
            List<DatasetRelPublication> relatedPublications = dataset.getLatestVersionForCopy().getRelatedPublications();
            if (!relatedPublications.isEmpty()) {
                for (DatasetRelPublication relatedPub : relatedPublications) {
                    attributes.clear();

                    String pubIdType = relatedPub.getIdType();
                    String identifier = relatedPub.getIdNumber();
                    String url = relatedPub.getUrl();
                    String relationType = relatedPub.getRelationType();
                    if(StringUtils.isBlank(relationType)) {
                        relationType = "IsSupplementTo";
                    }
                    /*
                     * Note - with identifier and url fields, it's not clear that there's a single
                     * way those two fields are used for all identifier types. The code here is
                     * ~best effort to interpret those fields.
                     */
                    logger.fine("Found relpub: " + pubIdType + " " + identifier + " " + url);

                    pubIdType = getCanonicalPublicationType(pubIdType);
                    logger.fine("Canonical type: " + pubIdType);
                    // Prefer identifier if set, otherwise check url
                    String relatedIdentifier = identifier;
                    if (StringUtils.isBlank(relatedIdentifier)) {
                        relatedIdentifier = url;
                    }
                    logger.fine("Related identifier: " + relatedIdentifier);
                    // For types where we understand the protocol, get the canonical form
                    if (StringUtils.isNotBlank(relatedIdentifier)) {
                        switch (pubIdType != null ? pubIdType : "none") {
                        case "DOI":
                            if (!(relatedIdentifier.startsWith("doi:") || relatedIdentifier.startsWith("http"))) {
                                relatedIdentifier = "doi:" + relatedIdentifier;
                            }
                            logger.fine("Intermediate Related identifier: " + relatedIdentifier);
                            try {
                                GlobalId pid = PidUtil.parseAsGlobalID(relatedIdentifier);
                                relatedIdentifier = pid.asRawIdentifier();
                            } catch (IllegalArgumentException e) {
                                logger.warning("Invalid DOI: " + e.getLocalizedMessage());
                                relatedIdentifier = null;
                            }
                            logger.fine("Final Related identifier: " + relatedIdentifier);
                            break;
                        case "Handle":
                            if (!relatedIdentifier.startsWith("hdl:") || !relatedIdentifier.startsWith("http")) {
                                relatedIdentifier = "hdl:" + relatedIdentifier;
                            }
                            try {
                                GlobalId pid = PidUtil.parseAsGlobalID(relatedIdentifier);
                                relatedIdentifier = pid.asRawIdentifier();
                            } catch (IllegalArgumentException e) {
                                relatedIdentifier = null;
                            }
                            break;
                        case "URL":
                            // If a URL is given, split the string to get a schemeUri
                            try {
                                URL relatedUrl = new URI(relatedIdentifier).toURL();
                                String protocol = relatedUrl.getProtocol();
                                String authority = relatedUrl.getAuthority();
                                String site = String.format("%s://%s", protocol, authority);
                                relatedIdentifier = relatedIdentifier.substring(site.length());
                                attributes.put("schemeURI", site);
                            } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
                                // Just an identifier but without a pubIdType we won't include it
                                logger.warning("Invalid Identifier of type URL: " + relatedIdentifier);
                                relatedIdentifier = null;
                            }
                            break;
                        case "none":
                            //Try to identify PIDs and URLs and send them as related identifiers
                            if (relatedIdentifier != null) {
                                // See if it is a GlobalID we know
                                try {
                                    GlobalId pid = PidUtil.parseAsGlobalID(relatedIdentifier);
                                    relatedIdentifier = pid.asRawIdentifier();
                                    pubIdType = getCanonicalPublicationType(pid.getProtocol());
                                } catch (IllegalArgumentException e) {
                                }
                                // For non-URL types, if a URL is given, split the string to get a schemeUri
                                try {
                                    URL relatedUrl = new URI(relatedIdentifier).toURL();
                                    String protocol = relatedUrl.getProtocol();
                                    String authority = relatedUrl.getAuthority();
                                    String site = String.format("%s://%s", protocol, authority);
                                    relatedIdentifier = relatedIdentifier.substring(site.length());
                                    attributes.put("schemeURI", site);
                                    pubIdType = "URL";
                                } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
                                    // Just an identifier but without a pubIdType we won't include it
                                    logger.warning("Related Identifier found without type: " + relatedIdentifier);
                                    //Won't be sent since pubIdType is null - could also set relatedIdentifier to null
                                }
                            }
                            break;
                        default:
                            //Some other valid type - we just send the identifier w/o optional attributes
                            //To Do - validation for other types?
                            break;
                        }
                    }
                    if (StringUtils.isNotBlank(relatedIdentifier) && StringUtils.isNotBlank(pubIdType)) {
                        // Still have a valid entry
                        attributes.put("relatedIdentifierType", pubIdType);
                        attributes.put("relationType", relationType);
                        relatedIdentifiersWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "relatedIdentifiers", relatedIdentifiersWritten);
                        XmlWriterUtil.writeFullElementWithAttributes(xmlw, "relatedIdentifier", attributes, relatedIdentifier);
                    }
                }
            }
            List<FileMetadata> fmds = dataset.getLatestVersionForCopy().getFileMetadatas();
            if (!((fmds==null) && fmds.isEmpty())) {
                attributes.clear();
                attributes.put("relationType", "HasPart");
                for (FileMetadata fmd : fmds) {
                    DataFile dataFile = fmd.getDataFile();
                    GlobalId pid = dataFile.getGlobalId();
                    if (pid != null) {
                        String pubIdType = getCanonicalPublicationType(pid.getProtocol());
                        if (pubIdType != null) {
                            attributes.put("relatedIdentifierType", pubIdType);
                            relatedIdentifiersWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "relatedIdentifiers", relatedIdentifiersWritten);
                            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "relatedIdentifier", attributes, pid.asRawIdentifier());
                        }
                    }
                }
            }
        } else if (dvObject instanceof DataFile df) {
            GlobalId pid = df.getOwner().getGlobalId();
            if (pid != null) {
                String pubIdType = getCanonicalPublicationType(pid.getProtocol());
                if (pubIdType != null) {

                    attributes.clear();
                    attributes.put("relationType", "IsPartOf");
                    attributes.put("relatedIdentifierType", pubIdType);
                    relatedIdentifiersWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "relatedIdentifiers", relatedIdentifiersWritten);
                    XmlWriterUtil.writeFullElementWithAttributes(xmlw, "relatedIdentifier", attributes, pid.asRawIdentifier());
                }
            }
        }
        if (relatedIdentifiersWritten) {
            xmlw.writeEndElement();
        }
    }

    static HashMap<String, String> relatedIdentifierTypeMap = new HashMap<String, String>();

    private static String getCanonicalPublicationType(String pubIdType) {
        if (relatedIdentifierTypeMap.isEmpty()) {
            relatedIdentifierTypeMap.put("ARK".toLowerCase(), "ARK");
            relatedIdentifierTypeMap.put("arXiv", "arXiv");
            relatedIdentifierTypeMap.put("bibcode".toLowerCase(), "bibcode");
            relatedIdentifierTypeMap.put("DOI".toLowerCase(), "DOI");
            relatedIdentifierTypeMap.put("EAN13".toLowerCase(), "EAN13");
            relatedIdentifierTypeMap.put("EISSN".toLowerCase(), "EISSN");
            relatedIdentifierTypeMap.put("Handle".toLowerCase(), "Handle");
            relatedIdentifierTypeMap.put("IGSN".toLowerCase(), "IGSN");
            relatedIdentifierTypeMap.put("ISBN".toLowerCase(), "ISBN");
            relatedIdentifierTypeMap.put("ISSN".toLowerCase(), "ISSN");
            relatedIdentifierTypeMap.put("ISTC".toLowerCase(), "ISTC");
            relatedIdentifierTypeMap.put("LISSN".toLowerCase(), "LISSN");
            relatedIdentifierTypeMap.put("LSID".toLowerCase(), "LSID");
            relatedIdentifierTypeMap.put("PISSN".toLowerCase(), "PISSN");
            relatedIdentifierTypeMap.put("PMID".toLowerCase(), "PMID");
            relatedIdentifierTypeMap.put("PURL".toLowerCase(), "PURL");
            relatedIdentifierTypeMap.put("UPC".toLowerCase(), "UPC");
            relatedIdentifierTypeMap.put("URL".toLowerCase(), "URL");
            relatedIdentifierTypeMap.put("URN".toLowerCase(), "URN");
            relatedIdentifierTypeMap.put("WOS".toLowerCase(), "WOS");
            // Add entry for Handle,Perma protocols so this can be used with GlobalId/getProtocol()
            relatedIdentifierTypeMap.put("hdl".toLowerCase(), "Handle");
            relatedIdentifierTypeMap.put("perma".toLowerCase(), "URL");
            
        }
        return relatedIdentifierTypeMap.get(pubIdType);
    }

    private void writeSize(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // sizes -> size
        boolean sizesWritten = false;
        List<DataFile> dataFiles = new ArrayList<DataFile>();

        if (dvObject instanceof Dataset dataset) {
            dataFiles = dataset.getFiles();
        } else if (dvObject instanceof DataFile df) {
            dataFiles.add(df);
        }
        if (dataFiles != null && !dataFiles.isEmpty()) {
            for (DataFile dataFile : dataFiles) {
                Long size = dataFile.getFilesize();
                if (size != -1) {
                    sizesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "sizes", sizesWritten);
                    XmlWriterUtil.writeFullElement(xmlw, "size", size.toString());
                }
            }
        }
        if (sizesWritten) {
            xmlw.writeEndElement();
        }

    }

    private void writeFormats(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {

        boolean formatsWritten = false;
        List<DataFile> dataFiles = new ArrayList<DataFile>();

        if (dvObject instanceof Dataset dataset) {
            dataFiles = dataset.getFiles();
        } else if (dvObject instanceof DataFile df) {
            dataFiles.add(df);
        }
        if (dataFiles != null && !dataFiles.isEmpty()) {
            for (DataFile dataFile : dataFiles) {
                String format = dataFile.getContentType();
                if (StringUtils.isNotBlank(format)) {
                    formatsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "formats", formatsWritten);
                    XmlWriterUtil.writeFullElement(xmlw, "format", format);
                }
                /*
                 * Should original formats be sent? What about original sizes above?
                 * if(dataFile.isTabularData()) { String originalFormat =
                 * dataFile.getOriginalFileFormat(); if(StringUtils.isNotBlank(originalFormat))
                 * { XmlWriterUtil.writeFullElement(xmlw, "format", format); } }
                 */
            }
        }
        if (formatsWritten) {
            xmlw.writeEndElement();
        }

    }

    private void writeVersion(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        Dataset d = null;
        if (dvObject instanceof Dataset) {
            d = (Dataset) dvObject;
        } else if (dvObject instanceof DataFile) {
            d = ((DataFile) dvObject).getOwner();
        }
        if (d != null) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            String version = dv.getFriendlyVersionNumber();
            if (StringUtils.isNotBlank(version)) {
                XmlWriterUtil.writeFullElement(xmlw, "version", version);
            }
        }

    }

    private void writeAccessRights(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // rightsList -> rights with rightsURI attribute
        xmlw.writeStartElement("rightsList"); // <rightsList>

        // set terms from the info:eu-repo-Access-Terms vocabulary
        xmlw.writeStartElement("rights"); // <rights>
        DatasetVersion dv = null;
        boolean closed = false;
        if (dvObject instanceof Dataset d) {
            dv = d.getLatestVersionForCopy();
            closed = dv.isHasRestrictedFile();
        } else if (dvObject instanceof DataFile df) {
            dv = df.getOwner().getLatestVersionForCopy();

            closed = df.isRestricted();
        }
        TermsOfUseAndAccess terms = dv.getTermsOfUseAndAccess();
        boolean requestsAllowed = terms.isFileAccessRequest();
        License license = terms.getLicense();

        if (requestsAllowed && closed) {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/restrictedAccess");
        } else if (!requestsAllowed && closed) {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/closedAccess");
        } else {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/openAccess");
        }
        xmlw.writeEndElement(); // </rights>
        xmlw.writeStartElement("rights"); // <rights>
        
        if (license != null) {
            xmlw.writeAttribute("rightsURI", license.getUri().toString());
            String label = license.getShortDescription();
            if(StringUtils.isBlank(label)) {
                //Use name as a backup in case the license has no short description
                label = license.getName();
            }
            
            if (license.getRightsIdentifier() != null) {
                xmlw.writeAttribute("rightsIdentifier", license.getRightsIdentifier());
            }
            if (license.getRightsIdentifierScheme() != null) {
                xmlw.writeAttribute("rightsIdentifierScheme", license.getRightsIdentifierScheme());
            }
            if (license.getSchemeUri() != null) {
                xmlw.writeAttribute("schemeURI", license.getSchemeUri());
            }
            String langCode = license.getLanguageCode();
            if (StringUtils.isBlank(langCode)) {
                langCode = "en";
            }
            xmlw.writeAttribute("xml:lang", langCode);
            xmlw.writeCharacters(license.getShortDescription());

        } else {
            xmlw.writeAttribute("rightsURI", DatasetUtil.getLicenseURI(dv));
            xmlw.writeCharacters(BundleUtil.getStringFromBundle("license.custom.description"));
            ;
        }
        xmlw.writeEndElement(); // </rights>
        xmlw.writeEndElement(); // </rightsList>
    }

    private void writeDescriptions(XMLStreamWriter xmlw, DvObject dvObject, boolean deaccessioned) throws XMLStreamException {
        // descriptions -> description with descriptionType attribute
        boolean descriptionsWritten = false;
        List<String> descriptions = null;
        DatasetVersion dv = null;
        if(deaccessioned) {
            descriptions = new ArrayList<String>();
            descriptions.add(AbstractDOIProvider.UNAVAILABLE);
        } else {
            if (dvObject instanceof Dataset d) {
                dv = d.getLatestVersionForCopy();
                descriptions = dv.getDescriptions();
            } else if (dvObject instanceof DataFile df) {
                String description = df.getDescription();
                if (description != null) {
                    descriptions = new ArrayList<String>();
                    descriptions.add(description);
                }
            }
        }
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("descriptionType", "Abstract");
        if (descriptions != null) {
            for (String description : descriptions) {
                descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, StringEscapeUtils.escapeXml10(description));
            }
        }

        if (dv != null) {
            List<DatasetField> dsfs = dv.getDatasetFields();

            for (DatasetField dsf : dsfs) {

                switch (dsf.getDatasetFieldType().getName()) {
                    case DatasetFieldConstant.software:
                        attributes.clear();
                        attributes.put("descriptionType", "TechnicalInfo");
                        List<DatasetFieldCompoundValue> dsfcvs = dsf.getDatasetFieldCompoundValues();
                        for (DatasetFieldCompoundValue dsfcv : dsfcvs) {

                            String softwareName = null;
                            String softwareVersion = null;
                            List<DatasetField> childDsfs = dsfcv.getChildDatasetFields();
                            for (DatasetField childDsf : childDsfs) {
                                if (DatasetFieldConstant.softwareName.equals(childDsf.getDatasetFieldType().getName())) {
                                    softwareName = childDsf.getValue();
                                } else if (DatasetFieldConstant.softwareVersion.equals(childDsf.getDatasetFieldType().getName())) {
                                    softwareVersion = childDsf.getValue();
                                }
                            }
                            if (StringUtils.isNotBlank(softwareName)) {
                                if (StringUtils.isNotBlank(softwareVersion)) {
                                    softwareName = softwareName + ", " + softwareVersion;
                                }
                                descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, softwareName);
                            }
                        }
                        break;
                    case DatasetFieldConstant.originOfSources:
                    case DatasetFieldConstant.characteristicOfSources:
                    case DatasetFieldConstant.accessToSources:
                        attributes.clear();
                        attributes.put("descriptionType", "Methods");
                        String method = dsf.getValue();
                        if (StringUtils.isNotBlank(method)) {
                            descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, method);

                        }
                        break;
                    case DatasetFieldConstant.series:
                        attributes.clear();
                        attributes.put("descriptionType", "SeriesInformation");
                        dsfcvs = dsf.getDatasetFieldCompoundValues();
                        for (DatasetFieldCompoundValue dsfcv : dsfcvs) {
                            List<DatasetField> childDsfs = dsfcv.getChildDatasetFields();
                            for (DatasetField childDsf : childDsfs) {

                                if (DatasetFieldConstant.seriesName.equals(childDsf.getDatasetFieldType().getName())) {
                                    String seriesInformation = childDsf.getValue();
                                    if (StringUtils.isNotBlank(seriesInformation)) {
                                        descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                                        XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, seriesInformation);
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    case DatasetFieldConstant.notesText:
                        attributes.clear();
                        attributes.put("descriptionType", "Other");
                        String notesText = dsf.getValue();
                        if (StringUtils.isNotBlank(notesText)) {
                            descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, notesText);
                        }
                        break;

                }
            }
            String versionNote = dv.getVersionNote();
            if(!StringUtils.isBlank(versionNote)) {
                attributes.clear();
                attributes.put("descriptionType", "TechnicalInfo");
                descriptionsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "descriptions", descriptionsWritten);
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "description", attributes, versionNote);
            }
        }

        if (descriptionsWritten) {
            xmlw.writeEndElement(); // </descriptions>
        }
    }

    private void writeGeoLocations(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        if (dvObject instanceof Dataset d) {
            boolean geoLocationsWritten = false;
            DatasetVersion dv = d.getLatestVersionForCopy();

            List<String[]> places = dv.getGeographicCoverage();
            if (places != null && !places.isEmpty()) {
                // geoLocationPlace
                geoLocationsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "geoLocations", geoLocationsWritten);
                for (String[] place : places) {
                    xmlw.writeStartElement("geoLocation"); // <geoLocation>
                    
                    ArrayList<String> placeList = new ArrayList<String>();
                    for (String placePart : place) {
                        if (!StringUtils.isBlank(placePart)) {
                            placeList.add(placePart);
                        }
                    }
                    XmlWriterUtil.writeFullElement(xmlw, "geoLocationPlace", Strings.join(placeList, ", "));
                    xmlw.writeEndElement(); // </geoLocation>
                }
                
            }
            boolean boundingBoxFound = false;
            boolean productionPlaceFound = false;
            for (DatasetField dsf : dv.getDatasetFields()) {
                switch (dsf.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.geographicBoundingBox:
                    boundingBoxFound = true;
                    for (DatasetFieldCompoundValue dsfcv : dsf.getDatasetFieldCompoundValues()) {
                        List<DatasetField> childDsfs = dsfcv.getChildDatasetFields();
                        String nLatitude = null;
                        String sLatitude = null;
                        String eLongitude = null;
                        String wLongitude = null;
                        for (DatasetField childDsf : childDsfs) {
                            switch (childDsf.getDatasetFieldType().getName()) {
                            case DatasetFieldConstant.northLatitude:
                                nLatitude = childDsf.getValue();
                                break;
                            case DatasetFieldConstant.southLatitude:
                                sLatitude = childDsf.getValue();
                                break;
                            case DatasetFieldConstant.eastLongitude:
                                eLongitude = childDsf.getValue();
                                break;
                            case DatasetFieldConstant.westLongitude:
                                wLongitude = childDsf.getValue();

                            }
                        }
                        if (StringUtils.isNoneBlank(wLongitude, eLongitude, nLatitude, sLatitude)) {
                            geoLocationsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "geoLocations", geoLocationsWritten);
                            xmlw.writeStartElement("geoLocation"); // <geoLocation>
                            if (wLongitude.equals(eLongitude) && nLatitude.equals(sLatitude)) {
                                // A point
                                xmlw.writeStartElement("geoLocationPoint");
                                XmlWriterUtil.writeFullElement(xmlw, "pointLongitude", eLongitude);
                                XmlWriterUtil.writeFullElement(xmlw, "pointLatitude", sLatitude);
                                xmlw.writeEndElement();
                            } else {
                                // A box
                                xmlw.writeStartElement("geoLocationBox");
                                XmlWriterUtil.writeFullElement(xmlw, "westBoundLongitude", wLongitude);
                                XmlWriterUtil.writeFullElement(xmlw, "eastBoundLongitude", eLongitude);
                                XmlWriterUtil.writeFullElement(xmlw, "southBoundLatitude", sLatitude);
                                XmlWriterUtil.writeFullElement(xmlw, "northBoundLatitude", nLatitude);
                                xmlw.writeEndElement();

                            }
                            xmlw.writeEndElement(); // </geoLocation>
                        }
                    }
                case DatasetFieldConstant.productionPlace:
                    productionPlaceFound = true;
                    // geoLocationPlace
                    geoLocationsWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "geoLocations", geoLocationsWritten);
                    List<String> prodPlaces = dsf.getValues();
                    for (String prodPlace : prodPlaces) {
                        xmlw.writeStartElement("geoLocation"); // <geoLocation>
                        XmlWriterUtil.writeFullElement(xmlw, "geoLocationPlace", prodPlace);
                        xmlw.writeEndElement(); // </geoLocation>
                    }
                    break;
                }
                if (boundingBoxFound && productionPlaceFound) {
                    break;
                }
            }
            if (geoLocationsWritten) {
                xmlw.writeEndElement(); // <geoLocations>
            }
        }

    }

    private void writeFundingReferences(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // fundingReferences -> fundingReference -> funderName, awardNumber
        boolean fundingReferenceWritten = false;
        DatasetVersion dv = null;
        if (dvObject instanceof Dataset d) {
            dv = d.getLatestVersionForCopy();
        } else if (dvObject instanceof DataFile df) {
            dv = df.getOwner().getLatestVersionForCopy();
        }
        if (dv != null) {
            List<String> retList = new ArrayList<>();
            for (DatasetField dsf : dv.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributor)) {
                    boolean addFunder = false;
                    for (DatasetFieldCompoundValue contributorValue : dsf.getDatasetFieldCompoundValues()) {
                        String contributorName = null;
                        String contributorType = null;
                        for (DatasetField subField : contributorValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributorName)) {
                                contributorName = subField.getDisplayValue();
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributorType)) {
                                contributorType = subField.getRawValue();
                            }
                        }
                        // SEK 02/12/2019 move outside loop to prevent contrib type to carry over to
                        // next contributor
                        // TODO: Consider how this will work in French, Chinese, etc.
                        if ("Funder".equals(contributorType)) {
                            if (!StringUtils.isBlank(contributorName)) {
                                fundingReferenceWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "fundingReferences", fundingReferenceWritten);
                                xmlw.writeStartElement("fundingReference"); // <fundingReference>
                                XmlWriterUtil.writeFullElement(xmlw, "funderName", StringEscapeUtils.escapeXml10(contributorName));
                                xmlw.writeEndElement(); // </fundingReference>
                            }
                        }
                    }
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.grantNumber)) {
                    for (DatasetFieldCompoundValue grantObject : dsf.getDatasetFieldCompoundValues()) {
                        String funder = null;
                        String awardNumber = null;
                        for (DatasetField subField : grantObject.getChildDatasetFields()) {
                            // It would be nice to do something with grantNumberValue (the actual number)
                            // but schema.org doesn't support it.
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.grantNumberAgency)) {
                                String grantAgency = subField.getDisplayValue();
                                funder = grantAgency;
                            } else if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.grantNumberValue)) {
                                String grantNumberValue = subField.getDisplayValue();
                                awardNumber = grantNumberValue;
                            }
                        }
                        if (!StringUtils.isBlank(funder)) {
                            fundingReferenceWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "fundingReferences", fundingReferenceWritten);
                            boolean isROR=false;
                            String funderIdentifier = null;
                            ExternalIdentifier externalIdentifier = ExternalIdentifier.ROR;
                            if (externalIdentifier.isValidIdentifier(funder)) {
                                isROR = true;
                                JsonObject jo = getExternalVocabularyValue(funder);
                                if (jo != null) {
                                    funderIdentifier = funder;
                                    funder = jo.getString("termName");
                                }
                            }
                          
                            xmlw.writeStartElement("fundingReference"); // <fundingReference>
                            XmlWriterUtil.writeFullElement(xmlw, "funderName", StringEscapeUtils.escapeXml10(funder));
                            if (isROR) {
                                Map<String, String> attributeMap = new HashMap<>();
                                attributeMap.put("schemeURI", "https://ror.org");
                                attributeMap.put("funderIdentifierType", "ROR");
                                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "funderIdentifier", attributeMap, StringEscapeUtils.escapeXml10(funderIdentifier));
                            }
                            if (StringUtils.isNotBlank(awardNumber)) {
                                XmlWriterUtil.writeFullElement(xmlw, "awardNumber", StringEscapeUtils.escapeXml10(awardNumber));
                            }
                            xmlw.writeEndElement(); // </fundingReference>
                        }

                    }
                }
            }

            if (fundingReferenceWritten) {
                xmlw.writeEndElement(); // </fundingReferences>
            }

        }
    }
}