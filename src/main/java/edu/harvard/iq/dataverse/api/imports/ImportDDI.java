package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.*;  
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
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
import javax.ejb.EJBException;
import javax.json.Json;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.stream.XMLInputFactory;


/**
 *
 * @author ellenk
 */
public class ImportDDI {
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
     
    public ImportDDI(ImportType importType) {
        this.importType=importType;
        xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

    }
    
      
    public DatasetDTO doImport(String xmlToParse) throws XMLStreamException {
        DatasetDTO datasetDTO = this.initializeDataset();

        // Read docDescr and studyDesc into DTO objects.
        Map fileMap = mapDDI(xmlToParse, datasetDTO);
        if (!importType.equals(ImportType.MIGRATION)) {
                  //EMK TODO:  Call methods for reading FileMetadata and related objects from xml, return list of FileMetadata objects.
                   /*try {
            
             Map<String, DataTable> dataTableMap = new DataTableImportDDI().processDataDscr(xmlr);
             } catch(Exception e) {
            
             }*/
        }
        return datasetDTO;
    }
    
     public void importDDI(String xmlToParse, DatasetVersion datasetVersion, DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService, SettingsServiceBean settingsService) {
        DatasetDTO datasetDTO = this.initializeDataset();
        try {
            // Read docDescr and studyDesc into DTO objects.
            Map fileMap = mapDDI(xmlToParse, datasetDTO);
            // 
            // convert DTO to Json, 
            Gson gson = new Gson();
            String json = gson.toJson(datasetDTO.getDatasetVersion());
            JsonReader jsonReader = Json.createReader(new StringReader(json));
            JsonObject obj = jsonReader.readObject();
            //and call parse Json to read it into a datasetVersion
            DatasetVersion dv = new JsonParser(datasetFieldSvc, blockService, settingsService).parseDatasetVersion(obj, datasetVersion);
        } catch (Exception e) {
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

    public Map mapDDI(String xmlToParse, DatasetDTO datasetDTO) throws XMLStreamException {

        Map filesMap = new HashMap();
        StringReader reader = new StringReader(xmlToParse);
        XMLStreamReader xmlr = null;
        XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlr = xmlFactory.createXMLStreamReader(reader);
        processDDI(xmlr, datasetDTO, filesMap);

        return filesMap;
    }
   
 
    public Map mapDDI(File ddiFile,  DatasetDTO datasetDTO ) {
        FileInputStream in = null;
        XMLStreamReader xmlr = null;
        Map filesMap = new HashMap();

        try {
            in = new FileInputStream(ddiFile);
            xmlr =  xmlInputFactory.createXMLStreamReader(in);
            processDDI( xmlr,  datasetDTO , filesMap );
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
    private void processDDI( XMLStreamReader xmlr, DatasetDTO datasetDTO, Map filesMap) throws XMLStreamException {
       
        // make sure we have a codeBook
        //while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
        xmlr.nextTag();
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, "codeBook");

        // Some DDIs provide an ID in the <codeBook> section.
        // We are going to treat it as just another otherId.
        // (we've seen instances where this ID was the only ID found in
        // in a harvested DDI).

        String codeBookLevelId = xmlr.getAttributeValue(null, "ID");
        
        // (but first we will parse and process the entire DDI - and only 
        // then add this codeBook-level id to the list of identifiers; i.e., 
        // we don't want it to be the first on the list, if one or more
        // ids are available in the studyDscr section - those should take 
        // precedence!)
        // In fact, we should only use these IDs when no ID is available down 
        // in the study description section!      
        
        processCodeBook(xmlr,  datasetDTO, filesMap);
        MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
     
         if (codeBookLevelId != null && !codeBookLevelId.equals("")) {
            if (citationBlock.getField("otherId")==null) {
                // this means no ids were found during the parsing of the 
                // study description section. we'll use the one we found in 
                // the codeBook entry:
                FieldDTO otherIdValue = FieldDTO.createPrimitiveFieldDTO("otherIdValue", codeBookLevelId);
                FieldDTO otherId = FieldDTO.createCompoundFieldDTO("otherId", otherIdValue);
                citationBlock.getFields().add(otherId);
                
            }
        }
        

    }
    // EMK TODO: update unit test so this doesn't have to be public
    public DatasetDTO initializeDataset() {
        DatasetDTO  datasetDTO = new DatasetDTO();
        DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
        datasetDTO.setDatasetVersion(datasetVersionDTO);
        HashMap<String, MetadataBlockDTO> metadataBlocks = new HashMap<>();
        datasetVersionDTO.setMetadataBlocks(metadataBlocks);
        
        datasetVersionDTO.getMetadataBlocks().put("citation", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("citation").setFields(new ArrayList<FieldDTO>());
        datasetVersionDTO.getMetadataBlocks().put("socialscience", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("socialscience").setFields(new ArrayList<FieldDTO>());
        datasetVersionDTO.getMetadataBlocks().put("geospatial", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("geospatial").setFields(new ArrayList<FieldDTO>());
     
        return datasetDTO;
        
    }
       // Read the XMLStream, and populate datasetDTO and filesMap
       private void processCodeBook( XMLStreamReader xmlr, DatasetDTO datasetDTO, Map filesMap) throws XMLStreamException {
         for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("docDscr")) {
                    processDocDscr(xmlr, datasetDTO);
                }
                else if (xmlr.getLocalName().equals("stdyDscr")) {
                    processStdyDscr(xmlr, datasetDTO);
                }
                else if (xmlr.getLocalName().equals("fileDscr") && !importType.equals(ImportType.MIGRATION)) {
                    // EMK TODO: add this back in
                    // processFileDscr(xmlr, datasetDTO, filesMap);
                    
                }
                  else if (xmlr.getLocalName().equals("otherMat") && !importType.equals(ImportType.MIGRATION) ) {
                    // EMK TODO: add this back in
                    // processOtherMat(xmlr, studyVersion);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("codeBook")) return;
            }
        }
    }
       
  private void processDocDscr(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                
                   if (xmlr.getLocalName().equals("IDNo") && StringUtil.isEmpty(datasetDTO.getIdentifier()) ) {
                    // this will set a StudyId if it has not yet been set; it will get overridden by a metadata
                    // id in the StudyDscr section, if one exists
                    if ( AGENCY_HANDLE.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdHandle( parseText(xmlr), datasetDTO );
                    } /* else if ( AGENCY_HANDLE.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdDOI( parseText(xmlr), metadata.getStudyVersion().getStudy() );
                    } */ 
                // EMK TODO: we need to save this somewhere when we add harvesting infrastructure 
                } /*else if ( xmlr.getLocalName().equals("holdings") && StringUtil.isEmpty(datasetDTO..getHarvestHoldings()) ) {
                    metadata.setHarvestHoldings( xmlr.getAttributeValue(null, "URI") );
                }*/
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("docDscr")) return;
            }
        }
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
        StringBuffer content = new StringBuffer();
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
    
    private void processStdyDscr(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException {
        
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("citation")) processCitation(xmlr, datasetDTO);
                else if (xmlr.getLocalName().equals("stdyInfo")) processStdyInfo(xmlr, datasetDTO.getDatasetVersion());
                else if (xmlr.getLocalName().equals("method")) processMethod(xmlr, datasetDTO.getDatasetVersion());
                
                else if (xmlr.getLocalName().equals("dataAccs")) processDataAccs(xmlr, datasetDTO.getDatasetVersion()); /*
                   // EMK TODO: add back in these sections
             else if (xmlr.getLocalName().equals("othrStdyMat")) processOthrStdyMat(xmlr, metadata);*/
                else if (xmlr.getLocalName().equals("notes")) processNotes(xmlr, datasetDTO.getDatasetVersion());
                
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("stdyDscr")) return;
            }
        }
    }
     private void processCitation(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException {
        DatasetVersionDTO dvDTO = datasetDTO.getDatasetVersion();
        MetadataBlockDTO citation=datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
      // EMK TODO:  remove this when we decided where to read subject
        citation.getFields().add(FieldDTO.createMultipleVocabFieldDTO("subject", Arrays.asList("Other")));
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("titlStmt")) processTitlStmt(xmlr, datasetDTO);
                else if (xmlr.getLocalName().equals("rspStmt")) processRspStmt(xmlr,citation);
                else if (xmlr.getLocalName().equals("prodStmt")) processProdStmt(xmlr,citation);
                else if (xmlr.getLocalName().equals("distStmt")) processDistStmt(xmlr,citation);
                else if (xmlr.getLocalName().equals("serStmt")) processSerStmt(xmlr,citation);
                else if (xmlr.getLocalName().equals("verStmt")) processVerStmt(xmlr,dvDTO);
                else if (xmlr.getLocalName().equals("notes")) {
                    String _note = parseNoteByType( xmlr, NOTE_TYPE_UNF );
                    if (_note != null) {
                        datasetDTO.getDatasetVersion().setUNF( parseUNF( _note ) );
                    } else {
                      
                       processNotes(xmlr,dvDTO);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation")) return;
            }
        }
    }
     
 
   /**
    * 
    * 
    * @param xmlr
    * @param citation
    * @throws XMLStreamException 
    */  
   private void processStdyInfo(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
       List<HashSet<FieldDTO>> descriptions = new ArrayList<>();
      
       for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("subject")) {
                             processSubject(xmlr, getCitation(dvDTO));
                } else if (xmlr.getLocalName().equals("abstract")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"dsDescriptionDate", xmlr.getAttributeValue(null, "date"));
                    addToSet(set,"dsDescriptionValue",  parseText(xmlr, "abstract"));
                    descriptions.add(set);
                    
                } else if (xmlr.getLocalName().equals("sumDscr")) processSumDscr(xmlr, dvDTO);
            
                 else if (xmlr.getLocalName().equals("notes")) processNotes(xmlr,dvDTO);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("stdyInfo")) {
                    getCitation(dvDTO).getFields().add(FieldDTO.createMultipleCompoundFieldDTO("dsDescription", descriptions));
                    return;
                }
            }
        }
    } 
    private void processSubject(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> keywords = new ArrayList<>();
        List<HashSet<FieldDTO>> topicClasses = new ArrayList<>();
          for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
              
                if (xmlr.getLocalName().equals("keyword")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"keywordVocabulary", xmlr.getAttributeValue(null, "vocab"));     
                    addToSet(set, "keywordVocabularyURI", xmlr.getAttributeValue(null, "vocabURI") );
                    addToSet(set,"keywordValue", parseText(xmlr));
                    keywords.add(set);
                } else if (xmlr.getLocalName().equals("topcClas")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"topicClassVocab", xmlr.getAttributeValue(null, "vocab"));         
                    addToSet(set,"topicClassVocabURI", xmlr.getAttributeValue(null, "vocabURI") );
                    addToSet(set,"topicClassValue",parseText(xmlr));
                    topicClasses.add(set);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("subject")) {
                    citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("keyword", keywords));
                    citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("topicClassification", topicClasses));
                    return;
                }
            } else {
              //      citation.getFields().add(FieldDTO.createPrimitiveFieldDTO( "subject",xmlr.getElementText()));
               
            }
        }
    }
    
    private void processNotes (XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        String note = " Subject: "+xmlr.getAttributeValue(null, "subject")+" "
        + " Type: "+xmlr.getAttributeValue(null, "type")+" "
        + " Notes: "+parseText(xmlr, "notes")+";";
        addNote(note, dvDTO);
       
    } 
    
    private void addNote(String noteText, DatasetVersionDTO dvDTO ) {
        MetadataBlockDTO citation = getCitation(dvDTO);
        FieldDTO field = citation.getField("notesText");
        if (field==null) {
            field = FieldDTO.createPrimitiveFieldDTO("notesText", "");
            citation.getFields().add(field);
        }
        String noteValue = field.getSinglePrimitive();
        noteValue+= noteText;
        field.setSinglePrimitive(noteValue);
    }
  
    private void processSumDscr(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        List<String> geoUnit = new ArrayList<>();
        List<String> unitOfAnalysis = new ArrayList<>();
        List<String> universe = new ArrayList<>();
        List<String> kindOfData = new ArrayList<>();
        List<HashSet<FieldDTO>> geoBoundBox = new ArrayList<>();
        List<HashSet<FieldDTO>> geoCoverages = new ArrayList<>();
        FieldDTO timePeriodStart = null;
        FieldDTO timePeriodEnd = null;
        FieldDTO dateOfCollectionStart = null;
        FieldDTO dateOfCollectionEnd = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("timePrd")) {
                    
                    String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr) || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        timePeriodStart = FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredStart", parseDate(xmlr, "timePrd"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        timePeriodEnd = FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredEnd", parseDate(xmlr, "timePrd"));
                    }                   
                } else if (xmlr.getLocalName().equals("collDate")) {
                    String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr) || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        dateOfCollectionStart = FieldDTO.createPrimitiveFieldDTO("dateOfCollectionStart", parseDate(xmlr, "collDate"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        dateOfCollectionEnd = FieldDTO.createPrimitiveFieldDTO("dateOfCollectionEnd", parseDate(xmlr, "collDate"));
                    }
                   
                } else if (xmlr.getLocalName().equals("nation")) {
                     HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createVocabFieldDTO("country", parseText(xmlr)));
                    geoCoverages.add(set);
                } else if (xmlr.getLocalName().equals("geogCover")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("otherGeographicCoverage", parseText(xmlr)));
                    geoCoverages.add(set);
                } else if (xmlr.getLocalName().equals("geogUnit")) {
                    geoUnit.add(parseText(xmlr));
                } else if (xmlr.getLocalName().equals("geoBndBox")) {
                    geoBoundBox.add(processGeoBndBox(xmlr));
                } else if (xmlr.getLocalName().equals("anlyUnit")) {
                    unitOfAnalysis.add(parseText(xmlr, "anlyUnit"));
                } else if (xmlr.getLocalName().equals("universe")) {
                    universe.add(parseText(xmlr, "universe"));
                } else if (xmlr.getLocalName().equals("dataKind")) {
                    kindOfData.add(parseText(xmlr));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("sumDscr")) {
                    if (timePeriodStart!=null || timePeriodEnd!=null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("timePeriodCovered", timePeriodStart, timePeriodEnd));
                    }
                    if (dateOfCollectionStart!=null || dateOfCollectionEnd!=null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("dateOfCollection", dateOfCollectionStart, dateOfCollectionEnd));
                    }
                  
                    if (geoUnit.size() > 0) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("geographicUnit", geoUnit));
                    }
                    if (unitOfAnalysis.size() > 0) {
                        getSocialScience(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("unitOfAnalysis", unitOfAnalysis));
                    }
                    if (universe.size() > 0) {
                        getSocialScience(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("universe", universe));
                    }
                    if (kindOfData.size() > 0) {
                        getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("kindOfData", kindOfData));
                    }
                    if (geoCoverages.size()>0) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("geographicCoverage", geoCoverages));
                    }
                    if (geoBoundBox.size()>0) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("geographicBoundingBox", geoBoundBox));
                    }
                    return ;
                }
            }
        }
    }
    
    
    
 private HashSet<FieldDTO> processGeoBndBox(XMLStreamReader xmlr) throws XMLStreamException {
       HashSet<FieldDTO> set = new HashSet<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("westBL")) {
                    addToSet(set,"westLongitude", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("eastBL")) {
                     addToSet(set,"eastLongitude", parseText(xmlr));
               } else if (xmlr.getLocalName().equals("southBL")) {
                     addToSet(set,"southLongitude", parseText(xmlr));
               } else if (xmlr.getLocalName().equals("northBL")) {
                      addToSet(set,"northLongitude", parseText(xmlr));
              }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("geoBndBox")) break; 
            }
        }
        return set;
    }
   private void processMethod(XMLStreamReader xmlr, DatasetVersionDTO dvDTO ) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("dataColl")) {
                    processDataColl(xmlr, dvDTO);
                } else if (xmlr.getLocalName().equals("notes")) {
                    // As of 3.0, this note is going to be used for the extended
                    // metadata fields. For now, we are not going to try to 
                    // process these in any meaningful way.
                    // We also don't want them to become the "Study-Level Error
                    // notes" -- that's what the note was being used for 
                    // exclusively in pre-3.0 practice. 
                    // TODO: this needs to be revisited after 3.0, when we 
                    // figure out how DVNs will be harvesting each others'
                    // extended metadata.
                    
                    // TODO: clarify mapping. The spreadsheet says that datasetLevelErrorNotes should
                    // be mapped from anlyInfo.  datasetLevelErrorNotes isn't currently defined in tsv
                    // file, so I don't know if it's intended to allowMultiples.
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (!NOTE_TYPE_EXTENDED_METADATA.equalsIgnoreCase(noteType) ) {
                        addNote("Subject: Study Level Error Note, Notes: "+ parseText( xmlr,"notes" ) +";", dvDTO);
                       
                        
                    }
                } else if (xmlr.getLocalName().equals("anlyInfo")) {
                    processAnlyInfo(xmlr, getSocialScience(dvDTO));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("method")) return;
            }
        }
    }
   
    private void processSources(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // citation dataSources
                if (xmlr.getLocalName().equals("dataSrc")) {
                    citation.addField(FieldDTO.createMultiplePrimitiveFieldDTO("dataSources", Arrays.asList(parseText( xmlr, "dataSrc" ))));
                    // citation originOfSources
                } else if (xmlr.getLocalName().equals("srcOrig")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("originOfSources", parseText( xmlr, "srcOrig" )));
                     // citation characteristicOfSources
                } else if (xmlr.getLocalName().equals("srcChar")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("characteristicOfSources", parseText( xmlr, "srcChar" )));
                     // citation accessToSources
                } else if (xmlr.getLocalName().equals("srcDocu")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("accessToSources", parseText( xmlr, "srcDocu" )));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("sources")) return;
            }
        }
    }
   private void processAnlyInfo(XMLStreamReader xmlr, MetadataBlockDTO socialScience) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // socialscience responseRate
                if (xmlr.getLocalName().equals("respRate")) {
                    socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("responseRate", parseText( xmlr, "respRate" )));
                // socialscience samplingErrorEstimates    
                } else if (xmlr.getLocalName().equals("EstSmpErr")) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("samplingErrorEstimates", parseText( xmlr, "EstSmpErr" )));
                // socialscience otherDataAppraisal    
                } else if (xmlr.getLocalName().equals("dataAppr")) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("otherDataAppraisal", parseText( xmlr, "dataAppr" )));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("anlyInfo")) return;
            }
        }
    }

    private void processDataColl(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        MetadataBlockDTO socialScience =getSocialScience(dvDTO);
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                //timeMethod
                if (xmlr.getLocalName().equals("timeMeth")) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("timeMethod", parseText( xmlr, "timeMeth" )));
               } else if (xmlr.getLocalName().equals("dataCollector")) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("dataCollector", parseText( xmlr, "dataCollector" )));
                // frequencyOfDataCollection    
                } else if (xmlr.getLocalName().equals("frequenc")) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("frequencyOfDataCollection", parseText( xmlr, "frequenc" )));
                //samplingProcedure
                } else if (xmlr.getLocalName().equals("sampProc")) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("samplingProcedure", parseText( xmlr, "sampProc" )));
                //targetSampleSize
                } else if (xmlr.getLocalName().equals("targetSampleSize")) {
                  processTargetSampleSize(xmlr, socialScience);
                    //devationsFromSamplingDesign
                } else if (xmlr.getLocalName().equals("deviat")) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("deviationsFromSampleDesign", parseText( xmlr, "deviat" )));
                 // collectionMode
                } else if (xmlr.getLocalName().equals("collMode")) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("collectionMode", parseText( xmlr, "collMode" )));                      
                //researchInstrument
                } else if (xmlr.getLocalName().equals("resInstru")) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("researchInstrument", parseText( xmlr, "resInstru" )));
                } else if (xmlr.getLocalName().equals("sources")) {
                    processSources(xmlr,getCitation(dvDTO));
                } else if (xmlr.getLocalName().equals("collSitu")) {
                     socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("dataCollectionSituation", parseText( xmlr, "collSitu" )));
                } else if (xmlr.getLocalName().equals("actMin")) {
                      socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("actionsToMinimizeLoss", parseText( xmlr, "actMin" )));
                } else if (xmlr.getLocalName().equals("ConOps")) {
                       socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("controlOperations", parseText( xmlr, "ConOps" )));
                } else if (xmlr.getLocalName().equals("weight")) {
                        socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("weighting", parseText( xmlr, "weight" )));
                } else if (xmlr.getLocalName().equals("cleanOps")) {
                       socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("cleaningOperations", parseText( xmlr, "cleanOps" )));
                 }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("dataColl")) return;
            }
        }
    }

    private void processTargetSampleSize(XMLStreamReader xmlr, MetadataBlockDTO socialScience) throws XMLStreamException {
        FieldDTO sampleSize=null;
        FieldDTO sampleSizeFormula=null;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("sampleSize")) {
                    sampleSize = FieldDTO.createPrimitiveFieldDTO("targetSampleActualSize",  parseText( xmlr, "sampleSize" ));
                } else if (xmlr.getLocalName().equals("sampleSizeFormula")) {
                    sampleSizeFormula = FieldDTO.createPrimitiveFieldDTO("targetSampleSizeFormula", parseText( xmlr, "sampleSizeFormula" ));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("targetSampleSize")) {
                    if (sampleSize!=null || sampleSizeFormula!=null) {
                        socialScience.getFields().add(FieldDTO.createCompoundFieldDTO("targetSampleSize", sampleSize,sampleSizeFormula));
                    }
                    return;
                }
            }
        }

    }
    /*
    EMK TODO:  In DVN 3.6, users were allowed to enter their own version date, and in addition the app assigned a version date when
     the version is released.  So DDI's that we have to migrate, we can see this:
    <verStmt>
		<version date="2004-04-04">1</version>
	</verStmt>
	<verStmt source="DVN">
		<version date="2014-05-21" type="RELEASED">1</version>
	</verStmt>
    Question:  what to do with these two different dates?  Need to review with Eleni
    Note: we should use the verStmt with source="DVN" as the 'official' version statement
    DDI's that we are migrating should have one and only one DVN version statement
    */
    private void processVerStmt(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.HARVEST)) {
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("verStmt")) {
                        String source = xmlr.getAttributeValue(null,"source");
                        System.out.println("found source:"+ source);
                    } 
                    if (xmlr.getLocalName().equals("version")) {
                        dvDTO.setReleaseDate(xmlr.getAttributeValue(null, "date"));
                        String versionState =xmlr.getAttributeValue(null,"type");
                        if (versionState!=null ) {
                            if( versionState.equals("ARCHIVED")) {
                                versionState="RELEASED";
                            }
                            dvDTO.setVersionState(Enum.valueOf(VersionState.class, versionState));  
                        }                     
                        parseVersionNumber(dvDTO, parseText(xmlr));
                      
                         } else if (xmlr.getLocalName().equals("notes")) { processNotes(xmlr, dvDTO); 
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (xmlr.getLocalName().equals("verStmt")) {
                        return;
                    }
                }

            }
        }
        if (importType.equals(ImportType.NEW)) {
            // If this is a new, Draft version, versionNumber and minor versionNumber are null.
            dvDTO.setVersionState(VersionState.DRAFT);
        } else if (importType.equals(ImportType.HARVEST)) {
            dvDTO.setVersionState(VersionState.RELEASED);
        } 
    }
    
      private void processDataAccs(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("setAvail")) processSetAvail(xmlr,dvDTO);
                else if (xmlr.getLocalName().equals("useStmt")) processUseStmt(xmlr,dvDTO);
                else if (xmlr.getLocalName().equals("notes")) {
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_TERMS_OF_USE.equalsIgnoreCase(noteType) ) {
                       // Ignore Harvest Terms of Use, not relevant in Dataverse 4.0
                       // because we are not presenting harvested data for download
                    } else {
                        processNotes( xmlr, dvDTO );
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("dataAccs")) return;
            }
        }
    }
    
    private void processSetAvail(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("accsPlac")) {
                    dvDTO.setDataAccessPlace( parseText( xmlr, "accsPlac" ) );
                } else if (xmlr.getLocalName().equals("origArch")) {
                    dvDTO.setOriginalArchive( parseText( xmlr, "origArch" ) );
                } else if (xmlr.getLocalName().equals("avlStatus")) {
                    dvDTO.setAvailabilityStatus( parseText( xmlr, "avlStatus" ) );
                } else if (xmlr.getLocalName().equals("collSize")) {                
                    dvDTO.setSizeOfCollection(parseText( xmlr, "collSize" ) );
                } else if (xmlr.getLocalName().equals("complete")) {
                    dvDTO.setStudyCompletion( parseText( xmlr, "complete" ) );
                } else if (xmlr.getLocalName().equals("notes")) {
                    processNotes( xmlr, dvDTO );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("setAvail")) return;
            }
        }
    }

    private void processUseStmt(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("confDec")) {
                    dvDTO.setConfidentialityDeclaration( parseText( xmlr, "confDec" ) );
                } else if (xmlr.getLocalName().equals("specPerm")) {
                    dvDTO.setSpecialPermissions( parseText( xmlr, "specPerm" ) );
                } else if (xmlr.getLocalName().equals("restrctn")) {
                    dvDTO.setRestrictions( parseText( xmlr, "restrctn" ) );
                } else if (xmlr.getLocalName().equals("contact")) {
                    dvDTO.setContactForAccess( parseText( xmlr, "contact" ) );
                } else if (xmlr.getLocalName().equals("citReq")) {
                    dvDTO.setCitationRequirements( parseText( xmlr, "citReq" ) );
                } else if (xmlr.getLocalName().equals("deposReq")) {
                    dvDTO.setDepositorRequirements( parseText( xmlr, "deposReq" ) );
                } else if (xmlr.getLocalName().equals("conditions")) {
                    dvDTO.setConditions( parseText( xmlr, "conditions" ) );
                } else if (xmlr.getLocalName().equals("disclaimer")) {
                    dvDTO.setDisclaimer( parseText( xmlr, "disclaimer" ) );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("useStmt")) return;
            }
        }
    }
   /**
    * Separate the versionNumber into two parts - before the first '.' 
    * is the versionNumber, and after is the minorVersionNumber.
    * If no minorVersionNumber exists, set to "0".
    * @param dvDTO
    * @param versionNumber 
    */
    private void parseVersionNumber(DatasetVersionDTO dvDTO, String versionNumber) {
        int firstIndex = versionNumber.indexOf('.');
        if (firstIndex == -1) {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber));
            dvDTO.setMinorVersionNumber("0");
        } else {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber.substring(0, firstIndex - 1)));
            dvDTO.setMinorVersionNumber(versionNumber.substring(firstIndex + 1));
        }
       

    }
   
   private void processSerStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
          FieldDTO seriesName=null;
          FieldDTO seriesInformation=null;
          for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("serName")) {
                   seriesName = FieldDTO.createPrimitiveFieldDTO("seriesName", parseText(xmlr));
                  
                } else if (xmlr.getLocalName().equals("serInfo")) {
                    seriesInformation=FieldDTO.createPrimitiveFieldDTO("seriesInformation", parseText(xmlr) );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("serStmt")) {
                    citation.getFields().add(FieldDTO.createCompoundFieldDTO("series",seriesName,seriesInformation ));
                    return;
                }
            }
        }
    }

    private void processDistStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> distributors = new ArrayList<>();
        List<HashSet<FieldDTO>> datasetContacts = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("distrbtr")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "distributorAbbreviation", xmlr.getAttributeValue(null, "abbr"));
                    addToSet(set, "distributorAffiliation", xmlr.getAttributeValue(null, "affiliation"));

                    Map<String, String> distDetails = parseCompoundText(xmlr, "distrbtr");
                    addToSet(set, "distributorName", distDetails.get("name"));
                    addToSet(set, "distributorURL", distDetails.get("url"));
                    addToSet(set, "distributorLogoURL", distDetails.get("logo"));
                    distributors.add(set);

                } else if (xmlr.getLocalName().equals("contact")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "datasetContactEmail", xmlr.getAttributeValue(null, "email"));
                    addToSet(set, "datasetContactAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    addToSet(set, "datasetContactName", parseText(xmlr));
                    datasetContacts.add(set);

                } else if (xmlr.getLocalName().equals("depositr")) {
                    Map<String, String> depDetails = parseCompoundText(xmlr, "depositr");
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("depositor", depDetails.get("name")));
                } else if (xmlr.getLocalName().equals("depDate")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("dateOfDeposit", parseDate(xmlr, "depDate")));

                } else if (xmlr.getLocalName().equals("distDate")) {
                    // TODO: for New Import, we will ignore this, waiting for decision
                    // about what to do for Harvest & migration (12/15/2014)
                    // metadata.setDistributionDate( parseDate(xmlr,"distDate") );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("distStmt")) {
                    if (distributors.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("distributor", distributors));
                    }
                    if (datasetContacts.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("datasetContact", datasetContacts));
                    }
                    return;
                }
            }
        }
    }
    private void processProdStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> producers = new ArrayList<>();
        List<HashSet<FieldDTO>> grants = new ArrayList<>();
        List<HashSet<FieldDTO>> software = new ArrayList<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("producer")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("producerAbbreviation", xmlr.getAttributeValue(null, "abbr")));
                    set.add(FieldDTO.createPrimitiveFieldDTO("producerAffiliation", xmlr.getAttributeValue(null, "affiliation")));
                    
                    Map<String, String> prodDetails = parseCompoundText(xmlr, "producer");
                    set.add(FieldDTO.createPrimitiveFieldDTO("producerName", prodDetails.get("name")));
                    set.add(FieldDTO.createPrimitiveFieldDTO("producerURL", prodDetails.get("url") ));
                    set.add(FieldDTO.createPrimitiveFieldDTO("producerLogoURL", prodDetails.get("logo")));
                    producers.add(set);
                } else if (xmlr.getLocalName().equals("prodDate")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("productionDate", parseDate(xmlr, "prodDate")));
                } else if (xmlr.getLocalName().equals("prodPlac")) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("productionPlace", parseDate(xmlr, "prodPlac")));
                } else if (xmlr.getLocalName().equals("software")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("softwareVersion", xmlr.getAttributeValue(null, "version")));
                    set.add(FieldDTO.createPrimitiveFieldDTO("softwareName", xmlr.getAttributeValue(null, "version")));
                    software.add(set);

                    //TODO: ask Gustavo "fundAg"?TO
                } else if (xmlr.getLocalName().equals("fundAg")) {
                    //    metadata.setFundingAgency( parseText(xmlr) );
                } else if (xmlr.getLocalName().equals("grantNo")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("grantNumberAgency", xmlr.getAttributeValue(null, "agency")));
                    set.add(FieldDTO.createPrimitiveFieldDTO("grantNumberValue", parseText(xmlr)));
                    grants.add(set);

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("prodStmt")) {
                    if (software.size()>0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("software", software));
                    }
                    if (grants.size()>0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("grantNumber", grants));
                    } 
                    if (producers.size()>0) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("producer", producers));
                    }
                    return;
                }
            }
        }
    }
    
   private void processTitlStmt(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException {
       MetadataBlockDTO citation = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
       List<HashSet<FieldDTO>> otherIds = new ArrayList<>();
       
       for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("titl")) {
                    FieldDTO field = FieldDTO.createPrimitiveFieldDTO("title", parseText(xmlr));
                    citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("subTitl")) {
                  FieldDTO field = FieldDTO.createPrimitiveFieldDTO("subtitle", parseText(xmlr));
                   citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("altTitl")) {
                  FieldDTO field = FieldDTO.createPrimitiveFieldDTO("alternativeTitle", parseText(xmlr));
                   citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("IDNo")) {
                    if ( AGENCY_HANDLE.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdHandle( parseText(xmlr), datasetDTO );
                    } else if ( AGENCY_DOI.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdDOI( parseText(xmlr), datasetDTO );
                    } else {
                        HashSet<FieldDTO> set = new HashSet<>();
                        FieldDTO agencyValue = FieldDTO.createPrimitiveFieldDTO("otherIdAgency", xmlr.getAttributeValue(null, "agency"));
                        FieldDTO otherIdValue = FieldDTO.createPrimitiveFieldDTO("otherIdValue", parseText(xmlr));
                        set.add(agencyValue);
                        set.add(otherIdValue);
                        otherIds.add(set);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("titlStmt")) {
                    if (otherIds.size()>0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("otherId", otherIds));
                    }
                    return;
                }
            }
        }
    }
   private void processRspStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
     
       List<HashSet<FieldDTO>> authors = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("AuthEnty")) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("authorAffiliation", xmlr.getAttributeValue(null, "affiliation")));
                    set.add(FieldDTO.createPrimitiveFieldDTO("authorName", parseText(xmlr)));
                    authors.add(set);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("rspStmt")) {
                    FieldDTO author = FieldDTO.createMultipleCompoundFieldDTO("author", authors);
                    citation.getFields().add(author);
                    return;
                }
            }
        }
    }
   private Map<String,String> parseCompoundText (XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        Map<String,String> returnMap = new HashMap<String,String>();
        String text = "";

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                if (text != "") { text += "\n";}
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
        Map returnMap = null;

        while (true) {
            if (!returnString.equals("")) { returnString += "\n";}
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
        if (unfString.indexOf("UNF:") != -1) {
            return unfString.substring( unfString.indexOf("UNF:") );
        } else {
            return null;
        }
    }
  
    private Map parseDVNCitation(XMLStreamReader xmlr) throws XMLStreamException {
        Map returnValues = new HashMap();
        
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
                        returnValues.put("replicationData", new Boolean(true));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation")) break;
            }
        } 
        
        return returnValues;
    }    
     
   private void parseStudyIdHandle(String _id, DatasetDTO datasetDTO) {

        int index1 = _id.indexOf(':');
        int index2 = _id.indexOf('/');
        if (index1==-1) {
            throw new EJBException("Error parsing (Handle) IdNo: "+_id+". ':' not found in string");
        } else {
            datasetDTO.setProtocol(_id.substring(0,index1));
        }
        if (index2 == -1) {
            throw new EJBException("Error parsing (Handle) IdNo: "+_id+". '/' not found in string");

        } else {
            datasetDTO.setAuthority(_id.substring(index1+1, index2));
        }
        datasetDTO.setDoiSeparator("/");
        datasetDTO.setProtocol("hdl");
        datasetDTO.setIdentifier(_id.substring(index2+1));
    }

    private void parseStudyIdDOI(String _id, DatasetDTO datasetDTO) {

        // TODO: 
        // This method needs to be modified to reflect the specifics of DOI
        // string conventions; 
        // (in particular, there may be an extra character sequence embedded 
        // between the authority and the id - ?? ("fk2" - ?)
        // -- L.A. - v3.6. 
        
        int index1 = _id.indexOf(':');
        // Note the "lastIndexOf()" below - which is different to what we are 
        // doing for handles (above); the idea is that DOIs may have "shoulders" - 
        // sub-namespaces - but we are going to be treating all these combined
        // levels of namespaces as the single "Authority". 
        // TODO: still needs to be confirmed; -- L.A., v3.6 (still in dev.)
        int index2 = _id.lastIndexOf('/');
        if (index1==-1) {
            throw new EJBException("Error parsing (DOI) IdNo: "+_id+". ':' not found in string");
        } else {
            datasetDTO.setProtocol(_id.substring(0,index1));
        }
        if (index2 == -1) {
            throw new EJBException("Error parsing (DOI) IdNo: "+_id+". '/' not found in string");

        } else {
            datasetDTO.setAuthority(_id.substring(index1+1, index2));
        }
        datasetDTO.setProtocol("doi");
        datasetDTO.setDoiSeparator("/");
       
        datasetDTO.setIdentifier(_id.substring(index2+1));
    }
    // Helper methods
    private MetadataBlockDTO getCitation(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("citation");
    }
    
    private MetadataBlockDTO getGeospatial(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("geospatial");
    }
    
  private MetadataBlockDTO getSocialScience(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("socialscience");
    }
      
    
    private void addToSet(HashSet<FieldDTO> set, String typeName, String value ) {
        if (value!=null) {
            set.add(FieldDTO.createPrimitiveFieldDTO(typeName, value));
        }
    }
    
    
