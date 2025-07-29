package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.ForeignMetadataFieldMapping;
import edu.harvard.iq.dataverse.ForeignMetadataFormatMapping;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.*;  
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.xml.XmlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import javax.xml.stream.XMLInputFactory;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;


/**
 *
 * @author ellenk
 * @author Leonid Andreev
 * @author Bob Treacy
*/
@Stateless
@Named
public class ImportGenericServiceBean {
    private static final Logger logger = Logger.getLogger(ImportGenericServiceBean.class.getCanonicalName());
    
    @EJB
    DatasetFieldServiceBean datasetfieldService;
    
    @EJB
    DatasetFieldServiceBean datasetFieldSvc;
    
    @EJB
    MetadataBlockServiceBean blockService;
    
    @EJB
    SettingsServiceBean settingsService;

    @EJB
    LicenseServiceBean licenseService;

    @EJB
    DatasetTypeServiceBean datasetTypeService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public static String DCTERMS = "http://purl.org/dc/terms/";
    
    public ForeignMetadataFormatMapping findFormatMappingByName (String name) {
        try {
            return em.createNamedQuery("ForeignMetadataFormatMapping.findByName", ForeignMetadataFormatMapping.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public void importXML(String xmlToParse, String foreignFormat, DatasetVersion datasetVersion) {
        
        StringReader reader = null;
        XMLStreamReader xmlr = null;        

        ForeignMetadataFormatMapping mappingSupported = findFormatMappingByName (foreignFormat);
        if (mappingSupported == null) {
            throw new EJBException("Unknown/unsupported foreign metadata format "+foreignFormat);
        }
        
        try {
            reader = new StringReader(xmlToParse);
            XMLInputFactory xmlFactory = XmlUtil.getSecureXMLInputFactory();
            xmlr =  xmlFactory.createXMLStreamReader(reader);
            DatasetDTO datasetDTO = processXML(xmlr, mappingSupported);
        
            Gson gson = new Gson();
            String json = gson.toJson(datasetDTO.getDatasetVersion());
            logger.fine(json);
            JsonReader jsonReader = Json.createReader(new StringReader(json));
            JsonObject obj = jsonReader.readObject();
            DatasetVersion dv = new JsonParser(datasetFieldSvc, blockService, settingsService, licenseService, datasetTypeService).parseDatasetVersion(obj, datasetVersion);
        } catch (XMLStreamException ex) {
            //Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred while parsing XML fragment  ("+xmlToParse.substring(0, 64)+"...); ", ex);
        } catch (JsonParseException ex) {
            Logger.getLogger(ImportGenericServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}
        }
    }

    public DatasetDTO processXML( XMLStreamReader xmlr, ForeignMetadataFormatMapping foreignFormatMapping) throws XMLStreamException {
        // init - similarly to what I'm doing in the metadata extraction code? 
        DatasetDTO datasetDTO = this.initializeDataset();

        while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
//        xmlr.nextTag();
        String openingTag = foreignFormatMapping.getStartElement();
        if (openingTag != null) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, openingTag);
        } else { 
            // TODO: 
            // add support for parsing the body regardless of the start element.
            // June 20 2014 -- L.A. 
            throw new EJBException("No support for format mappings without start element defined (yet)");
        }
                
        processXMLElement(xmlr, ":", openingTag, foreignFormatMapping, datasetDTO);
  
        return datasetDTO;

    }
    
    // Helper methods for importing harvested Dublin Core xml.
    // Dublin Core is considered a mandatory, built in metadata format mapping. 
    // It is distributed as required content, in reference_data.sql. 
    // Note that arbitrary formatting tags are supported for the outer xml
    // wrapper. -- L.A. 4.5
    public DatasetDTO processOAIDCxml(String DcXmlToParse) throws XMLStreamException {
        return processOAIDCxml(DcXmlToParse, null, false);
    }
    
    public DatasetDTO processOAIDCxml(String DcXmlToParse, String oaiIdentifier, boolean preferSuppliedIdentifier) throws XMLStreamException {
        // look up DC metadata mapping: 
        
        ForeignMetadataFormatMapping dublinCoreMapping = findFormatMappingByName(DCTERMS);
        if (dublinCoreMapping == null) {
            throw new EJBException("Failed to find metadata mapping for " + DCTERMS);
        }

        DatasetDTO datasetDTO = this.initializeDataset();
        StringReader reader = null;
        XMLStreamReader xmlr = null;

        try {
            reader = new StringReader(DcXmlToParse);
            XMLInputFactory xmlFactory = XmlUtil.getSecureXMLInputFactory();
            xmlr = xmlFactory.createXMLStreamReader(reader);

            //while (xmlr.next() == XMLStreamConstants.COMMENT); // skip pre root comments
            xmlr.nextTag();

            xmlr.require(XMLStreamConstants.START_ELEMENT, null, OAI_DC_OPENING_TAG);

            processXMLElement(xmlr, ":", OAI_DC_OPENING_TAG, dublinCoreMapping, datasetDTO);
        } catch (XMLStreamException ex) {
            throw new EJBException("ERROR occurred while parsing XML fragment  (" + DcXmlToParse.substring(0, 64) + "...); ", ex);
        } finally {
            if (xmlr != null) {
                try {
                    xmlr.close();
                } catch (XMLStreamException ex) {
                }
            }
        }

        
        datasetDTO.getDatasetVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        
        // Note that in some harvesting cases we will want to use the OAI 
        // identifier (the identifier from the <header> section of the OAI 
        // record) for the global id of the harvested dataset, without expecting 
        // to find a valid persistent id in the body of the DC record. This is  
        // the use case when harvesting from DataCite: we always want to use the
        // OAI identifier, disregarding any identifiers that may be found within
        // the metadata record. 
        // 
        // Otherwise, we will look at the list of identifiers extracted from the 
        // <dc:identifier> fields in the OAI_DC record. Our DC parser uses these
        // to populate the "Other Id" field in the Citation block. The first one 
        // of these that parses as a valid Persistent Identifier will be 
        // selected to serve as the global id for the imported dataset. If none 
        // are found there, we will try to use the OAI identifier as the last 
        // resort. Note that this is the default behavior. 
        
        String candidateGlobalId = selectIdentifier(datasetDTO.getDatasetVersion(), oaiIdentifier, preferSuppliedIdentifier);
        logger.fine("Selected global identifier: " + candidateGlobalId);

        // Re-assign the selected identifier to serve as the main persistent Id:
        String globalIdentifier = reassignIdentifierAsGlobalId(candidateGlobalId, datasetDTO);
        logger.fine("Successfully re-assigned the global identifier: " + globalIdentifier);

        if (globalIdentifier == null) {
            String exceptionMsg = oaiIdentifier == null ? 
                    "Failed to find a global identifier in the OAI_DC XML record." : 
                    "Failed to parse the supplied identifier as a valid Persistent Id";
            throw new EJBException(exceptionMsg);
        }
        
        return datasetDTO;

    }
    
    private void processXMLElement(XMLStreamReader xmlr, String currentPath, String openingTag, ForeignMetadataFormatMapping foreignFormatMapping, DatasetDTO datasetDTO) throws XMLStreamException {
        logger.fine("entering processXMLElement; ("+currentPath+")");

        while (xmlr.hasNext()) {

            int event;
            try {
                event = xmlr.next();
            } catch (XMLStreamException ex) {
                logger.warning("Error occurred in the XML parsing : " + ex.getMessage());
                continue; // Skip Undeclared namespace prefix and Unexpected close tag related to com.ctc.wstx.exc.WstxParsingException
            }

            if (event == XMLStreamConstants.START_ELEMENT) {
                String currentElement = xmlr.getLocalName();
                
                ForeignMetadataFieldMapping mappingDefined = datasetfieldService.findFieldMapping(foreignFormatMapping.getName(), currentPath+currentElement);
                
                if (mappingDefined != null) {
                    DatasetFieldType mappingDefinedFieldType = datasetfieldService.findByNameOpt(mappingDefined.getDatasetfieldName());
                    boolean compound = mappingDefinedFieldType.isCompound();
                    DatasetFieldCompoundValue cachedCompoundValue = null; 
                    String dataverseFieldName = mappingDefined.getDatasetfieldName();
                    // Process attributes, if any are defined in the mapping:
                    if (mappingDefinedFieldType.isCompound()) {
                        List<HashSet<FieldDTO>> compoundField = new ArrayList<>();
                        HashSet<FieldDTO> set = new HashSet<>();
                        for (ForeignMetadataFieldMapping childMapping : mappingDefined.getChildFieldMappings()) {
                            if (childMapping.isAttribute()) {
                                String attributeName = childMapping.getForeignFieldXPath();

                                String attributeValue = xmlr.getAttributeValue(null, attributeName);
                                if (attributeValue != null) {
                                    String mappedFieldName = childMapping.getDatasetfieldName();

                                    logger.fine("looking up dataset field " + mappedFieldName);

                                    DatasetFieldType mappedFieldType = datasetfieldService.findByNameOpt(mappedFieldName);
                                    if (mappedFieldType != null) {
                                        try {
                                            addToSet(set, attributeName, attributeValue);
                                            //FieldDTO value = FieldDTO.createPrimitiveFieldDTO(attributeName, attributeValue);
//                                        FieldDTO attribute = FieldDTO.createCompoundFieldDTO(attributeName, value);
                                            //MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
                                            //citationBlock.getFields().add(value);
// TO DO replace database output with Json                                        cachedCompoundValue = createDatasetFieldValue(mappedFieldType, cachedCompoundValue, attributeValue, datasetVersion);
                                        } catch (Exception ex) {
                                            logger.warning("Caught unknown exception when processing attribute " + currentPath + currentElement + "{" + attributeName + "} (skipping);");
                                        }
                                    } else {
                                        throw new EJBException("Bad foreign metadata field mapping: no such DatasetField " + mappedFieldName + "!");
                                    }
                                }
                            }
                        }
                        if (!set.isEmpty()) {
                            compoundField.add(set);
                            MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get(mappingDefinedFieldType.getMetadataBlock().getName());
                            citationBlock.addField(FieldDTO.createMultipleCompoundFieldDTO(mappingDefined.getDatasetfieldName(), compoundField));
                        } else{
                            FieldDTO value = null;
                            if (mappingDefinedFieldType.isAllowMultiples()){
                                List<String> values = new ArrayList<>();
                                values.add(parseText(xmlr));
                                value = FieldDTO.createMultiplePrimitiveFieldDTO(dataverseFieldName, values);
                            }else {
                                value = FieldDTO.createPrimitiveFieldDTO(dataverseFieldName, parseText(xmlr));
                            }
                                
                            value = makeDTO(mappingDefinedFieldType, value, dataverseFieldName);
                            MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get(mappingDefinedFieldType.getMetadataBlock().getName());
                            citationBlock.addField(value);
                        }
                    } else if (dataverseFieldName != null && !dataverseFieldName.isEmpty()) {
                        DatasetFieldType dataverseFieldType = datasetfieldService.findByNameOpt(dataverseFieldName);
                        FieldDTO value;
                        if (dataverseFieldType != null) {

                            if (dataverseFieldType.isControlledVocabulary()) {
                                value = FieldDTO.createVocabFieldDTO(dataverseFieldName, parseText(xmlr));
                            } else {
                                value = FieldDTO.createPrimitiveFieldDTO(dataverseFieldName, parseText(xmlr));
                            }
                            value = makeDTO(dataverseFieldType, value, dataverseFieldName);
                            //  value = FieldDTO.createPrimitiveFieldDTO(dataverseFieldName, parseText(xmlr));
                            //  FieldDTO dataverseField = FieldDTO.createCompoundFieldDTO(dataverseFieldName, value);
                            MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get(mappingDefinedFieldType.getMetadataBlock().getName());
                            citationBlock.addField(value);
                            // TO DO replace database output with Json                             createDatasetFieldValue(dataverseFieldType, cachedCompoundValue, elementTextPayload, datasetVersion);
                        } else {
                            throw new EJBException("Bad foreign metadata field mapping: no such DatasetField " + dataverseFieldName + "!");
                        }
                    }
                } else {
                    // recursively, process the xml stream further down: 
                    processXMLElement(xmlr, currentPath+currentElement+":", currentElement, foreignFormatMapping, datasetDTO);
                }
                
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(openingTag)) return;
            }
        }
    }

