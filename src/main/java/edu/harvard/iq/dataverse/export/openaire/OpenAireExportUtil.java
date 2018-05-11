package edu.harvard.iq.dataverse.export.openaire;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.JsonObject;
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
import edu.harvard.iq.dataverse.util.json.JsonUtil;

public class OpenAireExportUtil {

	private static final Logger logger = Logger.getLogger(OpenAireExportUtil.class.getCanonicalName());
    
	public static String SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
	// public static String XOAI = "http://www.lyncode.com/xoai";
	public static String OAI_DATACITE_NAMESPACE = "http://schema.datacite.org/oai/oai-1.1/";
	public static String OAI_DATACITE_SCHEMA_LOCATION = "http://schema.datacite.org/oai/oai-1.1/oai.xsd";
	
	public static String SCHEMA_VERSION = "4.1";
	
	public static String RESOURCE_NAMESPACE = "http://schema.datacite.org/meta/kernel-4.1";
	public static String RESOURCE_SCHEMA_LOCATION = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";
    
	public static String language = null;
	
    public static void datasetJson2openaire(JsonObject datasetDtoAsJson, OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        
        dto2openaire(datasetDto, outputStream);
    }
    
    private static void dto2openaire(DatasetDTO datasetDto, OutputStream outputStream) throws XMLStreamException {
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        
        xmlw.writeStartElement("oai_datacite"); // <oai_datacite>
        
        xmlw.writeAttribute("xmlns:xsi", SCHEMA_INSTANCE);
        // xmlw.writeAttribute("xmlns:doc", XOAI);
        xmlw.writeAttribute("xmlns:oai_datacite", OAI_DATACITE_NAMESPACE);
        xmlw.writeAttribute("xsi:schemaLocation", OAI_DATACITE_NAMESPACE + " " + OAI_DATACITE_SCHEMA_LOCATION);
        
        xmlw.writeStartElement("schemaVersion"); // <schemaVersion>
        xmlw.writeCharacters(SCHEMA_VERSION);
        xmlw.writeEndElement(); // </schemaVersion>
        
        xmlw.writeStartElement("payload"); // <payload>
        
        xmlw.writeStartElement("resource"); // <resource>
        
        xmlw.writeAttribute("xsi:schemaLocation", RESOURCE_NAMESPACE + " " + RESOURCE_SCHEMA_LOCATION);
        
        createOpenAire(xmlw, datasetDto);
        
        xmlw.writeEndElement(); // </resource>
        xmlw.writeEndElement(); // </payload>
        xmlw.writeEndElement(); // </oai_datacite>
        
        xmlw.flush();
    }
    
