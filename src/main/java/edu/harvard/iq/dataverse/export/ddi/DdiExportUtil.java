package edu.harvard.iq.dataverse.export.ddi;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.datavariable.VariableRange;
import edu.harvard.iq.dataverse.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.CategoryMetadata;

import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.LEVEL_FILE;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_UNF;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_UNF;
import edu.harvard.iq.dataverse.export.DDIExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import static edu.harvard.iq.dataverse.util.SystemConfig.FQDN;
import static edu.harvard.iq.dataverse.util.SystemConfig.SITE_URL;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.DOMException;

// For write operation
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());
    public static final String NOTE_TYPE_TERMS_OF_USE = "DVN:TOU";
    public static final String NOTE_TYPE_TERMS_OF_ACCESS = "DVN:TOA";
    public static final String NOTE_TYPE_DATA_ACCESS_PLACE = "DVN:DAP";


    public static final String LEVEL_DV = "dv";

    
    static SettingsServiceBean settingsService;
    
    public static final String NOTE_TYPE_CONTENTTYPE = "DATAVERSE:CONTENTTYPE";
    public static final String NOTE_SUBJECT_CONTENTTYPE = "Content/MIME Type";

    public static String datasetDtoAsJson2ddi(String datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson, DatasetDTO.class);
        try {
            return dto2ddi(datasetDto);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    // "short" ddi, without the "<fileDscr>"  and "<dataDscr>/<var>" sections:
    public static void datasetJson2ddi(JsonObject datasetDtoAsJson, OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        dtoddi(datasetDto, outputStream);
    }
    
    private static String dto2ddi(DatasetDTO datasetDto) throws XMLStreamException {
        OutputStream outputStream = new ByteArrayOutputStream();
        dtoddi(datasetDto, outputStream);
        String xml = outputStream.toString();
        return XmlPrinter.prettyPrintXml(xml);
    }
    
    private static void dtoddi(DatasetDTO datasetDto, OutputStream outputStream) throws XMLStreamException {
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("ddi:codebook:2_5");
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xsi:schemaLocation", DDIExporter.DEFAULT_XML_NAMESPACE + " " + DDIExporter.DEFAULT_XML_SCHEMALOCATION);
        writeAttribute(xmlw, "version", DDIExporter.DEFAULT_XML_VERSION);
        createStdyDscr(xmlw, datasetDto);
        createOtherMats(xmlw, datasetDto.getDatasetVersion().getFiles());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
    }

    
    // "full" ddi, with the the "<fileDscr>"  and "<dataDscr>/<var>" sections: 
    public static void datasetJson2ddi(JsonObject datasetDtoAsJson, DatasetVersion version, OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("ddi:codebook:2_5");
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xsi:schemaLocation", DDIExporter.DEFAULT_XML_NAMESPACE + " " + DDIExporter.DEFAULT_XML_SCHEMALOCATION);
        writeAttribute(xmlw, "version", DDIExporter.DEFAULT_XML_VERSION);
        createStdyDscr(xmlw, datasetDto);
        createFileDscr(xmlw, version);
        createDataDscr(xmlw, version);
        createOtherMatsFromFileMetadatas(xmlw, version.getFileMetadatas());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
    }
    
    
    /**
     * @todo This is just a stub, copied from DDIExportServiceBean. It should
     * produce valid DDI based on
     * http://guides.dataverse.org/en/latest/developers/tools.html#msv but it is
     * incomplete and will be worked on as part of
     * https://github.com/IQSS/dataverse/issues/2579 . We'll want to reference
     * the DVN 3.x code for creating a complete DDI.
     *
     * @todo Rename this from "study" to "dataset".
     */
    private static void createStdyDscr(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentProtocol = datasetDto.getProtocol();
        String persistentAgency = persistentProtocol;
        // The "persistentAgency" tag is used for the "agency" attribute of the 
        // <IDNo> ddi section; back in the DVN3 days we used "handle" and "DOI" 
        // for the 2 supported protocols, respectively. For the sake of backward
        // compatibility, we should probably stick with these labels: (-- L.A. 4.5)
        if ("hdl".equals(persistentAgency)) { 
            persistentAgency = "handle";
        } else if ("doi".equals(persistentAgency)) {
            persistentAgency = "DOI";
        }
        
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();       
        //docDesc Block
        writeDocDescElement (xmlw, datasetDto);
        //stdyDesc Block
        xmlw.writeStartElement("stdyDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
       
        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        writeFullElement(xmlw, "subTitl", dto2Primitive(version, DatasetFieldConstant.subTitle));
        writeFullElement(xmlw, "altTitl", dto2Primitive(version, DatasetFieldConstant.alternativeTitle));
        
        xmlw.writeStartElement("IDNo");
        writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(persistentProtocol + ":" + persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo
        writeOtherIdElement(xmlw, version);
        xmlw.writeEndElement(); // titlStmt

        writeAuthorsElement(xmlw, version);
        writeProducersElement(xmlw, version);
        
        xmlw.writeStartElement("distStmt");
      //The default is to add Dataverse Repository as a distributor. The excludeinstallationifset setting turns that off if there is a distributor defined in the metadata
        boolean distributorSet=false;
        MetadataBlockDTO citationDTO= version.getMetadataBlocks().get("citation");
        if(citationDTO!=null) {
            if(citationDTO.getField(DatasetFieldConstant.distributor)!=null) {
                distributorSet=true;
            }
        }
        logger.info("Dsitr set?: " + distributorSet);
        logger.info("Pub?: " + datasetDto.getPublisher());
        boolean excludeRepository = settingsService.isTrueForKey(SettingsServiceBean.Key.ExportInstallationAsDistributorOnlyWhenNotSet, false);
        logger.info("Exclude: " + excludeRepository);
        if (!StringUtils.isEmpty(datasetDto.getPublisher()) && !(excludeRepository && distributorSet)) {
            xmlw.writeStartElement("distrbtr");
            writeAttribute(xmlw, "source", "archive");
            xmlw.writeCharacters(datasetDto.getPublisher());
            xmlw.writeEndElement(); //distrbtr
        }
        writeDistributorsElement(xmlw, version);
        writeContactsElement(xmlw, version);
        writeFullElement(xmlw, "distDate", dto2Primitive(version, DatasetFieldConstant.distributionDate));
        writeFullElement(xmlw, "depositr", dto2Primitive(version, DatasetFieldConstant.depositor));
        writeFullElement(xmlw, "depDate", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));

        xmlw.writeEndElement(); // diststmt

        writeSeriesElement(xmlw, version);

        xmlw.writeEndElement(); // citation
        //End Citation Block
        
        //Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");
        
        writeSubjectElement(xmlw, version); //Subject and Keywords
        writeAbstractElement(xmlw, version); // Description
        writeSummaryDescriptionElement(xmlw, version);
        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.notesText));
        ////////
        xmlw.writeEndElement(); // stdyInfo

        writeMethodElement(xmlw, version);
        writeDataAccess(xmlw , version);
        writeOtherStudyMaterial(xmlw , version);

        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        
        xmlw.writeEndElement(); // stdyDscr

    }

    private static void writeOtherStudyMaterial(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("othrStdyMat");
        writeFullElementList(xmlw, "relMat", dto2PrimitiveList(version, DatasetFieldConstant.relatedMaterial));
        writeFullElementList(xmlw, "relStdy", dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets));
        writeRelPublElement(xmlw, version);
        writeFullElementList(xmlw, "othRefs", dto2PrimitiveList(version, DatasetFieldConstant.otherReferences));
        xmlw.writeEndElement(); //othrStdyMat
    }

    private static void writeDataAccess(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("dataAccs");
        if (version.getTermsOfUse() != null && !version.getTermsOfUse().trim().equals("")) {
            xmlw.writeStartElement("notes");
            writeAttribute(xmlw, "type", NOTE_TYPE_TERMS_OF_USE);
            writeAttribute(xmlw, "level", LEVEL_DV);
            xmlw.writeCharacters(version.getTermsOfUse());
            xmlw.writeEndElement(); //notes
        }
        if (version.getTermsOfAccess() != null && !version.getTermsOfAccess().trim().equals("")) {
            xmlw.writeStartElement("notes");
            writeAttribute(xmlw, "type", NOTE_TYPE_TERMS_OF_ACCESS);
            writeAttribute(xmlw, "level", LEVEL_DV);
            xmlw.writeCharacters(version.getTermsOfAccess());
            xmlw.writeEndElement(); //notes
        }

        xmlw.writeStartElement("setAvail");
        writeFullElement(xmlw, "accsPlac", version.getDataAccessPlace());
        writeFullElement(xmlw, "origArch", version.getOriginalArchive());
        writeFullElement(xmlw, "avlStatus", version.getAvailabilityStatus());
        writeFullElement(xmlw, "collSize", version.getSizeOfCollection());
        writeFullElement(xmlw, "complete", version.getStudyCompletion());
        xmlw.writeEndElement(); //setAvail
        xmlw.writeStartElement("useStmt");
        writeFullElement(xmlw, "confDec", version.getConfidentialityDeclaration());
        writeFullElement(xmlw, "specPerm", version.getSpecialPermissions());
        writeFullElement(xmlw, "restrctn", version.getRestrictions());
        writeFullElement(xmlw, "contact", version.getContactForAccess());
        writeFullElement(xmlw, "citReq", version.getCitationRequirements());
        writeFullElement(xmlw, "deposReq", version.getDepositorRequirements());
        writeFullElement(xmlw, "conditions", version.getConditions());
        writeFullElement(xmlw, "disclaimer", version.getDisclaimer());
        xmlw.writeEndElement(); //useStmt
        xmlw.writeEndElement(); //dataAccs
    }
    
    private static void writeDocDescElement (XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentProtocol = datasetDto.getProtocol();
        String persistentAgency = persistentProtocol;
        // The "persistentAgency" tag is used for the "agency" attribute of the 
        // <IDNo> ddi section; back in the DVN3 days we used "handle" and "DOI" 
        // for the 2 supported protocols, respectively. For the sake of backward
        // compatibility, we should probably stick with these labels: (-- L.A. 4.5)
        if ("hdl".equals(persistentAgency)) { 
            persistentAgency = "handle";
        } else if ("doi".equals(persistentAgency)) {
            persistentAgency = "DOI";
        }
        
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        
        xmlw.writeStartElement("docDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        xmlw.writeStartElement("IDNo");
        writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(persistentProtocol + ":" + persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo
        xmlw.writeEndElement(); // titlStmt
        xmlw.writeStartElement("distStmt");
        //The doc is always published by the Dataverse Repository
        if (!StringUtils.isEmpty(datasetDto.getPublisher())) {
            xmlw.writeStartElement("distrbtr");
            writeAttribute(xmlw, "source", "archive");
            xmlw.writeCharacters(datasetDto.getPublisher());
            xmlw.writeEndElement(); // distrbtr
        }
        writeFullElement(xmlw, "distDate", datasetDto.getPublicationDate());
        
        xmlw.writeEndElement(); // diststmt
        writeVersionStatement(xmlw, version);
        xmlw.writeStartElement("biblCit");
        xmlw.writeCharacters(version.getCitation());
        xmlw.writeEndElement(); // biblCit
        xmlw.writeEndElement(); // citation      
        xmlw.writeEndElement(); // docDscr
        
    }
    
    private static void writeVersionStatement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException{
        xmlw.writeStartElement("verStmt");
        writeAttribute(xmlw,"source","archive"); 
        xmlw.writeStartElement("version");
        writeAttribute(xmlw,"date", datasetVersionDTO.getReleaseTime().substring(0, 10));
        writeAttribute(xmlw,"type", datasetVersionDTO.getVersionState().toString()); 
        xmlw.writeCharacters(datasetVersionDTO.getVersionNumber().toString());
        xmlw.writeEndElement(); // version
        xmlw.writeEndElement(); // verStmt
    }
    
    private static void writeSummaryDescriptionElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("sumDscr");
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                Integer per = 0;
                Integer coll = 0;
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            per++;
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.timePeriodCoveredStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.timePeriodCoveredEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                                writeDateElement(xmlw, "timePrd", "P"+ per.toString(), "start", dateValStart );
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeDateElement(xmlw, "timePrd",  "P"+ per.toString(), "end", dateValEnd );
                            }
                        }
                    }
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            coll++;
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.dateOfCollectionStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.dateOfCollectionEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                                writeDateElement(xmlw, "collDate",  "P"+ coll.toString(), "start", dateValStart );
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeDateElement(xmlw,  "collDate",  "P"+ coll.toString(), "end", dateValEnd );
                            }
                        }
                    }
                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "dataKind", fieldDTO);                     
                    }
                }
            }
            
            if("geospatial".equals(key)){                
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.geographicCoverage.equals(fieldDTO.getTypeName())) {

                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            HashMap<String, String> geoMap = new HashMap<>();
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.country.equals(next.getTypeName())) {
                                    geoMap.put("country", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.city.equals(next.getTypeName())) {
                                    geoMap.put("city", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.state.equals(next.getTypeName())) {
                                    geoMap.put("state", next.getSinglePrimitive());
                                } 
                                if (DatasetFieldConstant.otherGeographicCoverage.equals(next.getTypeName())) {
                                    geoMap.put("otherGeographicCoverage", next.getSinglePrimitive());
                                } 
                            }

                            if (geoMap.get("country") != null) {
                                writeFullElement(xmlw, "nation", geoMap.get("country"));
                            }
                            if (geoMap.get("city") != null) {
                                writeFullElement(xmlw, "geogCover", geoMap.get("city"));
                            }
                            if (geoMap.get("state") != null) {
                                writeFullElement(xmlw, "geogCover", geoMap.get("state"));
                            }
                            if (geoMap.get("otherGeographicCoverage") != null) {
                                writeFullElement(xmlw, "geogCover", geoMap.get("otherGeographicCoverage"));
                            }

                        }
                    }
                    if (DatasetFieldConstant.geographicBoundingBox.equals(fieldDTO.getTypeName())) {

                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            xmlw.writeStartElement("geoBndBox");
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.westLongitude.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "westBL", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.eastLongitude.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "eastBL", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.northLatitude.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "northBL", next.getSinglePrimitive());
                                }  
                                if (DatasetFieldConstant.southLatitude.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "southBL", next.getSinglePrimitive());
                                }                               

                            }
                            xmlw.writeEndElement();
                        }

                    }
                }
                    writeFullElementList(xmlw, "geogUnit", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.geographicUnit));
            }

            if("socialscience".equals(key)){                
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.universe.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "universe", fieldDTO);
                    }
                    if (DatasetFieldConstant.unitOfAnalysis.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "anlyUnit", fieldDTO);                     
                    }
                }              
            }
        }
        xmlw.writeEndElement(); //sumDscr     
    }
    
    private static void writeMultipleElement(XMLStreamWriter xmlw, String element, FieldDTO fieldDTO) throws XMLStreamException {
        for (String value : fieldDTO.getMultiplePrimitive()) {
            writeFullElement(xmlw, element, value);
        }
    }
    
    private static void writeDateElement(XMLStreamWriter xmlw, String element, String cycle, String event, String dateIn) throws XMLStreamException {

        xmlw.writeStartElement(element);
        writeAttribute(xmlw, "cycle",  cycle);
        writeAttribute(xmlw, "event", event);
        writeAttribute(xmlw, "date", dateIn);
        xmlw.writeCharacters(dateIn);
        xmlw.writeEndElement(); 

    }
    
    private static void writeMethodElement(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException{
        xmlw.writeStartElement("method");
        xmlw.writeStartElement("dataColl");
        writeFullElement(xmlw, "timeMeth", dto2Primitive(version, DatasetFieldConstant.timeMethod)); 
        writeFullElement(xmlw, "dataCollector", dto2Primitive(version, DatasetFieldConstant.dataCollector));         
        writeFullElement(xmlw, "collectorTraining", dto2Primitive(version, DatasetFieldConstant.collectorTraining));   
        writeFullElement(xmlw, "frequenc", dto2Primitive(version, DatasetFieldConstant.frequencyOfDataCollection));      
        writeFullElement(xmlw, "sampProc", dto2Primitive(version, DatasetFieldConstant.samplingProcedure));

        writeTargetSampleElement(xmlw, version);

        writeFullElement(xmlw, "deviat", dto2Primitive(version, DatasetFieldConstant.deviationsFromSampleDesign));

        xmlw.writeStartElement("sources");
        writeFullElementList(xmlw, "dataSrc", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        writeFullElement(xmlw, "srcOrig", dto2Primitive(version, DatasetFieldConstant.originOfSources));
        writeFullElement(xmlw, "srcChar", dto2Primitive(version, DatasetFieldConstant.characteristicOfSources));
        writeFullElement(xmlw, "srcDocu", dto2Primitive(version, DatasetFieldConstant.accessToSources));
        xmlw.writeEndElement(); //sources

        writeFullElement(xmlw, "collMode", dto2Primitive(version, DatasetFieldConstant.collectionMode)); 
        writeFullElement(xmlw, "resInstru", dto2Primitive(version, DatasetFieldConstant.researchInstrument)); 
        writeFullElement(xmlw, "collSitu", dto2Primitive(version, DatasetFieldConstant.dataCollectionSituation)); 
        writeFullElement(xmlw, "actMin", dto2Primitive(version, DatasetFieldConstant.actionsToMinimizeLoss));
        writeFullElement(xmlw, "conOps", dto2Primitive(version, DatasetFieldConstant.controlOperations));
        writeFullElement(xmlw, "weight", dto2Primitive(version, DatasetFieldConstant.weighting));  
        writeFullElement(xmlw, "cleanOps", dto2Primitive(version, DatasetFieldConstant.cleaningOperations));

        xmlw.writeEndElement(); //dataColl
        xmlw.writeStartElement("anlyInfo");
        //writeFullElement(xmlw, "anylInfo", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        writeFullElement(xmlw, "respRate", dto2Primitive(version, DatasetFieldConstant.responseRate));  
        writeFullElement(xmlw, "EstSmpErr", dto2Primitive(version, DatasetFieldConstant.samplingErrorEstimates));
        writeFullElement(xmlw, "dataAppr", dto2Primitive(version, DatasetFieldConstant.otherDataAppraisal)); 
        xmlw.writeEndElement(); //anlyInfo
        writeNotesElement(xmlw, version);
        
        xmlw.writeEndElement();//method
    }
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException{ 
        
        //Key Words and Topic Classification
        
        xmlw.writeStartElement("subject");        
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())){
                        for ( String subject : fieldDTO.getMultipleVocab()){
                            xmlw.writeStartElement("keyword");
                            xmlw.writeCharacters(subject);
                            xmlw.writeEndElement(); //Keyword
                        }
                    }
                    
                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            String keywordVocab = "";
                            String keywordURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    keywordValue =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocab.equals(next.getTypeName())) {
                                    keywordVocab =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocabURI.equals(next.getTypeName())) {
                                    keywordURI =  next.getSinglePrimitive();
                                }
                            }
                            if (!keywordValue.isEmpty()){
                                xmlw.writeStartElement("keyword"); 
                                if(!keywordVocab.isEmpty()){
                                   writeAttribute(xmlw,"vocab",keywordVocab); 
                                }
                                if(!keywordURI.isEmpty()){
                                   writeAttribute(xmlw,"vocabURI",keywordURI);
                                } 
                                xmlw.writeCharacters(keywordValue);
                                xmlw.writeEndElement(); //Keyword
                            }

                        }
                    }
                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String topicClassificationValue = "";
                            String topicClassificationVocab = "";
                            String topicClassificationURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                    topicClassificationValue =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.topicClassVocab.equals(next.getTypeName())) {
                                    topicClassificationVocab =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.topicClassVocabURI.equals(next.getTypeName())) {
                                    topicClassificationURI =  next.getSinglePrimitive();
                                }
                            }
                            if (!topicClassificationValue.isEmpty()){
                                xmlw.writeStartElement("topcClas"); 
                                if(!topicClassificationVocab.isEmpty()){
                                   writeAttribute(xmlw,"vocab",topicClassificationVocab); 
                                } 
                                if(!topicClassificationURI.isEmpty()){
                                   writeAttribute(xmlw,"vocabURI",topicClassificationURI);
                                } 
                                xmlw.writeCharacters(topicClassificationValue);
                                xmlw.writeEndElement(); //topcClas
                            }
                        }
                    }
                }
            }
        }        
        xmlw.writeEndElement(); // subject       
    }
    
    private static void writeAuthorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                xmlw.writeStartElement("rspStmt");
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        String authorName = "";
                        String authorAffiliation = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    authorName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorAffiliation.equals(next.getTypeName())) {
                                    authorAffiliation =  next.getSinglePrimitive();
                                }
                            }
                            if (!authorName.isEmpty()){
                                xmlw.writeStartElement("AuthEnty"); 
                                if(!authorAffiliation.isEmpty()){
                                   writeAttribute(xmlw,"affiliation",authorAffiliation); 
                                } 
                                xmlw.writeCharacters(authorName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }

                    } else if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        String contributorName = "";
                        String contributorType = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                    contributorName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.contributorType.equals(next.getTypeName())) {
                                    contributorType =  next.getSinglePrimitive();
                                }
                            }
                            if (!contributorName.isEmpty()){
                                xmlw.writeStartElement("othId");
                                if(!contributorType.isEmpty()){
                                    writeAttribute(xmlw,"role", contributorType);
                                }
                                xmlw.writeCharacters(contributorName);
                                xmlw.writeEndElement(); //othId
                            }
                        }
                    }
                }
                xmlw.writeEndElement(); //rspStmt
            }

        }
    }
    
    private static void writeContactsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.datasetContact.equals(fieldDTO.getTypeName())) {
                        String datasetContactName = "";
                        String datasetContactAffiliation = "";
                        String datasetContactEmail = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.datasetContactName.equals(next.getTypeName())) {
                                    datasetContactName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.datasetContactAffiliation.equals(next.getTypeName())) {
                                    datasetContactAffiliation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.datasetContactEmail.equals(next.getTypeName())) {
                                    datasetContactEmail = next.getSinglePrimitive();
                                }
                            }
                            // TODO: Since datasetContactEmail is a required field but datasetContactName is not consider not checking if datasetContactName is empty so we can write out datasetContactEmail.
                            if (!datasetContactName.isEmpty()){
                                xmlw.writeStartElement("contact"); 
                                if(!datasetContactAffiliation.isEmpty()){
                                   writeAttribute(xmlw,"affiliation",datasetContactAffiliation); 
                                } 
                                if(!datasetContactEmail.isEmpty()){
                                   writeAttribute(xmlw,"email",datasetContactEmail); 
                                } 
                                xmlw.writeCharacters(datasetContactName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeProducersElement(XMLStreamWriter xmlw, DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("prodStmt");
        for (Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();

            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {

                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String producerName = "";
                            String producerAffiliation = "";
                            String producerAbbreviation = "";
                            String producerLogo = "";
                            String producerURL = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.producerName.equals(next.getTypeName())) {
                                    producerName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAffiliation.equals(next.getTypeName())) {
                                    producerAffiliation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAbbreviation.equals(next.getTypeName())) {
                                    producerAbbreviation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerLogo.equals(next.getTypeName())) {
                                    producerLogo = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerURL.equals(next.getTypeName())) {
                                    producerURL = next.getSinglePrimitive();

                                }
                            }
                            if (!producerName.isEmpty()) {
                                xmlw.writeStartElement("producer");
                                if (!producerAffiliation.isEmpty()) {
                                    writeAttribute(xmlw, "affiliation", producerAffiliation);
                                }
                                if (!producerAbbreviation.isEmpty()) {
                                    writeAttribute(xmlw, "abbr", producerAbbreviation);
                                }
                                if (!producerLogo.isEmpty()) {
                                    writeAttribute(xmlw, "role", producerLogo);
                                }
                                if (!producerURL.isEmpty()) {
                                    writeAttribute(xmlw, "URI", producerURL);
                                }
                                xmlw.writeCharacters(producerName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                        
                    }
                }
            }
        }
        writeFullElement(xmlw, "prodDate", dto2Primitive(version, DatasetFieldConstant.productionDate));    
        writeFullElement(xmlw, "prodPlac", dto2Primitive(version, DatasetFieldConstant.productionPlace));
        writeSoftwareElement(xmlw, version);
  
        writeGrantElement(xmlw, version);
        xmlw.writeEndElement(); //prodStmt
    }
    
    private static void writeDistributorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.distributor.equals(fieldDTO.getTypeName())) {
                        //xmlw.writeStartElement("distrbtr");
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String distributorName = "";
                            String distributorAffiliation = "";
                            String distributorAbbreviation = "";
                            String distributorURL = "";
                            String distributorLogoURL = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.distributorName.equals(next.getTypeName())) {
                                    distributorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorAffiliation.equals(next.getTypeName())) {
                                    distributorAffiliation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorAbbreviation.equals(next.getTypeName())) {
                                    distributorAbbreviation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorURL.equals(next.getTypeName())) {
                                    distributorURL = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.distributorLogo.equals(next.getTypeName())) {
                                    distributorLogoURL = next.getSinglePrimitive();
                                }
                            }
                            if (!distributorName.isEmpty()) {
                                xmlw.writeStartElement("distrbtr");
                                if (!distributorAffiliation.isEmpty()) {
                                    writeAttribute(xmlw, "affiliation", distributorAffiliation);
                                }
                                if (!distributorAbbreviation.isEmpty()) {
                                    writeAttribute(xmlw, "abbr", distributorAbbreviation);
                                }
                                if (!distributorURL.isEmpty()) {
                                    writeAttribute(xmlw, "URI", distributorURL);
                                }
                                if (!distributorLogoURL.isEmpty()) {
                                    writeAttribute(xmlw, "role", distributorLogoURL);
                                }
                                xmlw.writeCharacters(distributorName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                        //xmlw.writeEndElement(); //rspStmt
                    }

                }
            }
        }
    }
    
    private static void writeRelPublElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String pubString = "";
                            String citation = "";
                            String IDType = "";
                            String IDNo = "";
                            String url = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.publicationCitation.equals(next.getTypeName())) {
                                    citation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDType.equals(next.getTypeName())) {
                                    IDType =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDNumber.equals(next.getTypeName())) {
                                    IDNo =   next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationURL.equals(next.getTypeName())) {
                                    url =  next.getSinglePrimitive();
                                }
                            }
                            if (citation != null && !citation.trim().equals("")) {
                                xmlw.writeStartElement("relPubl");
                                xmlw.writeStartElement("citation");
                                if (IDNo != null && !IDNo.trim().equals("")) {
                                    xmlw.writeStartElement("titlStmt");
                                    xmlw.writeStartElement("IDNo");
                                    if (IDType != null && !IDType.trim().equals("")) {
                                        xmlw.writeAttribute("agency", IDType );
                                    }
                                    xmlw.writeCharacters(IDNo);
                                    xmlw.writeEndElement(); //IDNo
                                    xmlw.writeEndElement(); // titlStmt
                                }

                                writeFullElement(xmlw,"biblCit",citation);
                                xmlw.writeEndElement(); //citation
                                if (url != null && !url.trim().equals("") ) {
                                    xmlw.writeStartElement("ExtLink");
                                    xmlw.writeAttribute("URI", url);
                                    xmlw.writeEndElement(); //ExtLink
                                }
                                xmlw.writeEndElement(); //relPubl
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static String appendCommaSeparatedValue(String inVal, String next) {
        if (!next.isEmpty()) {
            if (!inVal.isEmpty()) {
                return inVal + ", " + next;
            } else {
                return next;
            }
        }
        return inVal;
    }
    
    private static void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        String descriptionText = "";
                        String descriptionDate = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionText =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.descriptionDate.equals(next.getTypeName())) {
                                    descriptionDate =  next.getSinglePrimitive();
                                }
                            }
                            if (!descriptionText.isEmpty()){
                                xmlw.writeStartElement("abstract"); 
                                if(!descriptionDate.isEmpty()){
                                   writeAttribute(xmlw,"date",descriptionDate); 
                                } 
                                xmlw.writeCharacters(descriptionText);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeGrantElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        String grantNumber = "";
                        String grantAgency = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.grantNumberValue.equals(next.getTypeName())) {
                                    grantNumber =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.grantNumberAgency.equals(next.getTypeName())) {
                                    grantAgency =  next.getSinglePrimitive();
                                }
                            }
                            if (!grantNumber.isEmpty()){
                                xmlw.writeStartElement("grantNo"); 
                                if(!grantAgency.isEmpty()){
                                   writeAttribute(xmlw,"agency",grantAgency); 
                                } 
                                xmlw.writeCharacters(grantNumber);
                                xmlw.writeEndElement(); //grantno
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeOtherIdElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        String otherId = "";
                        String otherIdAgency = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                    otherId =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.otherIdAgency.equals(next.getTypeName())) {
                                    otherIdAgency =  next.getSinglePrimitive();
                                }
                            }
                            if (!otherId.isEmpty()){
                                xmlw.writeStartElement("IDNo"); 
                                if(!otherIdAgency.isEmpty()){
                                   writeAttribute(xmlw,"agency",otherIdAgency); 
                                } 
                                xmlw.writeCharacters(otherId);
                                xmlw.writeEndElement(); //IDNo
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeSoftwareElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.software.equals(fieldDTO.getTypeName())) {
                        String softwareName = "";
                        String softwareVersion = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.softwareName.equals(next.getTypeName())) {
                                    softwareName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.softwareVersion.equals(next.getTypeName())) {
                                    softwareVersion =  next.getSinglePrimitive();
                                }
                            }
                            if (!softwareName.isEmpty()){
                                xmlw.writeStartElement("software"); 
                                if(!softwareVersion.isEmpty()){
                                   writeAttribute(xmlw,"version",softwareVersion); 
                                } 
                                xmlw.writeCharacters(softwareName);
                                xmlw.writeEndElement(); //software
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeSeriesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.series.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("serStmt");                        
                        String seriesName = "";
                        String seriesInformation = "";
                        Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.seriesName.equals(next.getTypeName())) {
                                    seriesName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.seriesInformation.equals(next.getTypeName())) {
                                    seriesInformation =  next.getSinglePrimitive();
                                }
                            }
                            if (!seriesName.isEmpty()){
                                xmlw.writeStartElement("serName"); 
                                xmlw.writeCharacters(seriesName);
                                xmlw.writeEndElement(); //grantno
                            }
                            if (!seriesInformation.isEmpty()){
                                xmlw.writeStartElement("serInfo"); 
                                xmlw.writeCharacters(seriesInformation);
                                xmlw.writeEndElement(); //grantno
                            }
                        xmlw.writeEndElement(); //serStmt
                    }
                }
            }
        }
    }
    
    private static void writeTargetSampleElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("socialscience".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.targetSampleSize.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("targetSampleSize");
                        String sizeFormula = "";
                        String actualSize = "";
                        Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            FieldDTO next = iterator.next();
                            if (DatasetFieldConstant.targetSampleSizeFormula.equals(next.getTypeName())) {
                                sizeFormula = next.getSinglePrimitive();
                            }
                            if (DatasetFieldConstant.targetSampleActualSize.equals(next.getTypeName())) {
                                actualSize = next.getSinglePrimitive();
                            }
                        }

                        if (!sizeFormula.isEmpty()) {
                            xmlw.writeStartElement("sampleSizeFormula");
                            xmlw.writeCharacters(sizeFormula);
                            xmlw.writeEndElement(); //sampleSizeFormula
                        }
                        if (!actualSize.isEmpty()) {
                            xmlw.writeStartElement("sampleSize");
                            xmlw.writeCharacters(actualSize);
                            xmlw.writeEndElement(); //sampleSize
                        }
                        xmlw.writeEndElement(); // targetSampleSize
                    }
                }
            }
        }
    }
    
    private static void writeNotesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("socialscience".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.socialScienceNotes.equals(fieldDTO.getTypeName())) {
                        String notesText = "";
                        String notesType = "";
                        String notesSubject= "";
                        Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            FieldDTO next = iterator.next();
                            if (DatasetFieldConstant.socialScienceNotesText.equals(next.getTypeName())) {
                                notesText = next.getSinglePrimitive();
                            }
                            if (DatasetFieldConstant.socialScienceNotesType.equals(next.getTypeName())) {
                                notesType = next.getSinglePrimitive();
                            }
                            if (DatasetFieldConstant.socialScienceNotesSubject.equals(next.getTypeName())) {
                                notesSubject = next.getSinglePrimitive();
                            }
                        }
                        if (!notesText.isEmpty()) {
                            xmlw.writeStartElement("notes");
                                if(!notesType.isEmpty()){
                                   writeAttribute(xmlw,"type",notesType); 
                                } 
                                if(!notesSubject.isEmpty()){
                                   writeAttribute(xmlw,"subject",notesSubject); 
                                } 
                            xmlw.writeCharacters(notesText);
                            xmlw.writeEndElement(); 
                        }
                    }
                }
            }
        }
    }
    
    // TODO: 
    // see if there's more information that we could encode in this otherMat. 
    // contentType? Unfs and such? (in the "short" DDI that is being used for 
    // harvesting *all* files are encoded as otherMats; even tabular ones.
    private static void createOtherMats(XMLStreamWriter xmlw, List<FileDTO> fileDtos) throws XMLStreamException {
        // The preferred URL for this dataverse, for cooking up the file access API links:
        String dataverseUrl = getDataverseSiteUrl();
        
        for (FileDTO fileDTo : fileDtos) {
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (fileDTo.getDataFile().getDataTables() == null || fileDTo.getDataFile().getDataTables().isEmpty()) {
                xmlw.writeStartElement("otherMat");
                writeAttribute(xmlw, "ID", "f" + fileDTo.getDataFile().getId());
                String pidURL = fileDTo.getDataFile().getPidURL();
                if (pidURL != null && !pidURL.isEmpty()){
                    writeAttribute(xmlw, "URI", pidURL);
                } else {
                    writeAttribute(xmlw, "URI", dataverseUrl + "/api/access/datafile/" + fileDTo.getDataFile().getId());
                }
                writeAttribute(xmlw, "level", "datafile");
                xmlw.writeStartElement("labl");
                xmlw.writeCharacters(fileDTo.getDataFile().getFilename());
                xmlw.writeEndElement(); // labl
                writeFileDescription(xmlw, fileDTo);
                // there's no readily available field in the othermat section 
                // for the content type (aka mime type); so we'll store it in this
                // specially formatted notes section:
                String contentType = fileDTo.getDataFile().getContentType();
                if (!StringUtilisEmpty(contentType)) {
                    xmlw.writeStartElement("notes");
                    writeAttribute(xmlw, "level", LEVEL_FILE);
                    writeAttribute(xmlw, "type", NOTE_TYPE_CONTENTTYPE);
                    writeAttribute(xmlw, "subject", NOTE_SUBJECT_CONTENTTYPE);
                    xmlw.writeCharacters(contentType);
                    xmlw.writeEndElement(); // notes
                }
                xmlw.writeEndElement(); // otherMat
            }
        }
    }
    
    // An alternative version of the createOtherMats method - this one is used 
    // when a "full" DDI is being cooked; just like the fileDscr and data/var sections methods, 
    // it operates on the list of FileMetadata entities, not on File DTOs. This is because
    // DTOs do not support "tabular", variable-level metadata yet. And we need to be able to 
    // tell if this file is in fact tabular data - so that we know if it needs an
    // otherMat, or a fileDscr section. 
    // -- L.A. 4.5 
    
    private static void createOtherMatsFromFileMetadatas(XMLStreamWriter xmlw, List<FileMetadata> fileMetadatas) throws XMLStreamException {
        // The preferred URL for this dataverse, for cooking up the file access API links:
        String dataverseUrl = getDataverseSiteUrl();
        
        for (FileMetadata fileMetadata : fileMetadatas) {
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (fileMetadata.getDataFile() != null && !fileMetadata.getDataFile().isTabularData()) {
                xmlw.writeStartElement("otherMat");
                writeAttribute(xmlw, "ID", "f" + fileMetadata.getDataFile().getId());
                String dfIdentifier = fileMetadata.getDataFile().getIdentifier();
                if (dfIdentifier != null && !dfIdentifier.isEmpty()){
                    GlobalId globalId = new GlobalId(fileMetadata.getDataFile());
                    writeAttribute(xmlw, "URI",  globalId.toURL().toString()); 
                }  else {
                    writeAttribute(xmlw, "URI", dataverseUrl + "/api/access/datafile/" + fileMetadata.getDataFile().getId()); 
                }

                writeAttribute(xmlw, "level", "datafile");
                xmlw.writeStartElement("labl");
                xmlw.writeCharacters(fileMetadata.getLabel());
                xmlw.writeEndElement(); // labl
                
                String description = fileMetadata.getDescription();
                if (description != null) {
                    xmlw.writeStartElement("txt");
                    xmlw.writeCharacters(description);
                    xmlw.writeEndElement(); // txt
                }
                // there's no readily available field in the othermat section 
                // for the content type (aka mime type); so we'll store it in this
                // specially formatted notes section:
                String contentType = fileMetadata.getDataFile().getContentType();
                if (!StringUtilisEmpty(contentType)) {
                    xmlw.writeStartElement("notes");
                    writeAttribute(xmlw, "level", LEVEL_FILE);
                    writeAttribute(xmlw, "type", NOTE_TYPE_CONTENTTYPE);
                    writeAttribute(xmlw, "subject", NOTE_SUBJECT_CONTENTTYPE);
                    xmlw.writeCharacters(contentType);
                    xmlw.writeEndElement(); // notes
                }
                xmlw.writeEndElement(); // otherMat
            }
        }
    }
    
    private static void writeFileDescription(XMLStreamWriter xmlw, FileDTO fileDTo) throws XMLStreamException {
        xmlw.writeStartElement("txt");
        String description = fileDTo.getDataFile().getDescription();
        if (description != null) {
            xmlw.writeCharacters(description);
        }
        xmlw.writeEndElement(); // txt
    }
    
    private static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
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
    
    private static List<String> dto2PrimitiveList(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getMultiplePrimitive();
                }
            }
        }
        return null;
    }

    private static void writeFullElementList(XMLStreamWriter xmlw, String name, List<String> values) throws XMLStreamException {
        //For the simplest Elements we can 
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                xmlw.writeStartElement(name);
                xmlw.writeCharacters(value);
                xmlw.writeEndElement(); // labl
            }
        }
    }
    
    private static void writeFullElement (XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        //For the simplest Elements we can 
        if (!StringUtilisEmpty(value)) {
            xmlw.writeStartElement(name);
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }
    }

    private static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (!StringUtilisEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }

    private static boolean StringUtilisEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    private static void saveJsonToDisk(String datasetVersionAsJson) throws IOException {
        Files.write(Paths.get("/tmp/out.json"), datasetVersionAsJson.getBytes());
    }
    
    /**
     * The "official", designated URL of the site;
     * can be defined as a complete URL; or derived from the 
     * "official" hostname. If none of these options is set,
     * defaults to the InetAddress.getLocalHOst() and https;
     */
    private static String getDataverseSiteUrl() {
        String hostUrl = System.getProperty(SITE_URL);
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty(FQDN);
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                hostName = null;
            }
        }
        
        if (hostName != null) {
            return "https://" + hostName;
        }
        
        return "http://localhost:8080";
    }
    
    
    
    
    // Methods specific to the tabular data ("<dataDscr>") section. 
    // Note that these do NOT operate on DTO objects, but instead directly 
    // on Dataverse DataVariable, DataTable, etc. objects. 
    // This is because for this release (4.5) we are recycling the already available 
    // code, and this is what we got. (We already have DTO objects for DataTable, 
    // and DataVariable, etc., but the current version JsonPrinter.jsonAsDatasetDto() 
    // does not produce JSON for these objects - it stops at DataFile. Eventually 
    // we want all of our objects to be exportable as JSON, and then all the exports
    // can go through the same DTO state... But we don't have time for it now; 
    // plus, the structure of file-level metadata is currently being re-designed, 
    // so we probably should not invest any time into it right now). -- L.A. 4.5
    
    public static void createDataDscr(XMLStreamWriter xmlw, DatasetVersion datasetVersion) throws XMLStreamException {

        if (datasetVersion.getFileMetadatas() == null || datasetVersion.getFileMetadatas().isEmpty()) {
            return;
        }

        boolean tabularData = false;

        // we're not writing the opening <dataDscr> tag until we find an actual 
        // tabular datafile.
        for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();

            /**
             * Previously (in Dataverse 5.3 and below) the dataDscr section was
             * included for restricted files but that meant that summary
             * statistics were exposed. (To get at these statistics, API users
             * should instead use the "Data Variable Metadata Access" endpoint.)
             * These days we return early to avoid this exposure.
             */
            if (dataFile.isRestricted()) {
                return;
            }

            if (dataFile != null && dataFile.isTabularData()) {
                if (!tabularData) {
                    xmlw.writeStartElement("dataDscr");
                    tabularData = true;
                }
                for (VarGroup varGrp : fileMetadata.getVarGroups()) {
                    createVarGroupDDI(xmlw, varGrp);
                }

                List<DataVariable> vars = dataFile.getDataTable().getDataVariables();

                for (DataVariable var : vars) {
                    createVarDDI(xmlw, var, fileMetadata);
                }
            }
        }

        if (tabularData) {
            xmlw.writeEndElement(); // dataDscr
        }
    }
    private static void createVarGroupDDI(XMLStreamWriter xmlw, VarGroup varGrp) throws XMLStreamException {
        xmlw.writeStartElement("varGrp");
        writeAttribute(xmlw, "ID", "VG" + varGrp.getId().toString());
        String vars = "";
        Set<DataVariable> varsInGroup = varGrp.getVarsInGroup();
        for (DataVariable var : varsInGroup) {
            vars = vars + " v" + var.getId();
        }
        vars = vars.trim();
        writeAttribute(xmlw, "var", vars );


        if (!StringUtilisEmpty(varGrp.getLabel())) {
            xmlw.writeStartElement("labl");
            xmlw.writeCharacters(varGrp.getLabel());
            xmlw.writeEndElement(); // group label (labl)
        }

        xmlw.writeEndElement(); //varGrp
    }
    
    private static void createVarDDI(XMLStreamWriter xmlw, DataVariable dv, FileMetadata fileMetadata) throws XMLStreamException {
        xmlw.writeStartElement("var");
        writeAttribute(xmlw, "ID", "v" + dv.getId().toString());
        writeAttribute(xmlw, "name", dv.getName());

        VariableMetadata vm = null;
        for (VariableMetadata vmIter : dv.getVariableMetadatas()) {
            FileMetadata fm = vmIter.getFileMetadata();
            if (fm != null && fm.equals(fileMetadata) ){
                vm = vmIter;
                break;
            }
        }

        if (dv.getNumberOfDecimalPoints() != null) {
            writeAttribute(xmlw, "dcml", dv.getNumberOfDecimalPoints().toString());
        }

        if (dv.isOrderedCategorical()) {
            writeAttribute(xmlw, "nature", "ordinal");
        }

        if (dv.getInterval() != null) {
            String interval = dv.getIntervalLabel();
            if (interval != null) {
                writeAttribute(xmlw, "intrvl", interval);
            }
        }

        if (vm != null) {
            if (vm.isIsweightvar()) {
                writeAttribute(xmlw, "wgt", "wgt");
            }
            if (vm.isWeighted() && vm.getWeightvariable() != null) {
                writeAttribute(xmlw, "wgt-var", "v"+vm.getWeightvariable().getId().toString());
            }
        }

        // location
        xmlw.writeEmptyElement("location");
        if (dv.getFileStartPosition() != null) {
            writeAttribute(xmlw, "StartPos", dv.getFileStartPosition().toString());
        }
        if (dv.getFileEndPosition() != null) {
            writeAttribute(xmlw, "EndPos", dv.getFileEndPosition().toString());
        }
        if (dv.getRecordSegmentNumber() != null) {
            writeAttribute(xmlw, "RecSegNo", dv.getRecordSegmentNumber().toString());
        }

        writeAttribute(xmlw, "fileid", "f" + dv.getDataTable().getDataFile().getId().toString());

        // labl
        if ((vm == null || StringUtilisEmpty(vm.getLabel())) && !StringUtilisEmpty(dv.getLabel())) {
            xmlw.writeStartElement("labl");
            writeAttribute(xmlw, "level", "variable");
            xmlw.writeCharacters(dv.getLabel());
            xmlw.writeEndElement(); //labl
        } else if (vm != null && !StringUtilisEmpty(vm.getLabel())) {
            xmlw.writeStartElement("labl");
            writeAttribute(xmlw, "level", "variable");
            xmlw.writeCharacters(vm.getLabel());
            xmlw.writeEndElement(); //labl
        }

        if (vm != null) {
            if (!StringUtilisEmpty(vm.getLiteralquestion()) || !StringUtilisEmpty(vm.getInterviewinstruction()) || !StringUtilisEmpty(vm.getPostquestion())) {
                xmlw.writeStartElement("qstn");
                if (!StringUtilisEmpty(vm.getLiteralquestion())) {
                    xmlw.writeStartElement("qstnLit");
                    xmlw.writeCharacters(vm.getLiteralquestion());
                    xmlw.writeEndElement(); // qstnLit
                }
                if (!StringUtilisEmpty(vm.getInterviewinstruction())) {
                    xmlw.writeStartElement("ivuInstr");
                    xmlw.writeCharacters(vm.getInterviewinstruction());
                    xmlw.writeEndElement(); //ivuInstr
                }
                if (!StringUtilisEmpty(vm.getPostquestion())) {
                    xmlw.writeStartElement("postQTxt");
                    xmlw.writeCharacters(vm.getPostquestion());
                    xmlw.writeEndElement(); //ivuInstr
                }
                xmlw.writeEndElement(); //qstn
            }
        }

        // invalrng
        boolean invalrngAdded = false;
        for (VariableRange range : dv.getInvalidRanges()) {
            //if (range.getBeginValueType() != null && range.getBeginValueType().getName().equals(DB_VAR_RANGE_TYPE_POINT)) {
            if (range.getBeginValueType() != null && range.isBeginValueTypePoint()) {
                if (range.getBeginValue() != null) {
                    invalrngAdded = checkParentElement(xmlw, "invalrng", invalrngAdded);
                    xmlw.writeEmptyElement("item");
                    writeAttribute(xmlw, "VALUE", range.getBeginValue());
                }
            } else {
                invalrngAdded = checkParentElement(xmlw, "invalrng", invalrngAdded);
                xmlw.writeEmptyElement("range");
                if (range.getBeginValueType() != null && range.getBeginValue() != null) {
                    if (range.isBeginValueTypeMin()) {
                        writeAttribute(xmlw, "min", range.getBeginValue());
                    } else if (range.isBeginValueTypeMinExcl()) {
                        writeAttribute(xmlw, "minExclusive", range.getBeginValue());
                    }
                }
                if (range.getEndValueType() != null && range.getEndValue() != null) {
                    if (range.isEndValueTypeMax()) {
                        writeAttribute(xmlw, "max", range.getEndValue());
                    } else if (range.isEndValueTypeMaxExcl()) {
                        writeAttribute(xmlw, "maxExclusive", range.getEndValue());
                    }
                }
            }
        }
        if (invalrngAdded) {
            xmlw.writeEndElement(); // invalrng
        }

        //universe
        if (vm != null) {
            if (!StringUtilisEmpty(vm.getUniverse())) {
                xmlw.writeStartElement("universe");
                xmlw.writeCharacters(vm.getUniverse());
                xmlw.writeEndElement(); //universe
            }
        }

        //sum stats
        for (SummaryStatistic sumStat : dv.getSummaryStatistics()) {
            xmlw.writeStartElement("sumStat");
            if (sumStat.getTypeLabel() != null) {
                writeAttribute(xmlw, "type", sumStat.getTypeLabel());
            } else {
                writeAttribute(xmlw, "type", "unknown");
            }
            xmlw.writeCharacters(sumStat.getValue());
            xmlw.writeEndElement(); //sumStat
        }

        // categories
        for (VariableCategory cat : dv.getCategories()) {
            xmlw.writeStartElement("catgry");
            if (cat.isMissing()) {
                writeAttribute(xmlw, "missing", "Y");
            }

            // catValu
            xmlw.writeStartElement("catValu");
            xmlw.writeCharacters(cat.getValue());
            xmlw.writeEndElement(); //catValu

            // label
            if (!StringUtilisEmpty(cat.getLabel())) {
                xmlw.writeStartElement("labl");
                writeAttribute(xmlw, "level", "category");
                xmlw.writeCharacters(cat.getLabel());
                xmlw.writeEndElement(); //labl
            }

            // catStat
            if (cat.getFrequency() != null) {
                xmlw.writeStartElement("catStat");
                writeAttribute(xmlw, "type", "freq");
                // if frequency is actually a long value, we want to write "100" instead of "100.0"
                if (Math.floor(cat.getFrequency()) == cat.getFrequency()) {
                    xmlw.writeCharacters(new Long(cat.getFrequency().longValue()).toString());
                } else {
                    xmlw.writeCharacters(cat.getFrequency().toString());
                }
                xmlw.writeEndElement(); //catStat
            }

            //catStat weighted freq
            if (vm != null && vm.isWeighted()) {
                for (CategoryMetadata cm : vm.getCategoriesMetadata()) {
                    if (cm.getCategory().getValue().equals(cat.getValue())) {
                        xmlw.writeStartElement("catStat");
                        writeAttribute(xmlw, "wgtd", "wgtd");
                        writeAttribute(xmlw, "type", "freq");
                        xmlw.writeCharacters(cm.getWfreq().toString());
                        xmlw.writeEndElement(); //catStat
                        break;
                    }
                }
            }

            xmlw.writeEndElement(); //catgry
        }


        // varFormat
        xmlw.writeEmptyElement("varFormat");
        if (dv.isTypeNumeric()) {
            writeAttribute(xmlw, "type", "numeric");
        } else if (dv.isTypeCharacter()) {
            writeAttribute(xmlw, "type", "character");
        } else {
            throw new XMLStreamException("Illegal Variable Format Type!");
        }
        writeAttribute(xmlw, "formatname", dv.getFormat());
        //experiment writeAttribute(xmlw, "schema", dv.getFormatSchema());
        writeAttribute(xmlw, "category", dv.getFormatCategory());

        // notes
        if (dv.getUnf() != null && !"".equals(dv.getUnf())) {
            xmlw.writeStartElement("notes");
            writeAttribute(xmlw, "subject", "Universal Numeric Fingerprint");
            writeAttribute(xmlw, "level", "variable");
            writeAttribute(xmlw, "type", "Dataverse:UNF");
            xmlw.writeCharacters(dv.getUnf());
            xmlw.writeEndElement(); //notes
        }

        if (vm != null) {
            if (!StringUtilisEmpty(vm.getNotes())) {
                xmlw.writeStartElement("notes");
                xmlw.writeCData(vm.getNotes());
                xmlw.writeEndElement(); //notes CDATA
            }
        }



        xmlw.writeEndElement(); //var

    }
    
    private static void createFileDscr(XMLStreamWriter xmlw, DatasetVersion datasetVersion) throws XMLStreamException {
        String dataverseUrl = getDataverseSiteUrl();
        for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();

            if (dataFile != null && dataFile.isTabularData()) {
                DataTable dt = dataFile.getDataTable();
                xmlw.writeStartElement("fileDscr");
                writeAttribute(xmlw, "ID", "f" + dataFile.getId());
                writeAttribute(xmlw, "URI", dataverseUrl + "/api/access/datafile/" + dataFile.getId());

                xmlw.writeStartElement("fileTxt");
                xmlw.writeStartElement("fileName");
                xmlw.writeCharacters(fileMetadata.getLabel());
                xmlw.writeEndElement(); // fileName

                if (dt.getCaseQuantity() != null || dt.getVarQuantity() != null || dt.getRecordsPerCase() != null) {
                    xmlw.writeStartElement("dimensns");

                    if (dt.getCaseQuantity() != null) {
                        xmlw.writeStartElement("caseQnty");
                        xmlw.writeCharacters(dt.getCaseQuantity().toString());
                        xmlw.writeEndElement(); // caseQnty
                    }

                    if (dt.getVarQuantity() != null) {
                        xmlw.writeStartElement("varQnty");
                        xmlw.writeCharacters(dt.getVarQuantity().toString());
                        xmlw.writeEndElement(); // varQnty
                    }

                    if (dt.getRecordsPerCase() != null) {
                        xmlw.writeStartElement("recPrCas");
                        xmlw.writeCharacters(dt.getRecordsPerCase().toString());
                        xmlw.writeEndElement(); // recPrCas
                    }

                    xmlw.writeEndElement(); // dimensns
                }

                xmlw.writeStartElement("fileType");
                xmlw.writeCharacters(dataFile.getContentType());
                xmlw.writeEndElement(); // fileType

                xmlw.writeEndElement(); // fileTxt

                // various notes:
                // this specially formatted note section is used to store the UNF
                // (Universal Numeric Fingerprint) signature:
                if (dt.getUnf() != null && !dt.getUnf().equals("")) {
                    xmlw.writeStartElement("notes");
                    writeAttribute(xmlw, "level", LEVEL_FILE);
                    writeAttribute(xmlw, "type", NOTE_TYPE_UNF);
                    writeAttribute(xmlw, "subject", NOTE_SUBJECT_UNF);
                    xmlw.writeCharacters(dt.getUnf());
                    xmlw.writeEndElement(); // notes
                }

                if (dataFile.getTags() != null) {
                    for (int i = 0; i < dataFile.getTags().size(); i++) {
                        xmlw.writeStartElement("notes");
                        writeAttribute(xmlw, "level", LEVEL_FILE);
                        writeAttribute(xmlw, "type", NOTE_TYPE_TAG);
                        writeAttribute(xmlw, "subject", NOTE_SUBJECT_TAG);
                        xmlw.writeCharacters(dataFile.getTags().get(i).getTypeLabel());
                        xmlw.writeEndElement(); // notes
                    }
                }

                // TODO: add the remaining fileDscr elements!
                xmlw.writeEndElement(); // fileDscr
            }
        }
    }
    
    

    private static boolean checkParentElement(XMLStreamWriter xmlw, String elementName, boolean elementAdded) throws XMLStreamException {
        if (!elementAdded) {
            xmlw.writeStartElement(elementName);
        }

        return true;
    }

    public static void datasetHtmlDDI(InputStream datafile, OutputStream outputStream) throws XMLStreamException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            Document document;
            InputStream  styleSheetInput = DdiExportUtil.class.getClassLoader().getResourceAsStream("edu/harvard/iq/dataverse/codebook2-0.xsl");

            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(datafile);

            // Use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource stylesource = new StreamSource(styleSheetInput);
            Transformer transformer = tFactory.newTransformer(stylesource);

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException tce) {
            // Error generated by the parser
            logger.severe("Transformer Factory error" + "   " + tce.getMessage());
        } catch (TransformerException te) {
            // Error generated by the parser
            logger.severe("Transformation error" + "   " + te.getMessage());

        } catch (SAXException sxe) {
            // Error generated by this application
            // (or a parser-initialization error)
            logger.severe("SAX error " + sxe.getMessage());

        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.severe("Parser configuration error " + pce.getMessage());
        } catch (IOException ioe) {
            // I/O error
            logger.info("I/O error " + ioe.getMessage());
        }

    }

    public static void injectSettingsService(SettingsServiceBean settingsSvc) {
        settingsService=settingsSvc;
    }

}