    private FieldDTO makeDTO(DatasetFieldType dataverseFieldType, FieldDTO value, String dataverseFieldName) {
        if (dataverseFieldType.isAllowMultiples()){
            if(dataverseFieldType.isCompound()) {
                value = FieldDTO.createMultipleCompoundFieldDTO(dataverseFieldName, value);
            } else if (dataverseFieldType.isControlledVocabulary()) {
                value = FieldDTO.createMultipleVocabFieldDTO(dataverseFieldName, Arrays.asList(value.getSinglePrimitive()));
            } else {
                value = FieldDTO.createMultiplePrimitiveFieldDTO(dataverseFieldName, Arrays.asList(value.getSinglePrimitive()));
            }
            if (dataverseFieldType.isChild()) {
                DatasetFieldType parentDatasetFieldType = dataverseFieldType.getParentDatasetFieldType();
                if (parentDatasetFieldType.isAllowMultiples()) {
                    value = FieldDTO.createMultipleCompoundFieldDTO(parentDatasetFieldType.getName(), value);
                    
                }
            }
        } else{
            if (dataverseFieldType.isCompound()){
                value = FieldDTO.createCompoundFieldDTO(dataverseFieldName, value);
            }
        }
        
        // TODO: 
        // it looks like the code below has already been executed, in one of the 
        // if () blocks above... is this ok to be doing it again?? -- L.A. 4.5
        if (dataverseFieldType.isChild()) {
                                    DatasetFieldType parentDatasetFieldType = dataverseFieldType.getParentDatasetFieldType();
                                    if (parentDatasetFieldType.isAllowMultiples()) {
                                        value = FieldDTO.createMultipleCompoundFieldDTO(parentDatasetFieldType.getName(), value);

                                    }
        }
        return value;
    }
    