    private static void createOpenAire(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
    	DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
    	
        
        
        // identifier with identifierType attribute
        writeIdentifierElement(xmlw, globalId.toURL().toString());
        
        
        
        // creators -> creator -> creatorName with nameType attribute, givenName, familyName, nameIdentifier
        writeCreatorElement(xmlw, version);
        
        
        
        // set the default language (using language attribute)
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.language.equals(fieldDTO.getTypeName())){
                        for (String language_found : fieldDTO.getMultipleVocab()){
                        	if (StringUtils.isNotBlank(language_found)) {
                        		language = language_found;
                        		break;
                        	}
                        }
                    }
                }
            }
        }
        
        
        
        // titles -> title with titleType attribute
        boolean title_check = false;
        
        String title = dto2Primitive(version, DatasetFieldConstant.title);
        title_check = writeTitleElement(xmlw, null, title, title_check);
        
        String subtitle = dto2Primitive(version, DatasetFieldConstant.subTitle);
        title_check = writeTitleElement(xmlw, "Subtitle", subtitle, title_check);
        
        String alternativeTitle = dto2Primitive(version, DatasetFieldConstant.alternativeTitle);
        title_check = writeTitleElement(xmlw, "Alternative Title", alternativeTitle, title_check);
        
        writeEndTag(xmlw, title_check);
        
        
        
        // publisher
        String publisher = dto2Primitive(version, datasetDto.getPublisher());
        writeFullElement(xmlw, null, "publisher", null, publisher);
        
        
        
        // publicationYear
        String publicationYear = dto2Primitive(version, DatasetFieldConstant.distributionDate);
        if (StringUtils.isNotBlank(publicationYear)) {
        	writeFullElement(xmlw, null, "publicationYear", null, publicationYear.substring(0, 4));
        }
        
        
        
        // subjects -> subject with subjectScheme and schemeURI attributes
        boolean subject_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())){
                        for (String subject : fieldDTO.getMultipleVocab()){
                        	if (StringUtils.isNotBlank(subject)) {
                        		subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                        		writeSubjectElement(xmlw, null, null, subject);
                        	}
                        }
                    }
                    
                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String subject = null;
                            String subjectScheme = null;
                            String schemeURI = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    subject =  next.getSinglePrimitive();
                                }
                                
                                if (DatasetFieldConstant.keywordVocab.equals(next.getTypeName())) {
                                	subjectScheme = next.getSinglePrimitive();
                                }
                                
                                if (DatasetFieldConstant.keywordVocabURI.equals(next.getTypeName())) {
                                	schemeURI = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(subject)){
                            	subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                        		writeSubjectElement(xmlw, subjectScheme, schemeURI, subject);
                            }
                        }
                    }
                    
                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String subject = null;
                            String subjectScheme = null;
                            String schemeURI = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                	subject =  next.getSinglePrimitive();
                                }
                                
                                if (DatasetFieldConstant.topicClassVocab.equals(next.getTypeName())) {
                                	subjectScheme = next.getSinglePrimitive();
                                }
                                
                                if (DatasetFieldConstant.topicClassVocabURI.equals(next.getTypeName())) {
                                	schemeURI = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(subject)){
                            	subject_check = writeOpenTag(xmlw, "subjects", subject_check);
                        		writeSubjectElement(xmlw, subjectScheme, schemeURI, subject);
                            }
                        }
                    }
                }
            }
        }

        writeEndTag(xmlw, subject_check);
        
        
        
        // contributors -> contributor with ContributorType attribute -> contributorName, affiliation
        boolean contributor_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                	if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String producerName = null;
                    		String producerAffiliation = null;
                            
                        	for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.producerName.equals(next.getTypeName())) {
                                	producerName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAffiliation.equals(next.getTypeName())) {
                                	producerAffiliation = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(producerName)) {
                            	contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);                            	
                            	writeContributorElement(xmlw, "Producer", producerName, producerAffiliation);
                            }
                        }
                	}
                	
                	if (DatasetFieldConstant.distributor.equals(fieldDTO.getTypeName())) {
                		for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                			String distributorName = null;
                    		String distributorAffiliation = null;
                    		
                			for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.distributorName.equals(next.getTypeName())) {
                                    distributorName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorAffiliation.equals(next.getTypeName())) {
                                	distributorAffiliation = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(distributorName)) {
                            	contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                            	writeContributorElement(xmlw, "Distributor", distributorName, distributorAffiliation);
                            }
                        }
                	}
                	
                    if (DatasetFieldConstant.datasetContact.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String contactName = null;
                            String contactAffiliation = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.datasetContactName.equals(next.getTypeName())) {
                                    contactName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.datasetContactAffiliation.equals(next.getTypeName())) {
                                	contactAffiliation = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(contactName)) {
                            	contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                            	writeContributorElement(xmlw, "ContactPerson", contactName, contactAffiliation);
                            }
                        }
                    }
                    
                    if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String contributorName = null;
                            String contributorType = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                	contributorName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.contributorType.equals(next.getTypeName())) {
                                	contributorType = next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(contributorName)) {
                            	contributor_check = writeOpenTag(xmlw, "contributors", contributor_check);
                            	writeContributorElement(xmlw, contributorType, contributorName, null);
                            }
                        }
                    }
                }
            }
        }
        
        writeEndTag(xmlw, contributor_check);
        
        
        
        // dates -> date with dateType attribute
        boolean date_check = false;
        
        String dateOfProduction = dto2Primitive(version, DatasetFieldConstant.productionDate);
        if (StringUtils.isNotBlank(dateOfProduction)) {
        	date_check = writeOpenTag(xmlw, "dates", date_check);
        	
        	Map <String, String> date_map = new HashMap <String, String> ();
        	date_map.put("dateType", "Created");
        	writeFullElement(xmlw, null, "date", date_map, dateOfProduction);
        }
        
        String dateOfDeposit = dto2Primitive(version, DatasetFieldConstant.dateOfDeposit);
        if (StringUtils.isNotBlank(dateOfDeposit)) {
        	date_check = writeOpenTag(xmlw, "dates", date_check);
        	
        	Map <String, String> date_map = new HashMap <String, String> ();
        	date_map.put("dateType", "Submitted");
        	writeFullElement(xmlw, null, "date", date_map, dateOfDeposit);
        }
        
        String dateOfVersion = version.getReleaseTime();
        if (StringUtils.isNotBlank(dateOfVersion)) {
        	date_check = writeOpenTag(xmlw, "dates", date_check);
        	
        	Map <String, String> date_map = new HashMap <String, String> ();
        	date_map.put("dateType", "Updated");
        	writeFullElement(xmlw, null, "date", date_map, dateOfVersion.substring(0, 10));
        }
        
        String dateOfDistribution = dto2Primitive(version, DatasetFieldConstant.distributionDate);
        if (StringUtils.isNotBlank(dateOfDistribution)) {
        	date_check = writeOpenTag(xmlw, "dates", date_check);
        	
        	Map <String, String> date_map = new HashMap <String, String> ();
        	date_map.put("dateType", "Issued");
        	writeFullElement(xmlw, null, "date", date_map, dateOfDistribution);
        }
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String dateOfCollectionStart = null;
                            String dateOfCollectionEnd = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
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
                            	
                            	Map <String, String> date_map = new HashMap <String, String> ();
                            	date_map.put("dateType", "Collected");
                            	writeFullElement(xmlw, null, "date", date_map, dateOfCollectionStart + "/" + dateOfCollectionEnd);
                            }
                        }
                    }
                }
            }
        }
        
        writeEndTag(xmlw, date_check);
        
        
        
        // language
        writeFullElement(xmlw, null, "language", null, language);
        /*for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.language.equals(fieldDTO.getTypeName())){
                        for (String language : fieldDTO.getMultipleVocab()){
                        	if (StringUtils.isNotBlank(language)) {
                        		writeFullElement(xmlw, null, "language", null, language);
                        		break;
                        	}
                        }
                    }
                }
            }
        }*/
        
        
        
        // resourceType with resourceTypeGeneral attribute
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())){
                        for (String resourceType : fieldDTO.getMultipleVocab()){
                        	if (StringUtils.isNotBlank(resourceType)) {
                        		Map <String, String> resourceType_map = new HashMap <String, String> ();
                            	resourceType_map.put("resourceTypeGeneral", "Dataset");
	                            writeFullElement(xmlw, null, "resourceType", resourceType_map, resourceType);
                        		break;
                        	}
                        }
                    }
                }
            }
        }
        
        
        
        // alternateIdentifiers -> alternateIdentifier with alternateIdentifierType attribute
        boolean alternateIdentifier_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String alternateIdentifier = null;
                            String alternateIdentifierType = null;
                            
                        	for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                	alternateIdentifier = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.otherIdAgency.equals(next.getTypeName())) {
                                	alternateIdentifierType =  next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(alternateIdentifier)) {
                            	alternateIdentifier_check = writeOpenTag(xmlw, "alternateIdentifiers", alternateIdentifier_check);
                            	
                            	if (StringUtils.isNotBlank(alternateIdentifierType)) {
                            		Map <String, String> alternateIdentifier_map = new HashMap <String, String> ();
                        			alternateIdentifier_map.put("alternateIdentifierType", alternateIdentifierType);
                        			writeFullElement(xmlw, null, "alternateIdentifier", alternateIdentifier_map, alternateIdentifier);
                                }
                        		else {
                        			writeFullElement(xmlw, null, "alternateIdentifier", null, alternateIdentifier);
                        		}
                            }
                        }
                    }
                }
            }
        }
        
        writeEndTag(xmlw, alternateIdentifier_check);
        
        
        
        // relatedIdentifiers -> relatedIdentifier with relatedIdentifierType and relationType attributes
        boolean relatedIdentifier_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String relatedIdentifierType = null;
                            String relatedIdentifier = null; // is used when relatedIdentifierType variable is not URL
                            String relatedURL = null; // is used when relatedIdentifierType variable is URL
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
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
                            	
                        		Map <String, String> relatedIdentifier_map = new HashMap <String, String> ();
                        		relatedIdentifier_map.put("relatedIdentifierType", relatedIdentifierType);
                        		relatedIdentifier_map.put("relationType", "isCitedBy");
                        		
                            	if (StringUtils.containsIgnoreCase(relatedIdentifierType, "url")) {
                            		writeFullElement(xmlw, null, "relatedIdentifier", relatedIdentifier_map, relatedURL);
                            	}
                            	else {
                            		writeFullElement(xmlw, null, "relatedIdentifier", relatedIdentifier_map, relatedIdentifier);
                            	}
                            }
                        }
                    }
                }
            }
        }
        
        writeEndTag(xmlw, relatedIdentifier_check);
        
        
        
        // sizes -> size
        boolean size_check = false;
        
        for (int i = 0; i < version.getFiles().size(); i++) {
        	Long size = version.getFiles().get(i).getDataFile().getFileSize(); 
        	if (size != null) {
        		size_check = writeOpenTag(xmlw, "sizes", size_check);
        		writeFullElement(xmlw, null, "size", null, size.toString());
        	}
        }
        
        writeEndTag(xmlw, size_check);
        
        
        
        // formats -> format
        boolean format_check = false;
        
        for (int i = 0; i < version.getFiles().size(); i++) {
        	String format = version.getFiles().get(i).getDataFile().getContentType();
        	if (StringUtils.isNotBlank(format)) {
        		format_check = writeOpenTag(xmlw, "formats", format_check);
        		writeFullElement(xmlw, null, "format", null, format);
        	}
        }
        
        writeEndTag(xmlw, format_check);
        
        
        
        // version
        Long majorVersionNumber = version.getVersionNumber();
        Long minorVersionNumber = version.getMinorVersionNumber();
        if (StringUtils.isNotBlank(majorVersionNumber.toString())) {
        	if (StringUtils.isNotBlank(minorVersionNumber.toString())) {
        		writeFullElement(xmlw, null, "version", null, majorVersionNumber.toString() + "." + minorVersionNumber.toString());
        	}
        	else {
        		writeFullElement(xmlw, null, "version", null, majorVersionNumber.toString());
        	}
        }
        
        
        
        // rightsList -> rights with rightsURI attribute
        writeRightsListElement(xmlw, version/*, version.getTermsOfAccess(), version.getRestrictions()*/);
        
        
        
        // descriptions -> description with descriptionType attribute
        boolean description_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                	if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                		for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                    		String descriptionOfAbstract = null;
                            
                        	for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                	descriptionOfAbstract = next.getSinglePrimitive();
                                }
                            }
                        	
                        	if (StringUtils.isNotBlank(descriptionOfAbstract)) {
                            	description_check = writeOpenTag(xmlw, "descriptions", description_check);
                            	writeDescriptionElement(xmlw, "Abstract", descriptionOfAbstract);
                        	}
                        }
                	}
                }
            }
        }
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.software.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String softwareName = null;
                            String softwareVersion = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
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
                            	writeDescriptionElement(xmlw, "Methods", softwareName + ", " + softwareVersion);
                            }
                        }
                    }
                }
            }
        }
        
        String descriptionOfMethodsOrigin = dto2Primitive(version, DatasetFieldConstant.originOfSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsOrigin)) {
        	description_check = writeOpenTag(xmlw, "descriptions", description_check);
        	writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsOrigin);
        }
        
        String descriptionOfMethodsCharacteristic = dto2Primitive(version, DatasetFieldConstant.characteristicOfSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsCharacteristic)) {
        	description_check = writeOpenTag(xmlw, "descriptions", description_check);
        	writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsCharacteristic);
        }
        
        String descriptionOfMethodsAccess = dto2Primitive(version, DatasetFieldConstant.accessToSources);
        if (StringUtils.isNotBlank(descriptionOfMethodsAccess)) {
        	description_check = writeOpenTag(xmlw, "descriptions", description_check);
        	writeDescriptionElement(xmlw, "Methods", descriptionOfMethodsAccess);
        }
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.series.equals(fieldDTO.getTypeName())) {
                        // String seriesName = null;
                        String seriesInformation = null;
                        
                        Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            FieldDTO next = iterator.next();
                            /*if (DatasetFieldConstant.seriesName.equals(next.getTypeName())) {
                                seriesName =  next.getSinglePrimitive();
                            }*/
                            if (DatasetFieldConstant.seriesInformation.equals(next.getTypeName())) {
                                seriesInformation =  next.getSinglePrimitive();
                            }
                        }
                        
                        /*if (StringUtils.isNotBlank(seriesName)){
                        	contributor_check = writeOpenTag(xmlw, "descriptions", description_check);
                        	
                        	writeDescriptionElement(xmlw, "SeriesInformation", seriesName);
                        }*/
                        
                        if (StringUtils.isNotBlank(seriesInformation)){
                        	contributor_check = writeOpenTag(xmlw, "descriptions", description_check);
                        	writeDescriptionElement(xmlw, "SeriesInformation", seriesInformation);
                        }
                    }
                }
            }
        }
        
        String descriptionOfOther = dto2Primitive(version, DatasetFieldConstant.notesText);
        if (StringUtils.isNotBlank(descriptionOfOther)) {
        	description_check = writeOpenTag(xmlw, "descriptions", description_check);
        	writeDescriptionElement(xmlw, "Other", descriptionOfOther);
        }
        
        writeEndTag(xmlw, description_check);
        
        
        
        // fundingReferences -> fundingReference -> funderName, awardNumber
        boolean fundingReference_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String awardNumber = null;
                            String funderName = null;
                            
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
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
	                            writeFullElement(xmlw, null, "funderName", null, funderName);
	                            
	                            if (StringUtils.isNotBlank(awardNumber)) {
	                            	writeFullElement(xmlw, null, "awardNumber", null, awardNumber);
	                            }
	                            
	                            xmlw.writeEndElement(); // </fundingReference>
                            }
                        }
                    }
                }
            }
        }
        
        writeEndTag(xmlw, fundingReference_check);
        
        
        
        // geoLocation -> geoLocationPlace
        String geoLocation = dto2Primitive(version, DatasetFieldConstant.productionPlace);
        if (StringUtils.isNotBlank(geoLocation)) {
        	xmlw.writeStartElement("geoLocations"); // <geoLocations>
        	writeFullElement(xmlw, "geoLocation", "geoLocationPlace", null, geoLocation);
            xmlw.writeEndElement(); // </geoLocations>
        }
    }
    
    
    
    private static void writeIdentifierElement(XMLStreamWriter xmlw, String identifier) throws XMLStreamException {
    	// write the identifier
    	if (StringUtils.isNotBlank(identifier)) {
	    	Map <String, String> identifier_map = new HashMap <String, String> ();
	    	
	    	if (StringUtils.containsIgnoreCase(identifier, GlobalId.DOI_RESOLVER_URL)) {
	    		identifier_map.put("identifierType", "DOI");
	    	}
	    	else if (StringUtils.containsIgnoreCase(identifier, GlobalId.HDL_RESOLVER_URL)) {
	    		identifier_map.put("identifierType", "Handle");
	    	}
	    	
    		writeFullElement(xmlw, null, "identifier", identifier_map, identifier);
        }
    }
    
    private static void writeCreatorElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        // write all creators
    	boolean creator_check = false;
        
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                        	String creatorName = null;
                            // String creatorFirstName = null;
                            // String creatorLastName = null;
                            String affiliation = null;
                            String nameIdentifier = null;
                            String nameIdentifierScheme = null;
                            
                        	for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    creatorName = next.getSinglePrimitive();
                                }
                                /*if (DatasetFieldConstant.creatorFirstName.equals(next.getTypeName())) {
                                	creatorFirstName =  next.getSinglePrimitive();
                                }*/
                                /*if (DatasetFieldConstant.creatorLastName.equals(next.getTypeName())) {
                                	creatorLastName =   next.getSinglePrimitive();
                                }*/
                                if (DatasetFieldConstant.authorAffiliation.equals(next.getTypeName())) {
                                	affiliation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorIdValue.equals(next.getTypeName())) {
                                	nameIdentifier =   next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorIdType.equals(next.getTypeName())) {
                                	nameIdentifierScheme =  next.getSinglePrimitive();
                                }
                            }
                            
                            if (StringUtils.isNotBlank(creatorName)) {
                            	creator_check = writeOpenTag(xmlw, "creators", creator_check);
                            	xmlw.writeStartElement("creator"); // <creator>
	                            
                            	boolean nameType_check = false;
                            	Map <String, String> creator_map = new HashMap <String, String> ();
                            	if ((StringUtils.containsIgnoreCase(nameIdentifierScheme, "orcid")) || (StringUtils.isNotBlank(affiliation))) {
                            		creator_map.put("nameType", "Personal");
                            		nameType_check = true;
                            	}
                            	writeFullElement(xmlw, null, "creatorName", creator_map, creatorName);
	                            
                            	if (creatorName.contains(",")) {
	                            	if ((nameType_check) && (!creatorName.replaceFirst(",", "").contains(","))) {
	                            		String [] fullName = creatorName.split(", ");
	                            		String givenName = fullName[1];
	                            		String familyName = fullName[0];
	                            		
	                            		writeFullElement(xmlw, null, "givenName", null, givenName);
	                            		writeFullElement(xmlw, null, "familyName", null, familyName);
	                            	}
                            	}
	                            /*if (StringUtils.isNotBlank(creatorFirstName)) {
	                            	writeFullElement(xmlw, null, "givenName", null, creatorFirstName);
	                            }*/
	                            
	                            /*if (StringUtils.isNotBlank(creatorLastName)) {
	                            	writeFullElement(xmlw, null, "familyName", null, creatorLastName);
	                            }*/
	                            
	                            if (StringUtils.isNotBlank(affiliation)) {
	                            	writeFullElement(xmlw, null, "affiliation", null, affiliation);
	                            }
	                            
	                            if (StringUtils.isNotBlank(nameIdentifier)) {
	                            	creator_map.clear();
	                            	if (StringUtils.isNotBlank(nameIdentifierScheme)) {
		                            	creator_map.put("nameIdentifierScheme", nameIdentifierScheme);
		                            	writeFullElement(xmlw, null, "nameIdentifier", creator_map, nameIdentifier);
	                            	}
	                            	else {
	                            		writeFullElement(xmlw, null, "nameIdentifier", null, nameIdentifier);
	                            	}
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
    
    private static boolean writeTitleElement(XMLStreamWriter xmlw, String titleType, String title, boolean title_check) throws XMLStreamException {
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
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, String subjectScheme, String schemeURI, String value) throws XMLStreamException {
    	// write a subject
    	Map <String, String> subject_map = new HashMap <String, String> ();
    	
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
			writeFullElement(xmlw, null, "subject", subject_map, value);
		}
		else {
			writeFullElement(xmlw, null, "subject", null, value);
		}
    }
    
    private static void writeContributorElement(XMLStreamWriter xmlw, String contributorType, String contributorName, String contributorAffiliation) throws XMLStreamException {
    	// write a contributor
    	xmlw.writeStartElement("contributor"); // <contributor>
    	if (StringUtils.isNotBlank(contributorType)) {
    		xmlw.writeAttribute("contributorType", contributorType.replaceAll(" ", ""));
    	}
        
        boolean nameType_check = false;
    	Map <String, String> contributor_map = new HashMap <String, String> ();
    	// check if the name is personal (maybe you can check if there is an ORCID [ORCID define a person])
    	if (StringUtils.isNotBlank(contributorAffiliation)) {
    		contributor_map.put("nameType", "Personal");
    		nameType_check = true;
    	}
    	writeFullElement(xmlw, null, "contributorName", contributor_map, contributorName);
        
    	if (contributorName.contains(",")) {
        	if ((nameType_check) && (!contributorName.replaceFirst(",", "").contains(","))) {
        		String [] fullName = contributorName.split(", ");
        		String givenName = fullName[1];
        		String familyName = fullName[0];
        		
        		writeFullElement(xmlw, null, "givenName", null, givenName);
        		writeFullElement(xmlw, null, "familyName", null, familyName);
        	}
    	}
        
    	
        if (StringUtils.isNotBlank(contributorAffiliation)) {
        	writeFullElement(xmlw, null, "affiliation", null, contributorAffiliation);
        }
        
        xmlw.writeEndElement(); // </contributor>
    }
    
    private static void writeRightsListElement(XMLStreamWriter xmlw, DatasetVersionDTO version/*, String termsOfAccess, String restrictions*/) throws XMLStreamException {
    	// write the rights
    	xmlw.writeStartElement("rightsList"); // <rightsList>
    	
    	// set terms from the info:eu-repo-Access-Terms vocabulary
    	writeRightsHeader(xmlw);
		boolean restrict = false;
		boolean closed = false;
		
    	if (version.isFileAccessRequest()) {
    		restrict = true;
    	}
    	for (int i = 0; i < version.getFiles().size(); i++) {
        	if (version.getFiles().get(i).isRestricted()) {
        		closed= true;
        		break;
        	}
        }
    	
    	if (restrict && closed) {
    		xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/restrictedAccess");
    	}
    	else if (!restrict && closed) {
    		xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/closedAccess");
    	}
    	else {
    		xmlw.writeAttribute("rightsURI", "info:eu-repo/semantics/openAccess");
    	}
    	xmlw.writeEndElement();
    	
    	
    	// check if getLicense() method contains CC0
        // check if getTermsOfUse() method contains http:// [check if it is a rightsURI]
    	writeRightsHeader(xmlw);
    	if (StringUtils.isNotBlank(version.getLicense())) {
        	if (StringUtils.containsIgnoreCase(version.getLicense(), "cc0")) {
        		xmlw.writeAttribute("rightsURI", "https://creativecommons.org/publicdomain/zero/1.0/");
        		if (StringUtils.isNotBlank(version.getTermsOfUse())) {
        			xmlw.writeCharacters(version.getTermsOfUse());
        		}
        	}
        	else if (StringUtils.isNotBlank(version.getTermsOfUse())) {
        		if (StringUtils.containsIgnoreCase(version.getTermsOfUse(), "http")) {
            		xmlw.writeAttribute("rightsURI", version.getTermsOfUse());
            		// xmlw.writeCharacters(version.getLicense());
            	}
        		else {
        			xmlw.writeCharacters(version.getTermsOfUse());
            	}
        	}
        }
        else if (StringUtils.isNotBlank(version.getTermsOfUse())) {
    		if (StringUtils.containsIgnoreCase(version.getTermsOfUse(), "http")) {
        		xmlw.writeAttribute("rightsURI", version.getTermsOfUse());
        		// xmlw.writeCharacters(version.getLicense());
        	}
    		else {
    			xmlw.writeCharacters(version.getTermsOfUse());
        	}
    	}
    	xmlw.writeEndElement();
    	
    	xmlw.writeEndElement();
    }
    
    private static void writeRightsHeader(XMLStreamWriter xmlw) throws XMLStreamException {
    	// write the rights header
		xmlw.writeStartElement("rights"); // <rights>
        
        if (StringUtils.isNotBlank(language)) {
    		xmlw.writeAttribute("xml:lang", language);
    	}
    }
    
    private static void writeDescriptionElement(XMLStreamWriter xmlw, String descriptionType, String description) throws XMLStreamException {
    	// write a description
    	Map <String, String> description_map = new HashMap <String, String> ();
    	
    	if (StringUtils.isNotBlank(language)) {
    		description_map.put("xml:lang", language);
    	}
    	
    	description_map.put("descriptionType", descriptionType);
    	writeFullElement(xmlw, null, "description", description_map, description);
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
    
    private static void writeFullElement(XMLStreamWriter xmlw, String tag_parent, String tag_son, Map <String, String> map, String value) throws XMLStreamException {
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
	        
	        writeEndTag(xmlw, tag_son_check);
	        writeEndTag(xmlw, tag_parent_check);
        }
    }
    
    private static void writeAttribute(XMLStreamWriter xmlw, Map <String, String> map) throws XMLStreamException {
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
    		xmlw.writeEndElement(); // </close current tag>
    	}
    }
    
}
