package edu.harvard.iq.dataverse.export.ddi;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());

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
        xmlw.writeAttribute("xsi:schemaLocation", "ddi:codebook:2_5 http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd");
        writeAttribute(xmlw, "version", "2.5");
        createStdyDscr(xmlw, datasetDto);
        createdataDscr(xmlw, datasetDto.getDatasetVersion().getFiles());
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
        String persistentAgency = datasetDto.getProtocol();
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
        xmlw.writeCharacters(persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo
       
        xmlw.writeEndElement(); // titlStmt

        writeAuthorsElement(xmlw, version);
        writeProducersElement(xmlw, version);
        
        xmlw.writeStartElement("distStmt");
        writeFullElement(xmlw, "distrbtr", datasetDto.getPublisher());
        writeFullElement(xmlw, "distDate", datasetDto.getPublicationDate());
        xmlw.writeEndElement(); // diststmt

        xmlw.writeEndElement(); // citation
        //End Citation Block
        
        //Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");
        
        writeSubjectElement(xmlw, version); //Subject and Keywords
        writeAbstractElement(xmlw, version); // Description
        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.notesText));
        
        writeSummaryDescriptionElement(xmlw, version);
        writeRelPublElement(xmlw, version);

        writeOtherIdElement(xmlw, version);
        writeDistributorsElement(xmlw, version);
        writeContactsElement(xmlw, version);
        writeFullElement(xmlw, "depositr", dto2Primitive(version, DatasetFieldConstant.depositor));    
        writeFullElement(xmlw, "depDate", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));  
        
        writeFullElementList(xmlw, "relMat", dto2PrimitiveList(version, DatasetFieldConstant.relatedMaterial));
        writeFullElementList(xmlw, "relStdy", dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets));
        writeFullElementList(xmlw, "othRefs", dto2PrimitiveList(version, DatasetFieldConstant.otherReferences));
        writeSeriesElement(xmlw, version);
        writeSoftwareElement(xmlw, version);
        writeFullElementList(xmlw, "dataSrc", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        writeFullElement(xmlw, "srcOrig", dto2Primitive(version, DatasetFieldConstant.originOfSources)); 
        writeFullElement(xmlw, "srcChar", dto2Primitive(version, DatasetFieldConstant.characteristicOfSources)); 
        writeFullElement(xmlw, "srcDocu", dto2Primitive(version, DatasetFieldConstant.accessToSources)); 
        xmlw.writeEndElement(); // stdyInfo
        // End Info Block
        
        //Social Science Metadata block
               
        writeMethodElement(xmlw, version);
        
        //Terms of Use and Access
        writeFullElement(xmlw, "useStmt", version.getTermsOfUse()); 
        writeFullElement(xmlw, "confDec", version.getConfidentialityDeclaration()); 
        writeFullElement(xmlw, "specPerm", version.getSpecialPermissions()); 
        writeFullElement(xmlw, "restrctn", version.getRestrictions()); 
        writeFullElement(xmlw, "citeReq", version.getCitationRequirements()); 
        writeFullElement(xmlw, "deposReq", version.getDepositorRequirements()); 
        writeFullElement(xmlw, "dataAccs", version.getTermsOfAccess()); 
        writeFullElement(xmlw, "accsPlac", version.getDataAccessPlace()); 
        writeFullElement(xmlw, "conditions", version.getConditions()); 
        writeFullElement(xmlw, "disclaimer", version.getDisclaimer()); 
        writeFullElement(xmlw, "origArch", version.getOriginalArchive()); 
        writeFullElement(xmlw, "avlStatus", version.getAvailabilityStatus()); 
        writeFullElement(xmlw, "contact", version.getContactForAccess()); 
        writeFullElement(xmlw, "collSize", version.getSizeOfCollection()); 
        writeFullElement(xmlw, "complete", version.getStudyCompletion()); 
        
        
        xmlw.writeEndElement(); // stdyDscr

    }
    
    private static void writeDocDescElement (XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        
        xmlw.writeStartElement("docDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        xmlw.writeStartElement("IDNo");
        writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo      
        xmlw.writeEndElement(); // titlStmt
        xmlw.writeStartElement("distStmt");
        writeFullElement(xmlw, "distrbtr", datasetDto.getPublisher());
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
        writeAttribute(xmlw,"source","DVN"); 
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
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.country.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "nation", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.city.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "geogCover", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.state.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "geogCover", next.getSinglePrimitive());
                                } 
                                if (DatasetFieldConstant.otherGeographicCoverage.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "geogCover", next.getSinglePrimitive());
                                } 
                            }
                        }
                    }
                    if (DatasetFieldConstant.geographicBoundingBox.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
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
        writeFullElement(xmlw, "collMode", dto2Primitive(version, DatasetFieldConstant.collectionMode)); 
        writeFullElement(xmlw, "resInstru", dto2Primitive(version, DatasetFieldConstant.researchInstrument)); 
        writeFullElement(xmlw, "collSitu", dto2Primitive(version, DatasetFieldConstant.dataCollectionSituation)); 
        writeFullElement(xmlw, "actMin", dto2Primitive(version, DatasetFieldConstant.actionsToMinimizeLoss));
        writeFullElement(xmlw, "conOps", dto2Primitive(version, DatasetFieldConstant.controlOperations));  
        writeFullElement(xmlw, "weight", dto2Primitive(version, DatasetFieldConstant.weighting));  
        writeFullElement(xmlw, "cleanOps", dto2Primitive(version, DatasetFieldConstant.cleaningOperations));

        xmlw.writeEndElement(); //dataColl
        xmlw.writeStartElement("anlyInfo");
        writeFullElement(xmlw, "anylInfo", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        writeFullElement(xmlw, "respRate", dto2Primitive(version, DatasetFieldConstant.responseRate));  
        writeFullElement(xmlw, "estSmpErr", dto2Primitive(version, DatasetFieldConstant.samplingErrorEstimates));  
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
                                   writeAttribute(xmlw,"URI",keywordURI); 
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
                                   writeAttribute(xmlw,"URI",topicClassificationURI); 
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
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("rspStmt");
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
                        xmlw.writeEndElement(); //rspStmt
                    }
                }
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
                                    datasetContactEmail =  next.getSinglePrimitive();
                                }
                            }
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
                        xmlw.writeStartElement("distrbtr");
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
                        xmlw.writeEndElement(); //rspStmt
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
                            pubString = appendCommaSeparatedValue(citation, IDType);
                            pubString = appendCommaSeparatedValue(pubString, IDNo);
                            pubString = appendCommaSeparatedValue(pubString, url);
                            if (!pubString.isEmpty()){
                                xmlw.writeStartElement("relPubl"); 
                                xmlw.writeCharacters(pubString);
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

    /**
     * @todo Create a full dataDscr and otherMat sections of the DDI. This stub
     * adapted from the minimal DDIExportServiceBean example.
     */
    private static void createdataDscr(XMLStreamWriter xmlw, List<FileDTO> fileDtos) throws XMLStreamException {
        if (fileDtos.isEmpty()) {
            return;
        }
        xmlw.writeStartElement("dataDscr");
        xmlw.writeEndElement(); // dataDscr
        for (FileDTO fileDTo : fileDtos) {
            xmlw.writeStartElement("otherMat");
            writeAttribute(xmlw, "ID", "f" + fileDTo.getDatafile().getId());
            writeAttribute(xmlw, "level", "datafile");
            xmlw.writeStartElement("labl");
            xmlw.writeCharacters(fileDTo.getDatafile().getName());
            xmlw.writeEndElement(); // labl
            writeFileDescription(xmlw, fileDTo);
            xmlw.writeEndElement(); // otherMat
        }
    }

    private static void writeFileDescription(XMLStreamWriter xmlw, FileDTO fileDTo) throws XMLStreamException {
        xmlw.writeStartElement("txt");
        String description = fileDTo.getDatafile().getDescription();
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

}