    public String selectIdentifier(DatasetVersionDTO datasetVersionDTO, String suppliedIdentifier) {
        return selectIdentifier(datasetVersionDTO, suppliedIdentifier, false);
    }
    
    private String selectIdentifier(DatasetVersionDTO datasetVersionDTO, String suppliedIdentifier, boolean preferSuppliedIdentifier) {
        List<String> otherIds = new ArrayList<>();
        
        if (suppliedIdentifier != null && preferSuppliedIdentifier) {
            // This supplied identifier (in practice, his is likely the OAI-PMH 
            // identifier from the <record> <header> section) will be our first 
            // choice candidate for the pid of the imported dataset:
            otherIds.add(suppliedIdentifier);
        }
        
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (FieldDTO next : foo) {
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                    otherIds.add(next.getSinglePrimitive());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (suppliedIdentifier != null && !preferSuppliedIdentifier) {
            // Unless specifically instructed to prefer this extra identifier 
            // (in practice, this is likely the OAI-PMH identifier from the 
            // <record> <header> section), we will try to use it as the *last*
            // possible candidate for the pid, so, adding it to the end of the 
            // list:
            otherIds.add(suppliedIdentifier);
        }
        
        if (!otherIds.isEmpty()) {
            // We prefer doi or hdl identifiers like "doi:10.7910/DVN/1HE30F"
            for (String otherId : otherIds) {
                if (otherId.startsWith(AbstractDOIProvider.DOI_PROTOCOL) || otherId.startsWith(HandlePidProvider.HDL_PROTOCOL) || otherId.startsWith(AbstractDOIProvider.DOI_RESOLVER_URL) || otherId.startsWith(HandlePidProvider.HDL_RESOLVER_URL) || otherId.startsWith(AbstractDOIProvider.HTTP_DOI_RESOLVER_URL) || otherId.startsWith(HandlePidProvider.HTTP_HDL_RESOLVER_URL) || otherId.startsWith(AbstractDOIProvider.DXDOI_RESOLVER_URL) || otherId.startsWith(AbstractDOIProvider.HTTP_DXDOI_RESOLVER_URL)) {
                    return otherId;
                }
            }
            // But identifiers without hdl or doi like "10.6084/m9.figshare.12725075.v1" are also allowed
            for (String otherId : otherIds) {
                try {
                    HandleResolver hr = new HandleResolver();
                    hr.resolveHandle(otherId);
                    return HandlePidProvider.HDL_PROTOCOL + ":" + otherId;
                } catch (HandleException e) {
                    logger.fine("Not a valid handle: " + e.toString());
                }
            }
        }
        return null;
    }
    
    /* This is a general parser that can take DOI and Handle Ids, in their local or
     * URL forms (e.g. doi:... or https://doi.org/...) and parse them into
     * protocol/authority/identifier parts that are assigned to the datasetDTO.
     * The name reflects the original purpose but it is now used in ImportDDIServiceBean as well.
     */
    
    //ToDo - sync with GlobalId.parsePersistentId(String) ? - that currently doesn't do URL forms, but could
    public String reassignIdentifierAsGlobalId(String identifierString, DatasetDTO datasetDTO) {

        int index1 = identifierString.indexOf(':');
        int index2 = identifierString.indexOf('/');
        if (index1==-1) {
            logger.warning("Error parsing identifier: " + identifierString + ". ':' not found in string");
            return null; 
        }  
       
        String protocol = identifierString.substring(0, index1);
        
        if (AbstractDOIProvider.DOI_PROTOCOL.equals(protocol) || HandlePidProvider.HDL_PROTOCOL.equals(protocol) || PermaLinkPidProvider.PERMA_PROTOCOL.equals(protocol)) {
            logger.fine("Processing hdl:- or doi:- or perma:-style identifier : "+identifierString);        
        
        } else if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            
            // We also recognize global identifiers formatted as global resolver URLs:
            //ToDo - refactor index1 always has -1 here so that we can use index1+1 later
            //ToDo - single map of protocol/url, are all three cases the same then?
            if (identifierString.startsWith(HandlePidProvider.HDL_RESOLVER_URL) || identifierString.startsWith(HandlePidProvider.HTTP_HDL_RESOLVER_URL)) {
                logger.fine("Processing Handle identifier formatted as a resolver URL: "+identifierString);
                protocol = HandlePidProvider.HDL_PROTOCOL;
                index1 = (identifierString.startsWith(HandlePidProvider.HDL_RESOLVER_URL)) ? HandlePidProvider.HDL_RESOLVER_URL.length() - 1 : HandlePidProvider.HTTP_HDL_RESOLVER_URL.length() - 1;
                index2 = identifierString.indexOf("/", index1 + 1);
            } else if (identifierString.startsWith(AbstractDOIProvider.DOI_RESOLVER_URL) || identifierString.startsWith(AbstractDOIProvider.HTTP_DOI_RESOLVER_URL) || identifierString.startsWith(AbstractDOIProvider.DXDOI_RESOLVER_URL) || identifierString.startsWith(AbstractDOIProvider.HTTP_DXDOI_RESOLVER_URL)) {
                logger.fine("Processing DOI identifier formatted as a resolver URL: "+identifierString);
                protocol = AbstractDOIProvider.DOI_PROTOCOL;
                identifierString = identifierString.replace(AbstractDOIProvider.DXDOI_RESOLVER_URL, AbstractDOIProvider.DOI_RESOLVER_URL);
                identifierString = identifierString.replace(AbstractDOIProvider.HTTP_DXDOI_RESOLVER_URL, AbstractDOIProvider.HTTP_DOI_RESOLVER_URL);
                index1 = (identifierString.startsWith(AbstractDOIProvider.DOI_RESOLVER_URL)) ? AbstractDOIProvider.DOI_RESOLVER_URL.length() - 1 : AbstractDOIProvider.HTTP_DOI_RESOLVER_URL.length() - 1;
                index2 = identifierString.indexOf("/", index1 + 1);
            } else if (identifierString.startsWith(PermaLinkPidProvider.PERMA_RESOLVER_URL + Dataset.TARGET_URL)) {
                protocol = PermaLinkPidProvider.PERMA_PROTOCOL;
                index1 = PermaLinkPidProvider.PERMA_RESOLVER_URL.length() + + Dataset.TARGET_URL.length() - 1; 
                index2 = identifierString.indexOf("/", index1 + 1);
            } else {
                logger.warning("HTTP Url in supplied as the identifier is neither a Handle nor DOI resolver: "+identifierString);
                return null;
            }
        } else {
            logger.warning("Unknown identifier format: "+identifierString);
            return null; 
        }
        
        if (index2 == -1) {
            logger.warning("Error parsing identifier: " + identifierString + ". Second '/' not found in string");
            return null;
        }

        String authority = identifierString.substring(index1 + 1, index2);
        String identifier = identifierString.substring(index2 + 1);

        datasetDTO.setProtocol(protocol);
        datasetDTO.setAuthority(authority);
        datasetDTO.setIdentifier(identifier);

        // reassemble and return: 
        logger.fine("parsed identifier, finalized " + protocol + ":" + authority + "/" + identifier);
        return protocol + ":" + authority + "/" + identifier;
    }
        
        
    public static final String OAI_DC_OPENING_TAG = "dc";
    public static final String DCTERMS_OPENING_TAG = "dcterms";
    
