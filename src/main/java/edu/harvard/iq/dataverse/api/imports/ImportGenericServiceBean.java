package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.ForeignMetadataFieldMapping;
import edu.harvard.iq.dataverse.ForeignMetadataFormatMapping;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.*;  
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLInputFactory;


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
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlr =  xmlFactory.createXMLStreamReader(reader);
            DatasetDTO datasetDTO = processXML(xmlr, mappingSupported);
        
            Gson gson = new Gson();
            String json = gson.toJson(datasetDTO.getDatasetVersion());
            logger.fine(json);
            JsonReader jsonReader = Json.createReader(new StringReader(json));
            JsonObject obj = jsonReader.readObject();
            DatasetVersion dv = new JsonParser(datasetFieldSvc, blockService, settingsService).parseDatasetVersion(obj, datasetVersion);
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
    
    public void importXML(File xmlFile, String foreignFormat, DatasetVersion datasetVersion) {
        
        FileInputStream in = null;
        XMLStreamReader xmlr = null;

        // look up the foreign metadata mapping for this format:
        
        ForeignMetadataFormatMapping mappingSupported = findFormatMappingByName (foreignFormat);
        if (mappingSupported == null) {
            throw new EJBException("Unknown/unsupported foreign metadata format "+foreignFormat);
        }
        
        try {
            in = new FileInputStream(xmlFile);
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlr =  xmlFactory.createXMLStreamReader(in);
            DatasetDTO datasetDTO = processXML(xmlr, mappingSupported);

            Gson gson = new Gson();
            String json = gson.toJson(datasetDTO.getDatasetVersion());
            logger.info("Json:\n"+json);
            JsonReader jsonReader = Json.createReader(new StringReader(json));
            JsonObject obj = jsonReader.readObject();
            DatasetVersion dv = new JsonParser(datasetFieldSvc, blockService, settingsService).parseDatasetVersion(obj, datasetVersion);
        } catch (FileNotFoundException ex) {
            //Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI: File Not Found!");
        } catch (XMLStreamException ex) {
            //Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred while parsing XML (file "+xmlFile.getAbsolutePath()+"); ", ex);
        } catch (JsonParseException ex) {
            Logger.getLogger(ImportGenericServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}

            try {
                if (in != null) { in.close();}
            } catch (IOException ex) {}
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
    
    // Helper method for importing harvested Dublin Core xml.
    // Dublin Core is considered a mandatory, built in metadata format mapping. 
    // It is distributed as required content, in reference_data.sql. 
    // Note that arbitrary formatting tags are supported for the outer xml
    // wrapper. -- L.A. 4.5
    public DatasetDTO processOAIDCxml(String DcXmlToParse) throws XMLStreamException {
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
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlr = xmlFactory.createXMLStreamReader(reader);

            //while (xmlr.next() == XMLStreamConstants.COMMENT); // skip pre root comments
            xmlr.nextTag();

            xmlr.require(XMLStreamConstants.START_ELEMENT, null, OAI_DC_OPENING_TAG);

            processXMLElement(xmlr, ":", OAI_DC_OPENING_TAG, dublinCoreMapping, datasetDTO);
        } catch (XMLStreamException ex) {
            throw new EJBException("ERROR occurred while parsing XML fragment  (" + DcXmlToParse.substring(0, 64) + "...); ", ex);
        }

        
        datasetDTO.getDatasetVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        
        // Our DC import handles the contents of the dc:identifier field 
        // as an "other id". In the context of OAI harvesting, we expect 
        // the identifier to be a global id, so we need to rearrange that: 
        
        String identifier = getOtherIdFromDTO(datasetDTO.getDatasetVersion());
        logger.fine("Imported identifier: "+identifier);
        
        String globalIdentifier = reassignIdentifierAsGlobalId(identifier, datasetDTO);
        logger.fine("Detected global identifier: "+globalIdentifier);
        
        if (globalIdentifier == null) {
            throw new EJBException("Failed to find a global identifier in the OAI_DC XML record.");
        }
        
        return datasetDTO;

    }
    
    private void processXMLElement(XMLStreamReader xmlr, String currentPath, String openingTag, ForeignMetadataFormatMapping foreignFormatMapping, DatasetDTO datasetDTO) throws XMLStreamException {
        logger.fine("entering processXMLElement; ("+currentPath+")");
        
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
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
                    } else // Process the payload of this XML element:
                    //xxString dataverseFieldName = mappingDefined.getDatasetfieldName();
                    if (dataverseFieldName != null && !dataverseFieldName.isEmpty()) {
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
    
    private String getOtherIdFromDTO(DatasetVersionDTO datasetVersionDTO) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        String otherId = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (FieldDTO next : foo) {
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                    otherId =  next.getSinglePrimitive();
                                }
                            }
                            if (!otherId.isEmpty()){
                                return otherId;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private String reassignIdentifierAsGlobalId(String identifierString, DatasetDTO datasetDTO) {

        int index1 = identifierString.indexOf(':');
        int index2 = identifierString.indexOf('/');
        if (index1==-1) {
            logger.warning("Error parsing identifier: " + identifierString + ". ':' not found in string");
            return null; 
        }  
       
        String protocol = identifierString.substring(0, index1);
        
        if (GlobalId.DOI_PROTOCOL.equals(protocol) || GlobalId.HDL_PROTOCOL.equals(protocol)) {
            logger.fine("Processing hdl:- or doi:-style identifier : "+identifierString);        
        
        } else if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            
            // We also recognize global identifiers formatted as global resolver URLs:
            
            if (identifierString.startsWith(GlobalId.HDL_RESOLVER_URL)) {
                logger.fine("Processing Handle identifier formatted as a resolver URL: "+identifierString);
                protocol = GlobalId.HDL_PROTOCOL;
                index1 = GlobalId.HDL_RESOLVER_URL.length() - 1;
                index2 = identifierString.indexOf("/", index1 + 1);
            } else if (identifierString.startsWith(GlobalId.DOI_RESOLVER_URL)) {
                logger.fine("Processing DOI identifier formatted as a resolver URL: "+identifierString);
                protocol = GlobalId.DOI_PROTOCOL;
                index1 = GlobalId.DOI_RESOLVER_URL.length() - 1; 
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
        xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

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
    
     public void importDCTerms(String xmlToParse, DatasetVersion datasetVersion, DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService, SettingsServiceBean settingsService) {
        DatasetDTO datasetDTO = this.initializeDataset();
        try {
            // Read docDescr and studyDesc into DTO objects.
            Map<String, String> fileMap = mapDCTerms(xmlToParse, datasetDTO);
            // 
            // convert DTO to Json, 
            Gson gson = new Gson();
            String json = gson.toJson(datasetDTO.getDatasetVersion());
            JsonReader jsonReader = Json.createReader(new StringReader(json));
            JsonObject obj = jsonReader.readObject();
            //and call parse Json to read it into a datasetVersion
            DatasetVersion dv = new JsonParser(datasetFieldSvc, blockService, settingsService).parseDatasetVersion(obj, datasetVersion);
        } catch (XMLStreamException | JsonParseException e) {
            // EMK TODO: exception handling
            e.printStackTrace();
        }
        
        //EMK TODO:  Call methods for reading FileMetadata and related objects from xml, return list of FileMetadata objects.
        /*try {
            
         Map<String, DataTable> dataTableMap = new DataTableImportDDI().processDataDscr(xmlr);
         } catch(Exception e) {
            
         }*/
        // Save Dataset and DatasetVersion in database
    }

    public Map<String, String> mapDCTerms(String xmlToParse, DatasetDTO datasetDTO) throws XMLStreamException {

        Map<String, String> filesMap = new HashMap<>();
        StringReader reader = new StringReader(xmlToParse);
        XMLStreamReader xmlr = null;
        XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlr = xmlFactory.createXMLStreamReader(reader);
        processDCTerms(xmlr, datasetDTO, filesMap);

        return filesMap;
    }
   
 
    public Map<String, String> mapDCTerms(File ddiFile, DatasetDTO datasetDTO) {
        FileInputStream in = null;
        XMLStreamReader xmlr = null;
        Map<String, String> filesMap = new HashMap<>();

        try {
            in = new FileInputStream(ddiFile);
            xmlr =  xmlInputFactory.createXMLStreamReader(in);
            processDCTerms( xmlr,  datasetDTO , filesMap );
        } catch (FileNotFoundException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI: File Not Found!");
        } catch (XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI.", ex);
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}

            try {
                if (in != null) { in.close();}
            } catch (IOException ex) {}
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

