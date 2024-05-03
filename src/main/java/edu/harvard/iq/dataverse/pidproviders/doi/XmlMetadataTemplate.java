package edu.harvard.iq.dataverse.pidproviders.doi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.export.DDIExporter;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
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
        // identifier with identifierType attribute
        Map<String, String> identifier_map = new HashMap<String, String>();
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
        List<String> subjects = null;
        List<DatasetFieldCompoundValue> compoundKeywords = null;
        List<DatasetFieldCompoundValue> compoundTopics = null;
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
        List<DatasetFieldCompoundValue> compoundProducers = null;
        List<DatasetFieldCompoundValue> compoundDistributors = null;
        List<DatasetFieldCompoundValue> compoundContacts = null;
        List<DatasetFieldCompoundValue> compoundContributors = null;
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

    private String generateRelatedIdentifiers(DvObject dvObject) {

        StringBuilder sb = new StringBuilder();
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;

            List<DatasetRelPublication> relatedPublications = dataset.getLatestVersionForCopy().getRelatedPublications();
            if (!relatedPublications.isEmpty()) {
                for (DatasetRelPublication relatedPub : relatedPublications) {
                    String pubIdType = relatedPub.getIdType();
                    String identifier = relatedPub.getIdNumber();
                    /*
                     * Note - with identifier and url fields, it's not clear that there's a single
                     * way those two fields are used for all identifier types In QDR, at this time,
                     * doi and isbn types always have the raw number in the identifier field,
                     * whereas there are examples where URLs are in the identifier or url fields.
                     * The code here addresses those practices and is not generic.
                     */
                    if (pubIdType != null) {
                        switch (pubIdType) {
                        case "doi":
                            if (identifier != null && identifier.length() != 0) {
                                appendIdentifier(sb, "DOI", "IsSupplementTo", "doi:" + identifier);
                            }
                            break;
                        case "isbn":
                            if (identifier != null && identifier.length() != 0) {
                                appendIdentifier(sb, "ISBN", "IsSupplementTo", "ISBN:" + identifier);
                            }
                            break;
                        case "url":
                            if (identifier != null && identifier.length() != 0) {
                                appendIdentifier(sb, "URL", "IsSupplementTo", identifier);
                            } else {
                                String pubUrl = relatedPub.getUrl();
                                if (pubUrl != null && pubUrl.length() > 0) {
                                    appendIdentifier(sb, "URL", "IsSupplementTo", pubUrl);
                                }
                            }
                            break;
                        default:
                            if (identifier != null && identifier.length() != 0) {
                                if (pubIdType.equalsIgnoreCase("arXiv")) {
                                    pubIdType = "arXiv";
                                } else if (pubIdType.equalsIgnoreCase("handle")) {
                                    // Initial cap required for handle
                                    pubIdType = "Handle";
                                } else if (!pubIdType.equals("bibcode")) {
                                    pubIdType = pubIdType.toUpperCase();
                                }
                                // For all others, do a generic attempt to match the identifier type to the
                                // datacite schema and send the raw identifier as the value
                                appendIdentifier(sb, pubIdType, "IsSupplementTo", identifier);
                            }
                            break;
                        }

                    } else {
                        logger.info(relatedPub.getIdNumber() + relatedPub.getUrl() + relatedPub.getTitle());
                    }
                }
            }

            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                List<String> datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    if (dataFile.getGlobalId() != null) {
                        if (sb.toString().isEmpty()) {
                            sb.append("<relatedIdentifiers>");
                        }
                        sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"HasPart\">"
                                + dataFile.getGlobalId() + "</relatedIdentifier>");
                    }
                }

                if (!sb.toString().isEmpty()) {
                    sb.append("</relatedIdentifiers>");
                }
            }
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            appendIdentifier(sb, "DOI", "IsPartOf", df.getOwner().getGlobalId().asString());
            if (sb.length() != 0) {
                // Should always be true
                sb.append("</relatedIdentifiers>");
            }
        }
        return sb.toString();
    }


    private void appendIdentifier(StringBuilder sb, String idType, String relationType, String identifier) {
        if (sb.toString().isEmpty()) {
            sb.append("<relatedIdentifiers>");
        }
        sb.append("<relatedIdentifier relatedIdentifierType=\"" + idType + "\" relationType=\"" + relationType + "\">" + identifier + "</relatedIdentifier>");
    }

    public void generateFileIdentifiers(DvObject dvObject) {

        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;

            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                List<String> datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    datafileIdentifiers.add(dataFile.getIdentifier());
                    // int x = xmlMetadata.indexOf("</relatedIdentifiers>") - 1;
                    // xmlMetadata = xmlMetadata.replace("{relatedIdentifier}",
                    // dataFile.getIdentifier());
                    // xmlMetadata = xmlMetadata.substring(0, x) + "<relatedIdentifier
                    // relatedIdentifierType=\"hasPart\" "
                    // + "relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>"
                    // + template.substring(x, template.length() - 1);

                }

            } else {
                // xmlMetadata = xmlMetadata.replace(
                // "<relatedIdentifier relatedIdentifierType=\"hasPart\"
                // relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>",
                // "");
            }
        }
    }

}