    public static final String SOURCE_DVN_3_0 = "DVN_3_0";
    
    public static final String NAMING_PROTOCOL_HANDLE = "hdl";
    public static final String NAMING_PROTOCOL_DOI = "doi";
    public static final String AGENCY_HANDLE = "handle";
    public static final String AGENCY_DOI = "DOI";
    public static final String REPLICATION_FOR_TYPE = "replicationFor";
    public static final String VAR_WEIGHTED = "wgtd";
    public static final String VAR_INTERVAL_CONTIN = "contin";
    public static final String VAR_INTERVAL_DISCRETE = "discrete";
    public static final String CAT_STAT_TYPE_FREQUENCY = "freq";
    public static final String VAR_FORMAT_TYPE_NUMERIC = "numeric";
    public static final String VAR_FORMAT_SCHEMA_ISO = "ISO";
    

    public static final String EVENT_START = "start";
    public static final String EVENT_END = "end";
    public static final String EVENT_SINGLE = "single";

    public static final String LEVEL_DVN = "dvn";
    public static final String LEVEL_DV = "dv";
    public static final String LEVEL_STUDY = "study";
    public static final String LEVEL_FILE = "file";
    public static final String LEVEL_VARIABLE = "variable";
    public static final String LEVEL_CATEGORY = "category";

