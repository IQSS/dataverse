package edu.harvard.iq.dataverse.pidproviders.doi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ocpsoft.common.util.Strings;

import edu.harvard.iq.dataverse.AlternativePersistentIdentifier;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.export.DDIExporter;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.util.PersonOrOrgUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import edu.harvard.iq.dataverse.util.xml.XmlWriterUtil;
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
        String metadataLanguage = null; // when set, otherwise  = language?
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("resource");
        xmlw.writeDefaultNamespace(XML_NAMESPACE);
        xmlw.writeAttribute("xmlns:xsi", XML_XSI);
        xmlw.writeAttribute("xsi:schemaLocation", XML_SCHEMA_LOCATION);

        writeIdentifier(xmlw, dvObject);
        writeCreators(xmlw, doiMetadata.getAuthors());
        writeTitles(xmlw, dvObject, language);
        writePublisher(xmlw, dvObject);
        writePublicationYear(xmlw, dvObject);
        writeSubjects(xmlw, dvObject);
        writeContributors(xmlw, dvObject);
        writeDates(xmlw, dvObject);
        writeLanguage(xmlw, dvObject);
        writeResourceType(xmlw, dvObject);
        writeAlternateIdentifiers(xmlw, dvObject);
        writeRelatedIdentifiers(xmlw, dvObject);
        writeSize(xmlw, dvObject);
        writeFormats(xmlw, dvObject);
        writeVersion(xmlw, dvObject);
        writeAccessRights(xmlw, dvObject);
        writeDescriptions(xmlw, dvObject);
        writeGeoLocations(xmlw, dvObject);
        writeFundingReferences(xmlw, dvObject);

        StringBuilder contributorsElement = new StringBuilder();
        if (doiMetadata.getContacts() != null) {
            for (String[] contact : doiMetadata.getContacts()) {
                if (!contact[0].isEmpty()) {
                    contributorsElement.append("<contributor contributorType=\"ContactPerson\"><contributorName>"
                            + StringEscapeUtils.escapeXml10(contact[0]) + "</contributorName>");
                    if (!contact[1].isEmpty()) {
                        contributorsElement.append("<affiliation>" + StringEscapeUtils.escapeXml10(contact[1]) + "</affiliation>");
                    }
                    contributorsElement.append("</contributor>");
                }
            }
        }

        if (doiMetadata.getProducers() != null) {
            for (String[] producer : doiMetadata.getProducers()) {
                contributorsElement.append("<contributor contributorType=\"Producer\"><contributorName>" + StringEscapeUtils.escapeXml10(producer[0])
                        + "</contributorName>");
                if (!producer[1].isEmpty()) {
                    contributorsElement.append("<affiliation>" + StringEscapeUtils.escapeXml10(producer[1]) + "</affiliation>");
                }
                contributorsElement.append("</contributor>");
            }
        }

        String relIdentifiers = generateRelatedIdentifiers(dvObject);

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
    private void writeTitles(XMLStreamWriter xmlw, DvObject dvObject, String language) throws XMLStreamException {
        String title = doiMetadata.getTitle();
        String subTitle = null;
        List<String> altTitles = null;
        // Only Datasets can have a subtitle or alternative titles
        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersion();
            Optional<DatasetField> subTitleField = dv.getDatasetFields().stream().filter(f -> f.getDatasetFieldType().getName().equals(DatasetFieldConstant.subTitle)).findFirst();
            if (subTitleField.isPresent()) {
                subTitle = subTitleField.get().getValue();
            }
            Optional<DatasetField> altTitleField = dv.getDatasetFields().stream().filter(f -> f.getDatasetFieldType().getName().equals(DatasetFieldConstant.alternativeTitle)).findFirst();
            if (altTitleField.isPresent()) {
                altTitles = altTitleField.get().getValues();
            }
        }

        if (StringUtils.isNotBlank(title) || StringUtils.isNotBlank(subTitle) || (altTitles != null && !String.join("", altTitles).isBlank())) {
            xmlw.writeStartElement("titles");
            XmlWriterUtil.writeFullElement(xmlw, "title", title, language);

            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("titleType", "Subtitle");

            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "title", attributes, title);

            attributes.clear();
            attributes.put("titleType", "AlternativeTitle");

            for (String altTitle : altTitles) {
                XmlWriterUtil.writeFullElementWithAttributes(xmlw, "title", attributes, altTitle);
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
    public void writeCreators(XMLStreamWriter xmlw, List<DatasetAuthor> authorList) throws XMLStreamException {
        // creators -> creator -> creatorName with nameType attribute, givenName,
        // familyName, nameIdentifier
        // write all creators
        xmlw.writeStartElement("creators"); // <creators>

        if (authorList != null && !authorList.isEmpty()) {
            for (DatasetAuthor author : authorList) {
                String creatorName = StringEscapeUtils.escapeXml10(author.getName().getDisplayValue());
                String affiliation = null;
                if (author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
                    affiliation = StringEscapeUtils.escapeXml10(author.getAffiliation().getDisplayValue());
                }
                String nameIdentifier = null;
                String nameIdentifierScheme = null;
                if (StringUtils.isNotBlank(author.getIdValue()) && StringUtils.isNotBlank(author.getIdType())) {
                    nameIdentifier = author.getIdValue();
                    if(nameIdentifier != null) {
                        // Normalizes to the URL form of the identifier, returns null if the identifier
                        // is not valid given the type
                        nameIdentifier = author.getIdentifierAsUrl();
                    }
                    nameIdentifierScheme = author.getIdType();
                }

                if (StringUtils.isNotBlank(creatorName)) {
                    xmlw.writeStartElement("creator"); // <creator>
                    JsonObject creatorObj = PersonOrOrgUtil.getPersonOrOrganization(creatorName, false,
                            StringUtils.containsIgnoreCase(nameIdentifierScheme, "orcid"));

                    writeEntityElements(xmlw, "creator", null, creatorObj, affiliation, nameIdentifier, nameIdentifierScheme);
                    xmlw.writeEndElement(); // </creator>
                }

                else {
                    // Authors unavailable
                    XmlWriterUtil.writeFullElement(xmlw, "creator", "creatorName", AbstractPidProvider.UNAVAILABLE);
                }
            }
        }
        xmlw.writeEndElement(); // </creators>
    }

    private void writePublisher(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // publisher should already be non null - :unav if it wasn't available
        XmlWriterUtil.writeFullElement(xmlw, "publisher", doiMetadata.getPublisher());
    }

    private void writePublicationYear(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        // Can't use "UNKNOWN" here because DataCite will respond with "[facet
        // 'pattern'] the value 'unknown' is not accepted by the pattern '[\d]{4}'"
        String pubYear = "9999";
        // FIXME: Investigate why this.publisherYear is sometimes null now that pull
        // request #4606 has been merged.
        if (doiMetadata.getPublisherYear() != null) {
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
            dv.getDatasetSubjects();
            for (DatasetField dsf : dv.getDatasetFields()) {
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
                case DatasetFieldConstant.keyword:
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
     * 7, Contributor (with optional given name, family name, name identifier
     * and affiliation sub-properties)
     *
     * @see #writeContributorElement(javax.xml.stream.XMLStreamWriter,
     * java.lang.String, java.lang.String, java.lang.String)
     *
     * @param xmlw The stream writer
     * @param dvObject The Dataset/DataFile
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
        //ToDo Include for files?
        /*if(dvObject instanceof DataFile df) {
            dvObject = df.getOwner();
        }*/
    
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
                case DatasetFieldConstant.contact:
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
                    contributorType = subField.getValue().replace(" ", "");
                    break;
                }
            }
            // QDR - doesn't have Funder in the contributor type list. 
            // Using a string isn't i18n
            if (StringUtils.isNotBlank(contributor) && !StringUtils.equalsIgnoreCase("Funder", contributorType)) {
                contributorsCreated = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "contributors", contributorsCreated);
                JsonObject entityObject = PersonOrOrgUtil.getPersonOrOrganization(contributor, false, false);
                writeEntityElements(xmlw, "contributor", contributorType, entityObject, null, null, null);
            }

        }
        
        if (contributorsCreated) {
            xmlw.writeEndElement();
        }
    }

    private void writeEntityElements(XMLStreamWriter xmlw, String elementName, String type, JsonObject entityObject, String affiliation, String nameIdentifier, String nameIdentifierScheme) throws XMLStreamException {
        xmlw.writeStartElement(elementName);
        Map<String, String> attributeMap = new HashMap<String, String>();
        if (StringUtils.isNotBlank(type)) {
            attributeMap.put("contributorType", type);
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
            if (affiliation.startsWith("https://ror.org/")) {

                attributeMap.put("schemeURI", "https://ror.org");
                attributeMap.put("affiliationIdentifierScheme", "ROR");
            }
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "affiliation", attributeMap, StringEscapeUtils.escapeXml10(affiliation));
        }
        xmlw.writeEndElement();
    }

    /**
     * 8, Date (with type sub-property) (R)
     *
     * @param xmlw The Steam writer
     * @param dvObject The dataset/datafile
     * @throws XMLStreamException
     */
    private void writeDates(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        boolean datesWritten = false;
        String dateOfDistribution = null;
        String dateOfProduction = null;
        String dateOfDeposit = null;
        Date releaseDate = null;
        List<DatasetFieldCompoundValue> datesOfCollection = new ArrayList<DatasetFieldCompoundValue>();

        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            releaseDate = dv.getReleaseTime();
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

        if (releaseDate != null) {
            String date = Util.getDateTimeFormat().format(releaseDate);
            datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);

            attributes.put("dateType", "Available");
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
                if (StringUtils.isNotBlank(startDate) || StringUtils.isNotBlank(endDate)) {
                    datesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "dates", datesWritten);
                    attributes.put("dateType", "Collected");
                    XmlWriterUtil.writeFullElementWithAttributes(xmlw, "date", attributes, (startDate + "/" + endDate).trim());
                }
            }
        }
        if (datesWritten) {
            xmlw.writeEndElement();
        }
    }


    // 9, Language (MA), language
    private void writeLanguage(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        //Currently not supported. Spec indicates one 'primary' language. Could send the first entry in DatasetFieldConstant.language or send iff there is only one entry, and/or default to the machine's default lang?
        return;
    }
    
    // 10, ResourceType (with mandatory general type 
    //      description sub- property) (M)
    private void writeResourceType(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        List<ControlledVocabularyValue> kindOfDataValues = new ArrayList<ControlledVocabularyValue>();
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put("resourceTypeGeneral", "Dataset");
        if (dvObject instanceof Dataset d) {
            DatasetVersion dv = d.getLatestVersionForCopy();
            for (DatasetField dsf : dv.getDatasetFields()) {
                switch (dsf.getDatasetFieldType().getName()) {
                case DatasetFieldConstant.kindOfData:
                    kindOfDataValues = dsf.getControlledVocabularyValues();
                    break;
                }

                if (kindOfDataValues.isEmpty()) {
                    // Write an attribute only element if there are no kindOfData values.
                    xmlw.writeStartElement("resourceType");
                    xmlw.writeAttribute("resourceTypeGeneral", attributes.get("resourceTypeGeneral"));
                    xmlw.writeEndElement();
                } else {
                    for (ControlledVocabularyValue kindOfDataValue : kindOfDataValues) {
                        String resourceType = kindOfDataValue.getStrValue();
                        if (StringUtils.isNotBlank(resourceType)) {
                            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "resourceType", attributes, resourceType);
                        }
                    }
                }
            }
        }
    }

    /**
     * 11 AlternateIdentifier (with type sub-property) (O)
     *
     * @param xmlw The Steam writer
     * @param dvObject The dataset/datafile
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
        if (!altPids.isEmpty()) {
            alternatesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "alternativeIdentifiers", alternatesWritten);
        }
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
            attributes.put("alternativeIdentifierType", identifierType);
            XmlWriterUtil.writeFullElementWithAttributes(xmlw, "alternateIdentifier", attributes, identifier);

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
            attributes.put("alternativeIdentifierType", identifierType);
            if (!StringUtils.isBlank(identifier)) {
                alternatesWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "alternativeIdentifiers", alternatesWritten);

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
     * @param xmlw The Steam writer
     * @param dvObject the dataset/datafile
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
                    /*
                     * Note - with identifier and url fields, it's not clear that there's a single
                     * way those two fields are used for all identifier types. The code here is
                     * ~best effort to interpret those fields.
                     */
                    pubIdType = getCanonicalPublicationType(pubIdType);

                    // Prefer url if set, otherwise check identifier
                    String relatedIdentifier = url;
                    if (StringUtils.isBlank(relatedIdentifier)) {
                        relatedIdentifier = identifier;
                    }
                    // For types where we understand the protocol, get the canonical form
                    switch (pubIdType) {
                    case "DOI":
                        if (!relatedIdentifier.startsWith("doi:") || relatedIdentifier.startsWith("http")) {
                            relatedIdentifier = "doi:" + relatedIdentifier;
                        }
                        try {
                            GlobalId pid = PidUtil.parseAsGlobalID(relatedIdentifier);
                            relatedIdentifier = pid.asRawIdentifier();
                        } catch (IllegalArgumentException e) {
                            relatedIdentifier = null;
                        }
                        break;
                    case "Handle":
                        if (!relatedIdentifier.startsWith("hdl:") || relatedIdentifier.startsWith("http")) {
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
                        break;
                    default:

                        // For non-URL types, if a URL is given, split the string to get a schemeUri
                        try {
                            URL relatedUrl = new URL(relatedIdentifier);
                            String protocol = relatedUrl.getProtocol();
                            String authority = relatedUrl.getAuthority();
                            String site = String.format("%s://%s", protocol, authority);
                            relatedIdentifier = relatedIdentifier.substring(site.length());
                            attributes.put("schemeURI", site);
                        } catch (MalformedURLException e) {
                            // Just an identifier
                        }
                    }

                    if (StringUtils.isNotBlank(relatedIdentifier)) {
                        // Still have a valid entry
                        attributes.put("relatedIdentifierType", pubIdType);
                        attributes.put("relationType", "IsSupplementTo");
                        relatedIdentifiersWritten = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "relatedIdentifiers", relatedIdentifiersWritten);
                        XmlWriterUtil.writeFullElementWithAttributes(xmlw, "relatedIdentifier", attributes, relatedIdentifier);
                    }
                }
            }
            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {
                attributes.clear();
                attributes.put("relationType", "HasPart");
                for (DataFile dataFile : dataset.getFiles()) {
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
            // Add entry for Handle protocol so this can be used with GlobalId/getProtocol()
            relatedIdentifierTypeMap.put("hdl".toLowerCase(), "Handle");
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
                /* Should original formats be sent? What about original sizes above?
                if(dataFile.isTabularData()) {
                    String originalFormat = dataFile.getOriginalFileFormat();
                    if(StringUtils.isNotBlank(originalFormat)) {
                        XmlWriterUtil.writeFullElement(xmlw, "format", format);
                    }
                }*/
            }
        }
        if (formatsWritten) {
            xmlw.writeEndElement();
        }

    }

    private void writeVersion(XMLStreamWriter xmlw, DvObject dvObject) throws XMLStreamException {
        Dataset d = null;
        if(dvObject instanceof Dataset) {
            d = (Dataset) dvObject;
        } else if (dvObject instanceof DataFile) {
            d = ((DataFile) dvObject).getOwner();
        }
        if(d !=null) {
            DatasetVersion dv = d.getLatestVersionForCopy();
        String version = dv.getFriendlyVersionNumber();
            if (StringUtils.isNotBlank(version)) {
                XmlWriterUtil.writeFullElement(xmlw, "version", version);
            }
        }
        
    }

    private void writeAccessRights(XMLStreamWriter xmlw, DvObject dvObject) {
        // rightsList -> rights with rightsURI attribute
        xmlw.writeStartElement("rightsList"); // <rightsList>

        // set terms from the info:eu-repo-Access-Terms vocabulary
        writeRightsHeader(xmlw, language);
        boolean restrict = false;
        boolean closed = false;

        if (datasetVersionDTO.isFileAccessRequest()) {
            restrict = true;
        }
        if (datasetVersionDTO.getFiles() != null) {
            for (int i = 0; i < datasetVersionDTO.getFiles().size(); i++) {
                if (datasetVersionDTO.getFiles().get(i).isRestricted()) {
                    closed = true;
                    break;
                }
            }
        }

        if (restrict && closed) {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/restrictedAccess");
        } else if (!restrict && closed) {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/closedAccess");
        } else {
            xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/openAccess");
        }
        xmlw.writeEndElement(); // </rights>

        writeRightsHeader(xmlw, language);
        if (datasetVersionDTO.getLicense() != null) {
            xmlw.writeAttribute("rightsURI", datasetVersionDTO.getLicense().getUri());
            xmlw.writeCharacters(datasetVersionDTO.getLicense().getName());
        }
        xmlw.writeEndElement(); // </rights>
        xmlw.writeEndElement(); // </rightsList>        
    }
}