/* 
    TODO: add this back in when needed? 
    private void processFileDscr(XMLStreamReader xmlr, DatasetVersionDTO dvDTO, Map filesMap) throws XMLStreamException {
        FileMetadataDTO fmd = new FileMetadataDTO();
        
        studyVersion.getFileMetadatas().add(fmd);

        //StudyFile sf = new OtherFile(studyVersion.getStudy()); // until we connect the sf and dt, we have to assume it's an other file
        // as an experiment, I'm going to do it the other way around:
        // assume that every fileDscr is a subsettable file now, and convert them
        // to otherFiles later if no variables are referencing it -- L.A.


        TabularDataFile sf = new TabularDataFile(studyVersion.getStudy()); 
        DataTable dt = new DataTable();
        dt.setStudyFile(sf);
        sf.setDataTable(dt);

        fmd.setStudyFile(sf);

        sf.setFileSystemLocation( xmlr.getAttributeValue(null, "URI"));
        String ddiFileId = xmlr.getAttributeValue(null, "ID");

        /// the following Strings are used to determine the category

        String catName = null;
        String icpsrDesc = null;
        String icpsrId = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("fileTxt")) {
                    String tempDDIFileId = processFileTxt(xmlr, fmd, dt);
                    ddiFileId = ddiFileId != null ? ddiFileId : tempDDIFileId;
                }
                else if (xmlr.getLocalName().equals("notes")) {
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_UNF.equalsIgnoreCase(noteType) ) {
                        String unf = parseUNF( parseText(xmlr) );
                        sf.setUnf(unf);
                        dt.setUnf(unf);
                    } else if ("vdc:category".equalsIgnoreCase(noteType) ) {
                        catName = parseText(xmlr);
                    } else if ("icpsr:category".equalsIgnoreCase(noteType) ) {
                        String subjectType = xmlr.getAttributeValue(null, "subject");
                        if ("description".equalsIgnoreCase(subjectType)) {
                            icpsrDesc = parseText(xmlr);
                        } else if ("id".equalsIgnoreCase(subjectType)) {
                            icpsrId = parseText(xmlr);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {// </codeBook>
                if (xmlr.getLocalName().equals("fileDscr")) {
                    // post process
                    if (fmd.getLabel() == null || fmd.getLabel().trim().equals("") ) {
                        fmd.setLabel("file");
                    }

                    fmd.setCategory(determineFileCategory(catName, icpsrDesc, icpsrId));


                    if (ddiFileId != null) {
                        List filesMapEntry = new ArrayList();
                        filesMapEntry.add(fmd);
                        filesMapEntry.add(dt);
                        filesMap.put( ddiFileId, filesMapEntry);
                    }

                    return;
                }
            }
        }
    }
    */
}