    public static final String NOTE_TYPE_UNF = "VDC:UNF";
    public static final String NOTE_SUBJECT_UNF = "Universal Numeric Fingerprint";

    public static final String NOTE_TYPE_TERMS_OF_USE = "DVN:TOU";
    public static final String NOTE_SUBJECT_TERMS_OF_USE = "Terms Of Use";

    public static final String NOTE_TYPE_CITATION = "DVN:CITATION";
    public static final String NOTE_SUBJECT_CITATION = "Citation";

    public static final String NOTE_TYPE_VERSION_NOTE = "DVN:VERSION_NOTE";
    public static final String NOTE_SUBJECT_VERSION_NOTE= "Version Note";

    public static final String NOTE_TYPE_ARCHIVE_NOTE = "DVN:ARCHIVE_NOTE";
    public static final String NOTE_SUBJECT_ARCHIVE_NOTE= "Archive Note";

    public static final String NOTE_TYPE_ARCHIVE_DATE = "DVN:ARCHIVE_DATE";
    public static final String NOTE_SUBJECT_ARCHIVE_DATE= "Archive Date";
    
    public static final String NOTE_TYPE_EXTENDED_METADATA = "DVN:EXTENDED_METADATA";

    public static final String NOTE_TYPE_LOCKSS_CRAWL = "LOCKSS:CRAWLING";
    public static final String NOTE_SUBJECT_LOCKSS_PERM = "LOCKSS Permission";

