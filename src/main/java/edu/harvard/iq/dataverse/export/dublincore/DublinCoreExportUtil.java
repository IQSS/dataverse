/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.dublincore;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author skraffmi
 */
public class DublinCoreExportUtil {
 
        private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());
        
    public static OutputStream datasetJson2dublincore(JsonObject datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        System.out.print(datasetDtoAsJson.toString());
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        try {
            return dto2dublincore(datasetDto);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private static OutputStream dto2dublincore(DatasetDTO datasetDto) throws XMLStreamException {
        OutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("metadata");
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
        xmlw.writeAttribute("xmlns:dcterms", "http://purl.org/dc/terms/");
        /*
        <metadata
  xmlns="http://example.org/myapp/"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://example.org/myapp/ http://example.org/myapp/schema.xsd"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:dcterms="http://purl.org/dc/terms/">
        */
        
        xmlw.writeDefaultNamespace("http://dublincore.org/documents/dcmi-terms/"); //
        //writeAttribute(xmlw, "version", "2.0"); //??
        createStdyDscr(xmlw, datasetDto);
        //createdataDscr(xmlw, datasetDto.getDatasetVersion().getFiles()); No Files, Right?
        xmlw.writeEndElement(); // metadata
        xmlw.flush();
        return outputStream;
    }
    
    private static void createStdyDscr(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
  
        writeFullElement(xmlw, "dcterms:title", dto2Primitive(version, DatasetFieldConstant.title));                       
        
        xmlw.writeStartElement("dcterms:identifier");
        xmlw.writeCharacters(persistentAgency + ":" + persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // decterms:identifier
       

        writeAuthorsElement(xmlw, version);
        
        writeFullElement(xmlw, "dcterms:publisher", datasetDto.getPublisher());
        writeFullElement(xmlw, "dcterms:issued", datasetDto.getPublicationDate());
        
        writeFullElement(xmlw, "dcterms:modified", datasetDto.getDatasetVersion().getLastUpdateTime());
        writeAbstractElement(xmlw, version); // Description
        writeSubjectElement(xmlw, version); 
        
        writeRelPublElement(xmlw, version);
        writeFullElement(xmlw, "dcterms:date", dto2Primitive(version, DatasetFieldConstant.productionDate));  
        
        writeFullElement(xmlw, "dcterms:contributor", dto2Primitive(version, DatasetFieldConstant.depositor));  
        
        writeContributorElement(xmlw, version);
        writeFullElement(xmlw, "dcterms:dateSubmitted", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));  
        
        writeTimeElements(xmlw, version);
       // writeProducersElement(xmlw, version);
     /*   
        xmlw.writeStartElement("distStmt");
        writeFullElement(xmlw, "distrbtr", datasetDto.getPublisher());
        writeFullElement(xmlw, "distdate", datasetDto.getPublicationDate());
        xmlw.writeEndElement(); // diststmt
        

        //End Citation Block
        
        //Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");
        
        //writeSubjectElement(xmlw, version); //Subject and Keywords
        //
        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.notesText));
        
       // writeSummaryDescriptionElement(xmlw, version);
       // 
        writeFullElement(xmlw, "prodDate", dto2Primitive(version, DatasetFieldConstant.productionDate));    
        writeFullElement(xmlw, "prodPlac", dto2Primitive(version, DatasetFieldConstant.productionPlace));
  
       // writeGrantElement(xmlw, version);
        //writeOtherIdElement(xmlw, version);
        //xmlw.writeStartElement("distStmt"); //Also need Contact
        //writeDistributorsElement(xmlw, version);
        //writeContactsElement(xmlw, version);
        //writeFullElement(xmlw, "depositr", dto2Primitive(version, DatasetFieldConstant.depositor));    
        writeFullElement(xmlw, "depDate", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));  
        //xmlw.writeEndElement(); // distStmt
        
        //writeFullElementList(xmlw, "relMat", dto2PrimitiveList(version, DatasetFieldConstant.relatedMaterial));
        //writeFullElementList(xmlw, "relStdy", dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets));
       // writeFullElementList(xmlw, "othRefs", dto2PrimitiveList(version, DatasetFieldConstant.otherReferences));
       // writeFullElementList(xmlw, "dataKind", dto2PrimitiveList(version, DatasetFieldConstant.kindOfData));
       // writeSeriesElement(xmlw, version);
       // writeSoftwareElement(xmlw, version);
        writeFullElementList(xmlw, "dataSrc", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        writeFullElement(xmlw, "srcOrig", dto2Primitive(version, DatasetFieldConstant.originOfSources)); 
        writeFullElement(xmlw, "srcChar", dto2Primitive(version, DatasetFieldConstant.characteristicOfSources)); 
        writeFullElement(xmlw, "srcDocu", dto2Primitive(version, DatasetFieldConstant.accessToSources)); 
        xmlw.writeEndElement(); // stdyInfo
        // End Info Block
        
        //Social Science Metadata block
               
        //writeMethodElement(xmlw, version);
        
        //Terms of Use and Access
        writeFullElement(xmlw, "useStmt", version.getTermsOfUse()); 
        writeFullElement(xmlw, "confDec", version.getConfidentialityDeclaration()); 
        writeFullElement(xmlw, "specPerm", version.getSpecialPermissions()); 
        writeFullElement(xmlw, "restrctn", version.getRestrictions()); 
        writeFullElement(xmlw, "citeReq", version.getCitationRequirements()); 
        writeFullElement(xmlw, "deposReq", version.getDepositorRequirements()); 
        writeFullElement(xmlw, "conditions", version.getConditions()); 
        writeFullElement(xmlw, "disclaimer", version.getDisclaimer()); 
        writeFullElement(xmlw, "dataAccs", version.getTermsOfAccess()); 
        writeFullElement(xmlw, "accsPlac", version.getDataAccessPlace()); 
        writeFullElement(xmlw, "origArch", version.getOriginalArchive()); 
        writeFullElement(xmlw, "avlStatus", version.getAvailabilityStatus()); 
        writeFullElement(xmlw, "contact", version.getContactForAccess()); 
        writeFullElement(xmlw, "collSize", version.getSizeOfCollection()); 
        writeFullElement(xmlw, "complete", version.getStudyCompletion()); 
        
        
        xmlw.writeEndElement(); // stdyDscr
*/
    }
    
    private static void writeAuthorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        String authorName = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    authorName = next.getSinglePrimitive();
                                }
                            }
                            if (!authorName.isEmpty()) {
                                xmlw.writeStartElement("dcterms:creator");
                                xmlw.writeCharacters(authorName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        String descriptionText = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionText =  next.getSinglePrimitive();
                                }
                            }
                            if (!descriptionText.isEmpty()){
                                xmlw.writeStartElement("dcterms:description");  
                                xmlw.writeCharacters(descriptionText);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException{ 
        
        //Key Words and Subject
      
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())){
                        for ( String subject : fieldDTO.getMultipleVocab()){
                            xmlw.writeStartElement("dcterms:subject");
                            xmlw.writeCharacters(subject);
                            xmlw.writeEndElement(); //Keyword
                        }
                    }
                    
                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    keywordValue =  next.getSinglePrimitive();
                                }
                            }
                            if (!keywordValue.isEmpty()){
                                xmlw.writeStartElement("dcterms:subject"); 
                                xmlw.writeCharacters(keywordValue);
                                xmlw.writeEndElement(); //Keyword
                            }
                        }
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
                                xmlw.writeStartElement("dcterms:isReferencedBy"); 
                                xmlw.writeCharacters(pubString);
                                xmlw.writeEndElement(); //relPubl
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeContributorElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        String contributorName = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                    contributorName =  next.getSinglePrimitive();
                                }
                            }
                            if (!contributorName.isEmpty()){
                                xmlw.writeStartElement("dcterms:contributor");  
                                xmlw.writeCharacters(contributorName);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeTimeElements(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
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
                                writeFullElement(xmlw, "dcterms:temporal", dateValStart); 
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeFullElement(xmlw, "dcterms:temporal", dateValEnd); 
                            }
                        }
                    }
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
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
                               writeFullElement(xmlw, "dcterms:temporal", dateValStart); 
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeFullElement(xmlw, "dcterms:temporal", dateValEnd); 
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
    
}
