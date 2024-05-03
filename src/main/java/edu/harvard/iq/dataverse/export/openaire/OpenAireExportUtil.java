package edu.harvard.iq.dataverse.export.openaire;

import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

import jakarta.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.util.PersonOrOrgUtil;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public class OpenAireExportUtil {

    private static final Logger logger = Logger.getLogger(OpenAireExportUtil.class.getCanonicalName());

    public static String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    public static String SCHEMA_VERSION = "4.1";
    public static String RESOURCE_NAMESPACE = "http://datacite.org/schema/kernel-4";
    public static String RESOURCE_SCHEMA_LOCATION = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";

    public static String FunderType = "Funder";

    public static void datasetJson2openaire(JsonObject datasetDtoAsJson, OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);

        dto2openaire(datasetDto, outputStream);
    }

    private static void dto2openaire(DatasetDTO datasetDto, OutputStream outputStream) throws XMLStreamException {
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);

        xmlw.writeStartElement("resource"); // <resource>

        xmlw.writeAttribute("xmlns:xsi", XSI_NAMESPACE);
        xmlw.writeAttribute("xmlns", RESOURCE_NAMESPACE);
        xmlw.writeAttribute("xsi:schemaLocation", RESOURCE_NAMESPACE + " " + RESOURCE_SCHEMA_LOCATION);

        createOpenAire(xmlw, datasetDto);

        xmlw.writeEndElement(); // </resource>

        xmlw.flush();
    }

    private static void createOpenAire(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        GlobalId globalId = PidUtil.parseAsGlobalID(persistentAgency, persistentAuthority, persistentId);

        // The sequence is revied using sample:
        // https://schema.datacite.org/meta/kernel-4.0/example/datacite-example-full-v4.0.xml
        //
        // See also: https://schema.datacite.org/meta/kernel-4.0/doc/DataCite-MetadataKernel_v4.0.pdf
        // Table 1: DataCite Mandatory Properties
        // set language 
        //String language = getLanguage(xmlw, version);
        String language = null;

        // 1, Identifier (with mandatory type sub-property) (M)
        writeIdentifierElement(xmlw, globalId.asURL(), language);

        // 2, Creator (with optional given name, family name, 
        //      name identifier and affiliation sub-properties) (M)
        writeCreatorsElement(xmlw, version, language);

        // 3, Title (with optional type sub-properties)
        writeTitlesElement(xmlw, version, language);

        // 4, Publisher (M)
        String publisher = datasetDto.getPublisher();
        if (StringUtils.isNotBlank(publisher)) {
            writeFullElement(xmlw, null, "publisher", null, publisher, language);
        }

        // 5, PublicationYear (M)
        String publicationDate = datasetDto.getPublicationDate();
        writePublicationYearElement(xmlw, version, publicationDate, language);

        // 6, Subject (with scheme sub-property)
        writeSubjectsElement(xmlw, version, language);

        // 7, Contributor (with optional given name, family name, 
        //      name identifier and affiliation sub-properties)
        writeContributorsElement(xmlw, version, language);

        // 8, Date (with type sub-property)  (R)
        writeDatesElement(xmlw, version, language);

        // 9, Language (MA), language
        writeFullElement(xmlw, null, "language", null, language, null);

        // 10, ResourceType (with mandatory general type 
        //      description sub- property) (M)
        writeResourceTypeElement(xmlw, version, language);

        // 11. AlternateIdentifier (with type sub-property) (O)
        writeAlternateIdentifierElement(xmlw, version, language);

        // 12, RelatedIdentifier (with type and relation type sub-properties) (R)
        writeRelatedIdentifierElement(xmlw, version, language);

        // 13, Size (O)
        writeSizeElement(xmlw, version, language);

        // 14 Format (O)
        writeFormatElement(xmlw, version, language);

        // 15 Version (O)
        writeVersionElement(xmlw, version, language);

        // 16 Rights (O), rights
        writeAccessRightsElement(xmlw, version/*, version.getTermsOfAccess(), version.getRestrictions()*/, language);

        // 17 Description (R), description
        writeDescriptionsElement(xmlw, version, language);

        // 18 GeoLocation (with point, box and polygon sub-properties) (R)
        writeGeoLocationsElement(xmlw, version, language);

        // 19 FundingReference (with name, identifier, and award related sub- properties) (O)
        writeFundingReferencesElement(xmlw, version, language);
    }

    /**
     * Get the language value or null
     *
     * @param xmlw
     * @param datasetVersionDTO
     * @return
     * @throws XMLStreamException
     */
    public static String getLanguage(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        String language = null;

        // set the default language (using language attribute)
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.language.equals(fieldDTO.getTypeName())) {
                        for (String language_found : fieldDTO.getMultipleVocab()) {
                            if (StringUtils.isNotBlank(language_found)) {
                                language = language_found;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return language;
    }

    /**
     * 1, Identifier (with mandatory type sub-property) (M)
     *
     * @param xmlw The Steam writer
     * @param identifier The identifier url like https://doi.org/10.123/123
     * @throws XMLStreamException
     */
    public static void writeIdentifierElement(XMLStreamWriter xmlw, String identifier, String language) throws XMLStreamException {
        // identifier with identifierType attribute        
        if (StringUtils.isNotBlank(identifier)) {
            Map<String, String> identifier_map = new HashMap<String, String>();

            if (StringUtils.containsIgnoreCase(identifier, AbstractDOIProvider.DOI_RESOLVER_URL)) {
                identifier_map.put("identifierType", "DOI");
                identifier = StringUtils.substring(identifier, identifier.indexOf("10."));
            } else if (StringUtils.containsIgnoreCase(identifier, HandlePidProvider.HDL_RESOLVER_URL)) {
                identifier_map.put("identifierType", "Handle");
                if (StringUtils.contains(identifier, "http")) {
                    identifier = identifier.replace(identifier.substring(0, identifier.indexOf("/") + 2), "");
                    identifier = identifier.substring(identifier.indexOf("/") + 1);
                }
            }
            writeFullElement(xmlw, null, "identifier", identifier_map, identifier, language);
        }
    }

    /**
     * 2, Creator (with optional given name, family name, name identifier and
     * affiliation sub-properties) (M)
     *
     * @param xmlw The stream writer
     * @param datasetVersionDTO
     * @param language current language value
     * @throws XMLStreamException
     */
    public static void writeCreatorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // creators -> creator -> creatorName with nameType attribute, givenName, familyName, nameIdentifier
        // write all creators
        boolean creator_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String creatorName = null;
                            String affiliation = null;
                            String nameIdentifier = null;
                            String nameIdentifierScheme = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    creatorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorIdValue.equals(next.getTypeName())) {
                                    nameIdentifier = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorIdType.equals(next.getTypeName())) {
                                    nameIdentifierScheme = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorAffiliation.equals(next.getTypeName())) {
                                    affiliation = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(creatorName)) {
                                creator_check = writeOpenTag(xmlw, "creators", creator_check);
                                xmlw.writeStartElement("creator"); // <creator>
                                
                                Map<String, String> creator_map = new HashMap<String, String>();
                                JsonObject creatorObj = PersonOrOrgUtil.getPersonOrOrganization(creatorName, false,
                                        StringUtils.containsIgnoreCase(nameIdentifierScheme, "orcid"));

                                // creatorName=<FamilyName>, <FirstName>
                                if (creatorObj.getBoolean("isPerson")) {
                                    creator_map.put("nameType", "Personal");
                                } else {
                                    creator_map.put("nameType", "Organizational");
                                }
                                writeFullElement(xmlw, null, "creatorName", creator_map,
                                        creatorObj.getString("fullName"), language);
                                if (creatorObj.containsKey("givenName")) {
                                    writeFullElement(xmlw, null, "givenName", null, creatorObj.getString("givenName"),
                                            language);
                                }
                                if (creatorObj.containsKey("familyName")) {
                                    writeFullElement(xmlw, null, "familyName", null, creatorObj.getString("familyName"),
                                            language);
                                }

                                if (StringUtils.isNotBlank(nameIdentifier)) {
                                    creator_map.clear();

                                    if (StringUtils.contains(nameIdentifier, "http")) {
                                        String site = nameIdentifier.substring(0, nameIdentifier.indexOf("/") + 2);
                                        nameIdentifier = nameIdentifier.replace(nameIdentifier.substring(0, nameIdentifier.indexOf("/") + 2), "");
                                        site = site + nameIdentifier.substring(0, nameIdentifier.indexOf("/") + 1);
                                        nameIdentifier = nameIdentifier.substring(nameIdentifier.indexOf("/") + 1);

                                        creator_map.put("SchemeURI", site);
                                    }

                                    if (StringUtils.isNotBlank(nameIdentifierScheme)) {
                                        creator_map.put("nameIdentifierScheme", nameIdentifierScheme);
                                        writeFullElement(xmlw, null, "nameIdentifier", creator_map, nameIdentifier, language);
                                    } else {
                                        writeFullElement(xmlw, null, "nameIdentifier", null, nameIdentifier, language);
                                    }
                                }

                                if (StringUtils.isNotBlank(affiliation)) {
                                    writeFullElement(xmlw, null, "affiliation", null, affiliation, language);
                                }
                                xmlw.writeEndElement(); // </creator>
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, creator_check);
    }

    /**
     * 3, Title (with optional type sub-properties) (M)
     *
     * @param xmlw The stream writer
     * @param datasetVersionDTO
     * @param language current language value
     * @throws XMLStreamException
     */
    public static void writeTitlesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // titles -> title with titleType attribute
        boolean title_check = false;

        String title = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.title);
        title_check = writeTitleElement(xmlw, null, title, title_check, language);

        String subtitle = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.subTitle);
        title_check = writeTitleElement(xmlw, "Subtitle", subtitle, title_check, language);

        title_check = writeMultipleTitleElement(xmlw, "AlternativeTitle", datasetVersionDTO, "citation", title_check, language);
        writeEndTag(xmlw, title_check);
    }

    private static boolean writeMultipleTitleElement(XMLStreamWriter xmlw, String titleType, DatasetVersionDTO datasetVersionDTO, String metadataBlockName, boolean title_check, String language) throws XMLStreamException {
        MetadataBlockDTO block = datasetVersionDTO.getMetadataBlocks().get(metadataBlockName);
        if (block != null) {
            logger.fine("Block is not empty");
            List<FieldDTO> fieldsBlock =  block.getFields();
            if (fieldsBlock != null) {
                for (FieldDTO fieldDTO : fieldsBlock) {
                    logger.fine(titleType + " " + fieldDTO.getTypeName());
                    if (titleType.toLowerCase().equals(fieldDTO.getTypeName().toLowerCase())) {
                        logger.fine("Found Alt title");
                        List<String> fields = fieldDTO.getMultiplePrimitive();
                        for (String value : fields) {
                            if (!writeTitleElement(xmlw, titleType, value, title_check, language))
                                title_check = false;
                        }
                        break;
                    }
                }
            }
        }

        return title_check;
    }

    /**
     * 3, Title (with optional type sub-properties) (M)
     *
     * @param xmlw The Steam writer
     * @param titleType The item type, for instance AlternativeTitle
     * @param title The title
     * @param title_check
     * @param language current language
     * @return
     * @throws XMLStreamException
     */
    private static boolean writeTitleElement(XMLStreamWriter xmlw, String titleType, String title, boolean title_check, String language) throws XMLStreamException {
        // write a title
        if (StringUtils.isNotBlank(title)) {
            title_check = writeOpenTag(xmlw, "titles", title_check);
            xmlw.writeStartElement("title"); // <title>

            if (StringUtils.isNotBlank(language)) {
                xmlw.writeAttribute("xml:lang", language);
            }

            if (StringUtils.isNotBlank(titleType)) {
                xmlw.writeAttribute("titleType", titleType);
            }

            xmlw.writeCharacters(title);
            xmlw.writeEndElement(); // </title>
        }
        return title_check;
    }

    /**
     * 5, PublicationYear (M)
     *
     * @param xmlw The stream writer
     * @param datasetVersionDTO
     * @param language current language value
     * @throws XMLStreamException
     */
    public static void writePublicationYearElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String publicationDate, String language) throws XMLStreamException {

        // publicationYear
        String distributionDate = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.distributionDate);
        //String publicationDate = datasetDto.getPublicationDate();
        String depositDate = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.dateOfDeposit);

        int distributionYear = -1;
        int publicationYear = -1;
        int yearOfDeposit = -1;
        int pubYear = 0;

        if (distributionDate != null) {
            distributionYear = Integer.parseInt(distributionDate.substring(0, 4));
        }
        if (publicationDate != null) {
            publicationYear = Integer.parseInt(publicationDate.substring(0, 4));
        }
        if (depositDate != null) {
            yearOfDeposit = Integer.parseInt(depositDate.substring(0, 4));
        }

        pubYear = Integer.max(Integer.max(distributionYear, publicationYear), yearOfDeposit);
        if (pubYear > -1) {
            writeFullElement(xmlw, null, "publicationYear", null, String.valueOf(pubYear), language);
        }
    }

    /**
     * 6, Subject (with scheme sub-property) R
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeSubjectsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // subjects -> subject with subjectScheme and schemeURI attributes
        boolean subject_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())) {
                        for (String subject : fieldDTO.getMultipleVocab()) {
                            if (StringUtils.isNotBlank(subject)) {
                                subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                                writeSubjectElement(xmlw, null, null, subject, language);
                            }
                        }
                    }

                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String subject = null;
                            String subjectScheme = null;
                            String schemeURI = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    subject = next.getSinglePrimitive();
                                }

                                if (DatasetFieldConstant.keywordVocab.equals(next.getTypeName())) {
                                    subjectScheme = next.getSinglePrimitive();
                                }

                                if (DatasetFieldConstant.keywordVocabURI.equals(next.getTypeName())) {
                                    schemeURI = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(subject)) {
                                subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                                writeSubjectElement(xmlw, subjectScheme, schemeURI, subject, language);
                            }
                        }
                    }

                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String subject = null;
                            String subjectScheme = null;
                            String schemeURI = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                    subject = next.getSinglePrimitive();
                                }

                                if (DatasetFieldConstant.topicClassVocab.equals(next.getTypeName())) {
                                    subjectScheme = next.getSinglePrimitive();
                                }

                                if (DatasetFieldConstant.topicClassVocabURI.equals(next.getTypeName())) {
                                    schemeURI = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(subject)) {
                                subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                                writeSubjectElement(xmlw, subjectScheme, schemeURI, subject, language);
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, subject_check);
    }

    /**
     * 6, Subject (with scheme sub-property) R
     *
     * @param xmlw
     * @param subjectScheme
     * @param schemeURI
     * @param value
     * @param language
     * @throws XMLStreamException
     */
    private static void writeSubjectElement(XMLStreamWriter xmlw, String subjectScheme, String schemeURI, String value, String language) throws XMLStreamException {
        // write a subject
        Map<String, String> subject_map = new HashMap<String, String>();

        if (StringUtils.isNotBlank(language)) {
            subject_map.put("xml:lang", language);
        }

        if (StringUtils.isNotBlank(subjectScheme)) {
            subject_map.put("subjectScheme", subjectScheme);
        }
        if (StringUtils.isNotBlank(schemeURI)) {
            subject_map.put("schemeURI", schemeURI);
        }

        if (!subject_map.isEmpty()) {
            writeFullElement(xmlw, null, "subject", subject_map, value, language);
        } else {
            writeFullElement(xmlw, null, "subject", null, value, language);
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
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeContributorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // contributors -> contributor with ContributorType attribute -> contributorName, affiliation
        boolean contributor_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    // skip non-scompound value

                    if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String producerName = null;
                            String producerAffiliation = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.producerName.equals(next.getTypeName())) {
                                    producerName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAffiliation.equals(next.getTypeName())) {
                                    producerAffiliation = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(producerName)) {
                                contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                                writeContributorElement(xmlw, "Producer", producerName, producerAffiliation, language);
                            }
                        }
                    } else if (DatasetFieldConstant.distributor.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String distributorName = null;
                            String distributorAffiliation = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.distributorName.equals(next.getTypeName())) {
                                    distributorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorAffiliation.equals(next.getTypeName())) {
                                    distributorAffiliation = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(distributorName)) {
                                contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                                writeContributorElement(xmlw, "Distributor", distributorName, distributorAffiliation, language);
                            }
                        }
                    } else if (DatasetFieldConstant.datasetContact.equals(fieldDTO.getTypeName())) {
                        if ("primitive".equals(fieldDTO.getTypeClass())) {
                            String contactAffiliation = null;
                            String contactName = null;

                            for (Iterator<String> iterator = fieldDTO.getMultiplePrimitive().iterator(); iterator.hasNext();) {
                                contactName = iterator.next();

                                if (StringUtils.isNotBlank(contactName)) {
                                    contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                                    writeContributorElement(xmlw, "ContactPerson", contactName, contactAffiliation, language);
                                }
                            }
                        } else {
                            for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                                String contactName = null;
                                String contactAffiliation = null;

                                for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                    FieldDTO next = iterator.next();
                                    if (DatasetFieldConstant.datasetContactName.equals(next.getTypeName())) {
                                        contactName = next.getSinglePrimitive();
                                    }
                                    if (DatasetFieldConstant.datasetContactAffiliation.equals(next.getTypeName())) {
                                        contactAffiliation = next.getSinglePrimitive();
                                    }
                                }

                                if (StringUtils.isNotBlank(contactName)) {
                                    contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                                    writeContributorElement(xmlw, "ContactPerson", contactName, contactAffiliation, language);
                                }
                            }
                        }
                    } else if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String contributorName = null;
                            String contributorType = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                    contributorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.contributorType.equals(next.getTypeName())) {
                                    contributorType = next.getSinglePrimitive();
                                }
                            }

                            // Fix Funder contributorType
                            if (StringUtils.isNotBlank(contributorName) && !StringUtils.equalsIgnoreCase(FunderType, contributorType)) {
                                contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                                writeContributorElement(xmlw, contributorType, contributorName, null, language);
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, contributor_check);
    }

    /**
     * 7, Contributor (with optional given name, family name, name identifier
     * and affiliation sub-properties)
     *
     * Write single contributor tag.
     *
     * @param xmlw The stream writer
     * @param contributorType The contributorType (M)
     * @param contributorName The contributorName (M)
     * @param contributorAffiliation
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeContributorElement(XMLStreamWriter xmlw, String contributorType, String contributorName, String contributorAffiliation, String language) throws XMLStreamException {
        // write a contributor
        xmlw.writeStartElement("contributor"); // <contributor>

        if (StringUtils.isNotBlank(contributorType)) {
            xmlw.writeAttribute("contributorType", contributorType.replaceAll(" ", ""));
        }

        boolean nameType_check = false;
        Map<String, String> contributor_map = new HashMap<String, String>();

        JsonObject contributorObj = PersonOrOrgUtil.getPersonOrOrganization(contributorName,
                ("ContactPerson".equals(contributorType) && !isValidEmailAddress(contributorName)), false);

        if (contributorObj.getBoolean("isPerson")) {
            if(contributorObj.containsKey("givenName")) {
                contributor_map.put("nameType", "Personal");
            }
        } else {
            contributor_map.put("nameType", "Organizational");
        }
        writeFullElement(xmlw, null, "contributorName", contributor_map, contributorName, language);

        if (contributorObj.containsKey("givenName")) {
            writeFullElement(xmlw, null, "givenName", null, contributorObj.getString("givenName"), language);
        }
        if (contributorObj.containsKey("familyName")) {
            writeFullElement(xmlw, null, "familyName", null, contributorObj.getString("familyName"), language);
        }

        if (StringUtils.isNotBlank(contributorAffiliation)) {
            writeFullElement(xmlw, null, "affiliation", null, contributorAffiliation, language);
        }
        xmlw.writeEndElement(); // </contributor>
    }

    /**
     * 8, Date (with type sub-property) (R)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeDatesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        boolean date_check = false;
        String dateOfDistribution = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.distributionDate);
        if (StringUtils.isNotBlank(dateOfDistribution)) {
            date_check = writeOpenTag(xmlw, "dates", date_check);

            Map<String, String> date_map = new HashMap<String, String>();
            date_map.put("dateType", "Issued");
            writeFullElement(xmlw, null, "date", date_map, dateOfDistribution, language);
        }
        // dates -> date with dateType attribute

        String dateOfProduction = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.productionDate);
        if (StringUtils.isNotBlank(dateOfProduction)) {
            date_check = writeOpenTag(xmlw, "dates", date_check);

            Map<String, String> date_map = new HashMap<String, String>();
            date_map.put("dateType", "Created");
            writeFullElement(xmlw, null, "date", date_map, dateOfProduction, language);
        }

        String dateOfDeposit = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.dateOfDeposit);
        if (StringUtils.isNotBlank(dateOfDeposit)) {
            date_check = writeOpenTag(xmlw, "dates", date_check);

            Map<String, String> date_map = new HashMap<String, String>();
            date_map.put("dateType", "Submitted");
            writeFullElement(xmlw, null, "date", date_map, dateOfDeposit, language);
        }

        String dateOfVersion = datasetVersionDTO.getReleaseTime();
        if (StringUtils.isNotBlank(dateOfVersion)) {
            date_check = writeOpenTag(xmlw, "dates", date_check);

            Map<String, String> date_map = new HashMap<String, String>();
            date_map.put("dateType", "Updated");
            writeFullElement(xmlw, null, "date", date_map, dateOfVersion.substring(0, 10), language);
        }

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String dateOfCollectionStart = null;
                            String dateOfCollectionEnd = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.dateOfCollectionStart.equals(next.getTypeName())) {
                                    dateOfCollectionStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.dateOfCollectionEnd.equals(next.getTypeName())) {
                                    dateOfCollectionEnd = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(dateOfCollectionStart) && StringUtils.isNotBlank(dateOfCollectionEnd)) {
                                date_check = writeOpenTag(xmlw, "dates", date_check);

                                Map<String, String> date_map = new HashMap<String, String>();
                                date_map.put("dateType", "Collected");
                                writeFullElement(xmlw, null, "date", date_map, dateOfCollectionStart + "/" + dateOfCollectionEnd, language);
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, date_check);
    }

    /**
     * 10, ResourceType (with mandatory general type description sub- property)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeResourceTypeElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // resourceType with resourceTypeGeneral attribute
        boolean resourceTypeFound = false;
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())) {
                        for (String resourceType : fieldDTO.getMultipleVocab()) {
                            if (StringUtils.isNotBlank(resourceType)) {
                                Map<String, String> resourceType_map = new HashMap<String, String>();
                                resourceType_map.put("resourceTypeGeneral", "Dataset");
                                writeFullElement(xmlw, null, "resourceType", resourceType_map, resourceType, language);
                                resourceTypeFound = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (!resourceTypeFound) {
            xmlw.writeStartElement("resourceType"); // <resourceType>
            xmlw.writeAttribute("resourceTypeGeneral", "Dataset");
            xmlw.writeEndElement(); // </resourceType>
        }
    }

    /**
     * 11 AlternateIdentifier (with type sub-property) (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeAlternateIdentifierElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // alternateIdentifiers -> alternateIdentifier with alternateIdentifierType attribute
        boolean alternateIdentifier_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String alternateIdentifier = null;
                            String alternateIdentifierType = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                    alternateIdentifier = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.otherIdAgency.equals(next.getTypeName())) {
                                    alternateIdentifierType = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(alternateIdentifier)) {
                                alternateIdentifier_check = writeOpenTag(xmlw, "alternateIdentifiers", alternateIdentifier_check);

                                if (StringUtils.isNotBlank(alternateIdentifierType)) {
                                    Map<String, String> alternateIdentifier_map = new HashMap<String, String>();
                                    alternateIdentifier_map.put("alternateIdentifierType", alternateIdentifierType);
                                    writeFullElement(xmlw, null, "alternateIdentifier", alternateIdentifier_map, alternateIdentifier, language);
                                } else {
                                    writeFullElement(xmlw, null, "alternateIdentifier", null, alternateIdentifier, language);
                                }
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, alternateIdentifier_check);
    }

    /**
     * 12, RelatedIdentifier (with type and relation type sub-properties) (R)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeRelatedIdentifierElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // relatedIdentifiers -> relatedIdentifier with relatedIdentifierType and relationType attributes
        boolean relatedIdentifier_check = false;
        HashMap relatedIdentifierTypeMap = new HashMap();
        {
            relatedIdentifierTypeMap.put("ARK".toLowerCase(), "ARK");
            relatedIdentifierTypeMap.put("arXiv".toLowerCase(), "arXiv");
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
        }

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String relatedIdentifierType = null;
                            String relatedIdentifier = null; // is used when relatedIdentifierType variable is not URL
                            String relatedURL = null; // is used when relatedIdentifierType variable is URL

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.publicationIDType.equals(next.getTypeName())) {
                                    relatedIdentifierType = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDNumber.equals(next.getTypeName())) {
                                    relatedIdentifier = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationURL.equals(next.getTypeName())) {
                                    relatedURL = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(relatedIdentifierType)) {
                                relatedIdentifier_check = writeOpenTag(xmlw, "relatedIdentifiers", relatedIdentifier_check);

                                Map<String, String> relatedIdentifier_map = new HashMap<String, String>();
                                // fix case
                                if (relatedIdentifierTypeMap.containsKey(relatedIdentifierType)) {
                                    relatedIdentifierType = (String) relatedIdentifierTypeMap.get(relatedIdentifierType);
                                }

                                relatedIdentifier_map.put("relatedIdentifierType", relatedIdentifierType);
                                relatedIdentifier_map.put("relationType", "IsCitedBy");

                                if (StringUtils.containsIgnoreCase(relatedIdentifierType, "url")) {
                                    writeFullElement(xmlw, null, "relatedIdentifier", relatedIdentifier_map, relatedURL, language);
                                } else {
                                    if (StringUtils.contains(relatedIdentifier, "http")) {
                                        String site = relatedIdentifier.substring(0, relatedIdentifier.indexOf("/") + 2);
                                        relatedIdentifier = relatedIdentifier.replace(relatedIdentifier.substring(0, relatedIdentifier.indexOf("/") + 2), "");
                                        site = site + relatedIdentifier.substring(0, relatedIdentifier.indexOf("/") + 1);
                                        relatedIdentifier = relatedIdentifier.substring(relatedIdentifier.indexOf("/") + 1);

                                        relatedIdentifier_map.put("SchemeURI", site);
                                    }
                                    writeFullElement(xmlw, null, "relatedIdentifier", relatedIdentifier_map, relatedIdentifier, language);
                                }
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, relatedIdentifier_check);
    }

    /**
     * 13, Size (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeSizeElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // sizes -> size
        boolean size_check = false;

        if (datasetVersionDTO.getFiles() != null) {
            for (int i = 0; i < datasetVersionDTO.getFiles().size(); i++) {
                Long size = datasetVersionDTO.getFiles().get(i).getDataFile().getFileSize();
                if (size != null) {
                    size_check = writeOpenTag(xmlw, "sizes", size_check);
                    writeFullElement(xmlw, null, "size", null, size.toString(), language);
                }
            }
            writeEndTag(xmlw, size_check);
        }
    }

    /**
     * 14, Format (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeFormatElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // formats -> format
        boolean format_check = false;

        if (datasetVersionDTO.getFiles() != null) {
            for (int i = 0; i < datasetVersionDTO.getFiles().size(); i++) {
                String format = datasetVersionDTO.getFiles().get(i).getDataFile().getContentType();
                if (StringUtils.isNotBlank(format)) {
                    format_check = writeOpenTag(xmlw, "formats", format_check);
                    writeFullElement(xmlw, null, "format", null, format, language);
                }
            }
            writeEndTag(xmlw, format_check);
        }
    }

    /**
     * 15, Version (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeVersionElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        Long majorVersionNumber = datasetVersionDTO.getVersionNumber();
        Long minorVersionNumber = datasetVersionDTO.getMinorVersionNumber();

        if (majorVersionNumber != null && StringUtils.isNotBlank(majorVersionNumber.toString())) {
            if (minorVersionNumber != null && StringUtils.isNotBlank(minorVersionNumber.toString())) {
                writeFullElement(xmlw, null, "version", null, majorVersionNumber.toString() + "." + minorVersionNumber.toString(), language);
            } else {
                writeFullElement(xmlw, null, "version", null, majorVersionNumber.toString(), language);
            }
        }
    }

    /**
     * 16 Rights (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeAccessRightsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
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

    /**
     * 16 Rights (O)
     *
     * Write headers
     *
     * @param xmlw The Steam writer
     * @param language current language
     * @throws XMLStreamException
     */
    private static void writeRightsHeader(XMLStreamWriter xmlw, String language) throws XMLStreamException {
        // write the rights header
        xmlw.writeStartElement("rights"); // <rights>

        if (StringUtils.isNotBlank(language)) {
            xmlw.writeAttribute("xml:lang", language);
        }
    }

    /**
     * 17 Descriptions (R)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeDescriptionsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // descriptions -> description with descriptionType attribute
        boolean description_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String descriptionOfAbstract = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionOfAbstract = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(descriptionOfAbstract)) {
                                description_check = writeOpenTag(xmlw, "descriptions", description_check);
                                writeDescriptionElement(xmlw, "Abstract", descriptionOfAbstract, language);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.software.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String softwareName = null;
                            String softwareVersion = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.softwareName.equals(next.getTypeName())) {
                                    softwareName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.softwareVersion.equals(next.getTypeName())) {
                                    softwareVersion = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(softwareName) && StringUtils.isNotBlank(softwareVersion)) {
                                description_check = writeOpenTag(xmlw, "descriptions", description_check);
                                writeDescriptionElement(xmlw, "TechnicalInfo", softwareName + ", " + softwareVersion, language);
                            }
                        }
                    }
                }
            }
        }

        String descriptionOfMethodsOrigin = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.originOfSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsOrigin)) {
            description_check = writeOpenTag(xmlw, "descriptions", description_check);
            writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsOrigin, language);
        }

        String descriptionOfMethodsCharacteristic = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.characteristicOfSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsCharacteristic)) {
            description_check = writeOpenTag(xmlw, "descriptions", description_check);
            writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsCharacteristic, language);
        }

        String descriptionOfMethodsAccess = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.accessToSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsAccess)) {
            description_check = writeOpenTag(xmlw, "descriptions", description_check);
            writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsAccess, language);
        }

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.series.equals(fieldDTO.getTypeName())) {
                        // String seriesName = null;
                        String seriesInformation = null;
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.seriesInformation.equals(next.getTypeName())) {
                                    seriesInformation = next.getSinglePrimitive();
                                }
                            }
                            if (StringUtils.isNotBlank(seriesInformation)) {
                                description_check = writeOpenTag(xmlw, "descriptions", description_check);
                                writeDescriptionElement(xmlw, "SeriesInformation", seriesInformation, language);
                            }
                        }
                    }
                }
            }
        }

        String descriptionOfOther = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.notesText);
        if (StringUtils.isNotBlank(descriptionOfOther)) {
            description_check = writeOpenTag(xmlw, "descriptions", description_check);
            writeDescriptionElement(xmlw, "Other", descriptionOfOther, language);
        }
        writeEndTag(xmlw, description_check);
    }

    /**
     * 17 Descriptions (R)
     *
     * @param xmlw
     * @param descriptionType
     * @param description
     * @param language
     * @throws XMLStreamException
     */
    private static void writeDescriptionElement(XMLStreamWriter xmlw, String descriptionType, String description, String language) throws XMLStreamException {
        // write a description
        Map<String, String> description_map = new HashMap<String, String>();

        if (StringUtils.isNotBlank(language)) {
            description_map.put("xml:lang", language);
        }

        description_map.put("descriptionType", descriptionType);
        writeFullElement(xmlw, null, "description", description_map, description, language);
    }

    /**
     * 18 GeoLocation (R)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeGeoLocationsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // geoLocation -> geoLocationPlace
        String geoLocationPlace = dto2Primitive(datasetVersionDTO, DatasetFieldConstant.productionPlace);
        boolean geoLocations_check = false;

        // write geoLocations
        geoLocations_check = writeOpenTag(xmlw, "geoLocations", geoLocations_check);
        writeGeolocationPlace(xmlw, geoLocationPlace, language);
                
        // get DatasetFieldConstant.geographicBoundingBox
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (DatasetFieldConstant.geographicBoundingBox.equals(fieldDTO.getTypeName())) {
                    geoLocations_check = writeOpenTag(xmlw, "geoLocations", geoLocations_check);
                    if (fieldDTO.getMultiple()) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            writeGeoLocationsElement(xmlw, fieldDTOs, language);
                        }
                    } else {
                        writeGeoLocationsElement(xmlw, fieldDTO.getSingleCompound(), language);
                    }
                }
            }
        }

        writeEndTag(xmlw, geoLocations_check);
    }

    /**
     * 18 GeoLocation (R)
     *
     * Write geoLocationPlace inside geoLocation element
     * 
     * @param xmlw The Steam writer
     * @param geoLocationPlace Geo location place
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeGeolocationPlace(XMLStreamWriter xmlw, String geoLocationPlace, String language) throws XMLStreamException {
        boolean geoLocation_check = false;
        
        if (StringUtils.isNotBlank(geoLocationPlace)) {
            geoLocation_check = writeOpenTag(xmlw, "geoLocation", geoLocation_check);
            writeFullElement(xmlw, null, "geoLocationPlace", null, geoLocationPlace, language);
        }
        writeEndTag(xmlw, geoLocation_check);
    }
    
    /**
     * 18 GeoLocation (R)
     *
     * @param xmlw The Steam writer
     * @param fieldDTOs
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeGeoLocationsElement(XMLStreamWriter xmlw, Set<FieldDTO> fieldDTOs, String language) throws XMLStreamException {
        //boolean geoLocations_check = false;
        boolean geoLocation_check = false;
        boolean geoLocationbox_check = false;

        geoLocation_check = writeOpenTag(xmlw, "geoLocation", geoLocation_check);
        geoLocationbox_check = writeOpenTag(xmlw, "geoLocationBox", geoLocationbox_check);

        for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
            FieldDTO next = iterator.next();
            String typeName = next.getTypeName();

            Pattern pattern = Pattern.compile("([a-z]+)(Longitude|Latitude)");
            Matcher matcher = pattern.matcher(next.getTypeName());
            boolean skip = false;
            if (matcher.find()) {
                switch (matcher.group(1)) {
                    case "south":
                    case "north":
                        typeName = matcher.group(1) + "BoundLatitude";
                        break;

                    case "west":
                    case "east":
                        typeName = matcher.group(1) + "BoundLongitude";
                        break;

                    default:
                        skip = true;
                        break;
                }
                if (!skip) {
                    writeFullElement(xmlw, null, typeName, null, next.getSinglePrimitive(), language);
                }
            }
        }
        writeEndTag(xmlw, geoLocationbox_check);
        writeEndTag(xmlw, geoLocation_check);
    }

    /**
     *
     * 19 FundingReference (with name, identifier, and award related sub-
     * properties) (O)
     *
     * @param xmlw The Steam writer
     * @param datasetVersionDTO
     * @param language current language
     * @throws XMLStreamException
     */
    public static void writeFundingReferencesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String language) throws XMLStreamException {
        // fundingReferences -> fundingReference -> funderName, awardNumber
        boolean fundingReference_check = false;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String awardNumber = null;
                            String funderName = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.grantNumberValue.equals(next.getTypeName())) {
                                    awardNumber = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.grantNumberAgency.equals(next.getTypeName())) {
                                    funderName = next.getSinglePrimitive();
                                }
                            }

                            if (StringUtils.isNotBlank(funderName)) {
                                fundingReference_check = writeOpenTag(xmlw, "fundingReferences", fundingReference_check);
                                xmlw.writeStartElement("fundingReference"); // <fundingReference>
                                writeFullElement(xmlw, null, "funderName", null, funderName, language);

                                if (StringUtils.isNotBlank(awardNumber)) {
                                    writeFullElement(xmlw, null, "awardNumber", null, awardNumber, language);
                                }

                                xmlw.writeEndElement(); // </fundingReference>
                            }
                        }
                    } else if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String contributorName = null;
                            String contributorType = null;

                            for (Iterator<FieldDTO> iterator = fieldDTOs.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                    contributorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.contributorType.equals(next.getTypeName())) {
                                    contributorType = next.getSinglePrimitive();
                                }
                            }

                            // Fix Funder contributorType
                            if (StringUtils.isNotBlank(contributorName) && StringUtils.equalsIgnoreCase(FunderType, contributorType)) {
                                fundingReference_check = writeOpenTag(xmlw, "fundingReferences", fundingReference_check);
                                xmlw.writeStartElement("fundingReference"); // <fundingReference>
                                writeFullElement(xmlw, null, "funderName", null, contributorName, language);

                                xmlw.writeEndElement(); // </fundingReference>
                            }
                        }
                    }
                }
            }
        }
        writeEndTag(xmlw, fundingReference_check);
    }

    private static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        // give the single value of the given metadata
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getSinglePrimitive();
                }
            }
        }
        return null;
    }

    /**
     * Write a full tag.
     *
     * @param xmlw
     * @param tag_parent Parent
     * @param tag_son Son
     * @param map Map of properties
     * @param value Value
     * @throws XMLStreamException
     */
    public static void writeFullElement(XMLStreamWriter xmlw, String tag_parent, String tag_son, Map<String, String> map, String value, String language) throws XMLStreamException {
        // write a full generic metadata
        if (StringUtils.isNotBlank(value)) {
            boolean tag_parent_check = false;
            if (StringUtils.isNotBlank(tag_parent)) {
                xmlw.writeStartElement(tag_parent); // <value of tag_parent>
                tag_parent_check = true;
            }
            boolean tag_son_check = false;
            if (StringUtils.isNotBlank(tag_son)) {
                xmlw.writeStartElement(tag_son); // <value of tag_son>
                tag_son_check = true;
            }

            if (map != null) {
                if (StringUtils.isNotBlank(language)) {
                    if (StringUtils.containsIgnoreCase(tag_son, "subject") || StringUtils.containsIgnoreCase(tag_parent, "subject")) {
                        map.put("xml:lang", language);
                    }
                }
                writeAttribute(xmlw, map);
            }

            xmlw.writeCharacters(value);

            writeEndTag(xmlw, tag_son_check); // </value of tag_son>
            writeEndTag(xmlw, tag_parent_check); //  </value of tag_parent>
        }
    }

    private static void writeAttribute(XMLStreamWriter xmlw, Map<String, String> map) throws XMLStreamException {
        // write attribute(s) of the current tag
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String map_key = entry.getKey();
            String map_value = entry.getValue();

            if (StringUtils.isNotBlank(map_key) && StringUtils.isNotBlank(map_value)) {
                xmlw.writeAttribute(map_key, map_value);
            }
        }
    }

    private static boolean writeOpenTag(XMLStreamWriter xmlw, String tag, boolean element_check) throws XMLStreamException {
        // check if the current tag isn't opened
        if (!element_check) {
            xmlw.writeStartElement(tag); // <value of tag>
        }
        return true;
    }

    private static void writeEndTag(XMLStreamWriter xmlw, boolean element_check) throws XMLStreamException {
        // close the current tag
        if (element_check) {
            xmlw.writeEndElement(); // </value of current tag>
        }
    }

    /**
     * Check if the string is a valid email.
     *
     * @param email
     * @return true/false
     */
    private static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
}