    public static final String NOTE_TYPE_REPLICATION_FOR = "DVN:REPLICATION_FOR";
    private XMLInputFactory xmlInputFactory = null;
    private ImportType importType;
    public enum ImportType{ NEW, MIGRATION, HARVEST};

    public ImportGenericServiceBean() {
    }
     
    public ImportGenericServiceBean(ImportType importType) {
        this.importType=importType;
        xmlInputFactory = XmlUtil.getSecureXMLInputFactory();
    }
    
      
    public DatasetDTO doImport(String xmlToParse) throws XMLStreamException {
        DatasetDTO datasetDTO = this.initializeDataset();

        // Read docDescr and studyDesc into DTO objects.
        Map<String, String> fileMap = mapDCTerms(xmlToParse, datasetDTO);
        if (!importType.equals(ImportType.MIGRATION)) {
                  //EMK TODO:  Call methods for reading FileMetadata and related objects from xml, return list of FileMetadata objects.
                   /*try {
            
             Map<String, DataTable> dataTableMap = new DataTableImportDDI().processDataDscr(xmlr);
             } catch(Exception e) {
            
             }*/
        }
        return datasetDTO;
    }
    
    public Map<String, String> mapDCTerms(String xmlToParse, DatasetDTO datasetDTO) throws XMLStreamException {

        Map<String, String> filesMap = new HashMap<>();
        StringReader reader = new StringReader(xmlToParse);
        XMLStreamReader xmlr = null;
        XMLInputFactory xmlFactory = XmlUtil.getSecureXMLInputFactory();
        xmlr = xmlFactory.createXMLStreamReader(reader);
        processDCTerms(xmlr, datasetDTO, filesMap);
        if (xmlr != null) {
            try {
                xmlr.close();
            } catch (XMLStreamException ex) {
            }
        }
        return filesMap;
    }
   
 
    public Map<String, String> mapDCTerms(File ddiFile, DatasetDTO datasetDTO) {
        XMLStreamReader xmlr = null;
        Map<String, String> filesMap = new HashMap<>();

        try (FileInputStream in = new FileInputStream(ddiFile)) {
            xmlr =  xmlInputFactory.createXMLStreamReader(in);
            processDCTerms( xmlr,  datasetDTO , filesMap );
        } catch (FileNotFoundException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI: File Not Found!");
        } catch (XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI.", ex);
        } catch (IOException e) {
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}
        }

        return filesMap;
    }
    
    private void processDCTerms(XMLStreamReader xmlr, DatasetDTO datasetDTO, Map<String, String> filesMap) throws XMLStreamException {
       
        // make sure we have a codeBook
        //while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
        xmlr.nextTag();
        MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
     
/*         if (codeBookLevelId != null && !codeBookLevelId.equals("")) {
            if (citationBlock.getField("otherId")==null) {
                // this means no ids were found during the parsing of the 
                // study description section. we'll use the one we found in 
                // the codeBook entry:
                FieldDTO otherIdValue = FieldDTO.createPrimitiveFieldDTO("otherIdValue", codeBookLevelId);
                FieldDTO otherId = FieldDTO.createCompoundFieldDTO("otherId", otherIdValue);
                citationBlock.getFields().add(otherId);
                
          } 
        }*/ 
        

    }
    // EMK TODO: update unit test so this doesn't have to be public
    public DatasetDTO initializeDataset() {
        DatasetDTO  datasetDTO = new DatasetDTO();
        DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
        datasetDTO.setDatasetVersion(datasetVersionDTO);
        HashMap<String, MetadataBlockDTO> metadataBlocks = new HashMap<>();
        datasetVersionDTO.setMetadataBlocks(metadataBlocks);
        
        datasetVersionDTO.getMetadataBlocks().put("citation", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("citation").setFields(new ArrayList<>());
        datasetVersionDTO.getMetadataBlocks().put("geospatial", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("geospatial").setFields(new ArrayList<>());
        datasetVersionDTO.getMetadataBlocks().put("social_science", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("social_science").setFields(new ArrayList<>());
        datasetVersionDTO.getMetadataBlocks().put("astrophysics", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("astrophysics").setFields(new ArrayList<>());
     
        return datasetDTO;
        
    }
     private String parseText(XMLStreamReader xmlr) throws XMLStreamException {
        return parseText(xmlr,true);
     }

     private String parseText(XMLStreamReader xmlr, boolean scrubText) throws XMLStreamException {
        String tempString = getElementText(xmlr);
        if (scrubText) {
            tempString = tempString.trim().replace('\n',' ');
        }
        return tempString;
     }
     private String parseDate (XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        String date = xmlr.getAttributeValue(null, "date");
        if (date == null) {
            date = parseText(xmlr);
        }
        return date;
    } 
 /* We had to add this method because the ref getElementText has a bug where it
     * would append a null before the text, if there was an escaped apostrophe; it appears
     * that the code finds an null ENTITY_REFERENCE in this case which seems like a bug;
     * the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
    private String getElementText(XMLStreamReader xmlr) throws XMLStreamException {
        if(xmlr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", xmlr.getLocation());
        }
        int eventType = xmlr.next();
        StringBuilder content = new StringBuilder();
        while(eventType != XMLStreamConstants.END_ELEMENT ) {
            if(eventType == XMLStreamConstants.CHARACTERS
            || eventType == XMLStreamConstants.CDATA
            || eventType == XMLStreamConstants.SPACE
            /* || eventType == XMLStreamConstants.ENTITY_REFERENCE*/) {
                content.append(xmlr.getText());
            } else if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT
                || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                // skipping
            } else if(eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if(eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", xmlr.getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type "+eventType, xmlr.getLocation());
            }
            eventType = xmlr.next();
        }
        return content.toString();
    }
    
   
    
   private Map<String,String> parseCompoundText (XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        Map<String,String> returnMap = new HashMap<>();
        String text = "";

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                if (!text.isEmpty()) {
                    text += "\n";
                }
                text += xmlr.getText().trim().replace('\n',' ');
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("ExtLink")) {
                    String mapKey  = ("image".equalsIgnoreCase( xmlr.getAttributeValue(null, "role") ) || "logo".equalsIgnoreCase(xmlr.getAttributeValue(null, "title")))? "logo" : "url";
                    returnMap.put( mapKey, xmlr.getAttributeValue(null, "URI") );
                    parseText(xmlr, "ExtLink"); // this line effectively just skips though until the end of the tag
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag)) break;
            }
        }

        returnMap.put( "name", text );
        return returnMap;
    }
   
    private String parseText(XMLStreamReader xmlr, String endTag) throws XMLStreamException {
         return (String) parseTextNew(xmlr,endTag);
     }
     
     
     private Object parseTextNew(XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        String returnString = "";
        Map<String, Object> returnMap = null;

        while (true) {
            if (!returnString.isEmpty()) {
                returnString += "\n";
            }
            int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                returnString += xmlr.getText().trim().replace('\n',' ');
           } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("p")) {
                    returnString += "<p>" + parseText(xmlr, "p") + "</p>";
                } else if (xmlr.getLocalName().equals("emph")) {
                    returnString += "<em>" + parseText(xmlr, "emph") + "</em>";
                } else if (xmlr.getLocalName().equals("hi")) {
                    returnString += "<strong>" + parseText(xmlr, "hi") + "</strong>";
                } else if (xmlr.getLocalName().equals("ExtLink")) {
                    String uri = xmlr.getAttributeValue(null, "URI");
                    String text = parseText(xmlr, "ExtLink").trim();
                    returnString += "<a href=\"" + uri + "\">" + ( StringUtil.isEmpty(text) ? uri : text) + "</a>";
                } else if (xmlr.getLocalName().equals("list")) {
                    returnString += parseText_list(xmlr);
                } else if (xmlr.getLocalName().equals("citation")) {
                    if (SOURCE_DVN_3_0.equals(xmlr.getAttributeValue(null, "source")) ) {
                        returnMap = parseDVNCitation(xmlr);
                    } else {
                        returnString += parseText_citation(xmlr);
                    }
                } else {
                    throw new EJBException("ERROR occurred in mapDDI (parseText): tag not yet supported: <" + xmlr.getLocalName() + ">" );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag)) break;
            }
        }
        
        if (returnMap != null) {
            // this is one of our new citation areas for DVN3.0
            return returnMap;
        }
      
        // otherwise it's a standard section and just return the String like we always did
        return returnString.trim();
    }
     
    private String parseNoteByType(XMLStreamReader xmlr, String type) throws XMLStreamException {
        if (type.equalsIgnoreCase(xmlr.getAttributeValue(null, "type"))) {
            return parseText(xmlr);
        } else {
            return null;
        }
    }
  private String parseText_list (XMLStreamReader xmlr) throws XMLStreamException {
        String listString = null;
        String listCloseTag = null;

        // check type
        String listType = xmlr.getAttributeValue(null, "type");
        if ("bulleted".equals(listType) ){
            listString = "<ul>\n";
            listCloseTag = "</ul>";
        } else if ("ordered".equals(listType) ) {
            listString = "<ol>\n";
            listCloseTag = "</ol>";
        } else {
            // this includes the default list type of "simple"
            throw new EJBException("ERROR occurred in mapDDI (parseText): ListType of types other than {bulleted, ordered} not currently supported.");
        }

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("itm")) {
                    listString += "<li>" + parseText(xmlr,"itm") + "</li>\n";
                } else {
                    throw new EJBException("ERROR occurred in mapDDI (parseText): ListType does not currently supported contained LabelType.");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("list")) break;
            }
        }

        return (listString + listCloseTag);
    }

    private String parseText_citation (XMLStreamReader xmlr) throws XMLStreamException {
        String citation = "<!--  parsed from DDI citation title and holdings -->";
        boolean addHoldings = false;
        String holdings = "";

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("titlStmt")) {
                    while (true) {
                        event = xmlr.next();
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            if (xmlr.getLocalName().equals("titl")) {
                                citation += parseText(xmlr);
                            }
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if (xmlr.getLocalName().equals("titlStmt")) break;
                        }
                    }
                } else if (xmlr.getLocalName().equals("holdings")) {
                    String uri = xmlr.getAttributeValue(null, "URI");
                    String holdingsText = parseText(xmlr);

                    if ( !StringUtil.isEmpty(uri) || !StringUtil.isEmpty(holdingsText)) {
                        holdings += addHoldings ? ", " : "";
                        addHoldings = true;

                        if ( StringUtil.isEmpty(uri) ) {
                            holdings += holdingsText;
                        } else if ( StringUtil.isEmpty(holdingsText) ) {
                            holdings += "<a href=\"" + uri + "\">" + uri + "</a>";
                        } else {
                            // both uri and text have values
                            holdings += "<a href=\"" + uri + "\">" + holdingsText + "</a>";
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation")) break;
            }
        }

        if (addHoldings) {
            citation += " (" + holdings + ")";
        }

        return citation;
    }
  
    private String parseUNF(String unfString) {
        if (unfString.contains("UNF:")) {
            return unfString.substring( unfString.indexOf("UNF:") );
        } else {
            return null;
        }
    }
  
    private Map<String, Object> parseDVNCitation(XMLStreamReader xmlr) throws XMLStreamException {
        Map<String, Object> returnValues = new HashMap<>();
        
        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
               if (xmlr.getLocalName().equals("IDNo")) {
                    returnValues.put("idType", xmlr.getAttributeValue(null, "agency") );
                    returnValues.put("idNumber", parseText(xmlr) );                   
               }
                else if (xmlr.getLocalName().equals("biblCit")) {
                    returnValues.put("text", parseText(xmlr) );                   
                }
                else if (xmlr.getLocalName().equals("holdings")) {
                    returnValues.put("url", xmlr.getAttributeValue(null, "URI") );                 
                }
                else if (xmlr.getLocalName().equals("notes")) {
                    if (NOTE_TYPE_REPLICATION_FOR.equals(xmlr.getAttributeValue(null, "type")) ) {
                        returnValues.put("replicationData", true);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation")) break;
            }
        } 
        
        return returnValues;
    }    
     

    private void addToSet(HashSet<FieldDTO> set, String typeName, String value ) {
        if (value!=null) {
            set.add(FieldDTO.createPrimitiveFieldDTO(typeName, value));
        }
    }
    
}

