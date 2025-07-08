package edu.harvard.iq.dataverse.export.ddi;

import com.google.gson.Gson;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.LicenseDTO;


import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.LEVEL_FILE;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_UNF;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_FILEDESCRIPTION;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_UNF;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_FILEDESCRIPTION;
import edu.harvard.iq.dataverse.export.DDIExporter;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;


import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import edu.harvard.iq.dataverse.util.xml.XmlUtil;
import edu.harvard.iq.dataverse.util.xml.XmlWriterUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.apache.commons.lang3.StringUtils;

// For write operation
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());
    public static final String NOTE_TYPE_TERMS_OF_USE = "DVN:TOU";
    public static final String NOTE_TYPE_TERMS_OF_ACCESS = "DVN:TOA";
    public static final String NOTE_TYPE_DATA_ACCESS_PLACE = "DVN:DAP";


    public static final String LEVEL_DV = "dv";

    
    static SettingsServiceBean settingsService;
    
    public static final String NOTE_TYPE_CONTENTTYPE = "DATAVERSE:CONTENTTYPE";
    public static final String NOTE_SUBJECT_CONTENTTYPE = "Content/MIME Type";
    public static final String CITATION_BLOCK_NAME = "citation";

    //Some tests don't send real PIDs that can be parsed
    //Use constant empty PID in these cases
    private static final String EMPTY_PID = "null:nullnullnull";

    public static String datasetDtoAsJson2ddi(String datasetDtoAsJson) {
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
        XMLStreamWriter xmlw = null;
        try {
            xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
            xmlw.writeStartElement("codeBook");
            xmlw.writeDefaultNamespace("ddi:codebook:2_5");
            xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlw.writeAttribute("xsi:schemaLocation", DDIExporter.DEFAULT_XML_NAMESPACE + " " + DDIExporter.DEFAULT_XML_SCHEMALOCATION);
            xmlw.writeAttribute("version", DDIExporter.DEFAULT_XML_VERSION);
            if (DvObjectContainer.isMetadataLanguageSet(datasetDto.getMetadataLanguage())) {
                xmlw.writeAttribute("xml:lang", datasetDto.getMetadataLanguage());
            }
            createStdyDscr(xmlw, datasetDto);
            createOtherMats(xmlw, datasetDto.getDatasetVersion().getFiles());
            xmlw.writeEndElement(); // codeBook
            xmlw.flush();
        } finally {
            if (xmlw != null) {
                try {
                    xmlw.close();
                } catch (XMLStreamException e) {
                    // Log this exception, but don't rethrow as it's in finally block
                    logger.log(Level.WARNING, "Error closing XMLStreamWriter", e);
                }
            }
        }
    }

    
    // "full" ddi, with the the "<fileDscr>"  and "<dataDscr>/<var>" sections: 
    public static void datasetJson2ddi(JsonObject datasetDtoAsJson, JsonArray fileDetails, OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        
        XMLStreamWriter xmlw = null;
        try {
            xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);

            xmlw.writeStartElement("codeBook");
            xmlw.writeDefaultNamespace("ddi:codebook:2_5");
            xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlw.writeAttribute("xsi:schemaLocation", DDIExporter.DEFAULT_XML_NAMESPACE + " " + DDIExporter.DEFAULT_XML_SCHEMALOCATION);
            xmlw.writeAttribute("version", DDIExporter.DEFAULT_XML_VERSION);
            if (DvObjectContainer.isMetadataLanguageSet(datasetDto.getMetadataLanguage())) {
                xmlw.writeAttribute("xml:lang", datasetDto.getMetadataLanguage());
            }
            createStdyDscr(xmlw, datasetDto);
            createFileDscr(xmlw, fileDetails);
            createDataDscr(xmlw, fileDetails);
            createOtherMatsFromFileMetadatas(xmlw, fileDetails);
            xmlw.writeEndElement(); // codeBook
            xmlw.flush();
        } finally {
            if (xmlw != null) {
                try {
                    xmlw.close();
                } catch (XMLStreamException e) {
                    // Log this exception, but don't rethrow as it's in finally block
                    logger.log(Level.WARNING, "Error closing XMLStreamWriter", e);
                }
            }
        }
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

        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();

        GlobalId pid = PidUtil.parseAsGlobalID(persistentProtocol, persistentAuthority, persistentId);
        String pidUri, pidString;
        if(pid != null) {
            pidUri = pid.asURL();
            pidString = pid.asString();
        } else {
            pidUri = EMPTY_PID;
            pidString = EMPTY_PID;
        }
        // The "persistentAgency" tag is used for the "agency" attribute of the 
        // <IDNo> ddi section; back in the DVN3 days we used "handle" and "DOI" 
        // for the 2 supported protocols, respectively. For the sake of backward
        // compatibility, we should probably stick with these labels: (-- L.A. 4.5)
        if ("hdl".equals(persistentAgency)) { 
            persistentAgency = "handle";
        } else if ("doi".equals(persistentAgency)) {
            persistentAgency = "DOI";
        }
        
        //docDesc Block
        writeDocDescElement (xmlw, datasetDto);
        //stdyDesc Block
        xmlw.writeStartElement("stdyDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
       
        XmlWriterUtil.writeFullElement(xmlw, "titl", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.title), datasetDto.getMetadataLanguage());
        XmlWriterUtil.writeFullElement(xmlw, "subTitl", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.subTitle));
        FieldDTO altField = dto2FieldDTO( version, DatasetFieldConstant.alternativeTitle, "citation"  );
        if (altField != null) {
            writeMultipleElement(xmlw, "altTitl", altField, datasetDto.getMetadataLanguage());
        }
        
        xmlw.writeStartElement("IDNo");
        XmlWriterUtil.writeAttribute(xmlw, "agency", persistentAgency);
        
        
        xmlw.writeCharacters(pidString);
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
        
        boolean excludeRepository = settingsService.isTrueForKey(SettingsServiceBean.Key.ExportInstallationAsDistributorOnlyWhenNotSet, false);
        if (!StringUtils.isEmpty(datasetDto.getPublisher()) && !(excludeRepository && distributorSet)) {
            xmlw.writeStartElement("distrbtr");
            xmlw.writeAttribute("source", "archive");
            xmlw.writeCharacters(datasetDto.getPublisher());
            xmlw.writeEndElement(); //distrbtr
        }
        writeDistributorsElement(xmlw, version, datasetDto.getMetadataLanguage());
        writeContactsElement(xmlw, version);
        /* per SCHEMA, depositr comes before depDate! - L.A. */
        XmlWriterUtil.writeFullElement(xmlw, "depositr", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.depositor));
        /* ... and depDate comes before distDate - L.A. */
        XmlWriterUtil.writeFullElement(xmlw, "depDate", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));
        XmlWriterUtil.writeFullElement(xmlw, "distDate", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.distributionDate));

        xmlw.writeEndElement(); // diststmt

        writeSeriesElement(xmlw, version);
        xmlw.writeStartElement("holdings");
        XmlWriterUtil.writeAttribute(xmlw, "URI", pidUri);
        xmlw.writeEndElement(); //holdings
        
        xmlw.writeEndElement(); // citation
        //End Citation Block
        
        //Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");
        
        writeSubjectElement(xmlw, version, datasetDto.getMetadataLanguage()); //Subject and Keywords
        writeAbstractElement(xmlw, version, datasetDto.getMetadataLanguage()); // Description
        writeSummaryDescriptionElement(xmlw, version, datasetDto.getMetadataLanguage());
        XmlWriterUtil.writeFullElement(xmlw, "notes", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.notesText));
        ////////
        xmlw.writeEndElement(); // stdyInfo

        writeMethodElement(xmlw, version, datasetDto.getMetadataLanguage());
        writeDataAccess(xmlw , version);
        writeOtherStudyMaterial(xmlw , version);

        XmlWriterUtil.writeFullElement(xmlw, "notes", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        
        xmlw.writeEndElement(); // stdyDscr

    }

    private static void writeOtherStudyMaterial(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        List<String> relMaterials;
        List<String> relDatasets;
        List<String> relReferences;
        try {
            relMaterials = dto2PrimitiveList(version, DatasetFieldConstant.relatedMaterial);
            relDatasets = dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets);
            relReferences = dto2PrimitiveList(version, DatasetFieldConstant.otherReferences); 
        } catch (Exception e) {
            logger.warning("Exporting dataset to DDI failed for related materials element: " + e.getMessage());
            return;
        }
        xmlw.writeStartElement("othrStdyMat");
        XmlWriterUtil.writeFullElementList(xmlw, "relMat", relMaterials);
        XmlWriterUtil.writeFullElementList(xmlw, "relStdy", relDatasets);
        writeRelPublElement(xmlw, version);
        XmlWriterUtil.writeFullElementList(xmlw, "othRefs", relReferences);
        xmlw.writeEndElement(); //othrStdyMat
    }

    /*
            <xs:sequence>
               <xs:element ref="setAvail" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="useStmt" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="notes" minOccurs="0" maxOccurs="unbounded"/>
            </xs:sequence>
    */
    private static void writeDataAccess(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("dataAccs");
        
        xmlw.writeStartElement("setAvail");
        XmlWriterUtil.writeFullElement(xmlw, "accsPlac", version.getDataAccessPlace());
        XmlWriterUtil.writeFullElement(xmlw, "origArch", version.getOriginalArchive());
        XmlWriterUtil.writeFullElement(xmlw, "avlStatus", version.getAvailabilityStatus());
        XmlWriterUtil.writeFullElement(xmlw, "collSize", version.getSizeOfCollection());
        XmlWriterUtil.writeFullElement(xmlw, "complete", version.getStudyCompletion());
        xmlw.writeEndElement(); //setAvail
        
        xmlw.writeStartElement("useStmt");
        XmlWriterUtil.writeFullElement(xmlw, "confDec", version.getConfidentialityDeclaration());
        XmlWriterUtil.writeFullElement(xmlw, "specPerm", version.getSpecialPermissions());
        XmlWriterUtil.writeFullElement(xmlw, "restrctn", version.getRestrictions());
        XmlWriterUtil.writeFullElement(xmlw, "contact", version.getContactForAccess());
        XmlWriterUtil.writeFullElement(xmlw, "citReq", version.getCitationRequirements());
        XmlWriterUtil.writeFullElement(xmlw, "deposReq", version.getDepositorRequirements());
        XmlWriterUtil.writeFullElement(xmlw, "conditions", version.getConditions());
        XmlWriterUtil.writeFullElement(xmlw, "disclaimer", version.getDisclaimer());
        xmlw.writeEndElement(); //useStmt

        /* any <note>s: */
        if (version.getTermsOfUse() != null && !version.getTermsOfUse().trim().equals("")) {
            xmlw.writeStartElement("notes");
            xmlw.writeAttribute("type", NOTE_TYPE_TERMS_OF_USE);
            xmlw.writeAttribute("level", LEVEL_DV);
            xmlw.writeCharacters(version.getTermsOfUse());
            xmlw.writeEndElement(); //notes
        }

        if (version.getTermsOfAccess() != null && !version.getTermsOfAccess().trim().equals("")) {
            xmlw.writeStartElement("notes");
            xmlw.writeAttribute("type", NOTE_TYPE_TERMS_OF_ACCESS);
            xmlw.writeAttribute("level", LEVEL_DV);
            xmlw.writeCharacters(version.getTermsOfAccess());
            xmlw.writeEndElement(); //notes
        }

        LicenseDTO license = version.getLicense();
        if (license != null) {
            String name = license.getName();
            String uri = license.getUri();
            if ((name != null && !name.trim().equals("")) && (uri != null && !uri.trim().equals(""))) {
                xmlw.writeStartElement("notes");
                xmlw.writeAttribute("type", NOTE_TYPE_TERMS_OF_USE);
                xmlw.writeAttribute("level", LEVEL_DV);
                xmlw.writeCharacters("<a href=" + '"' + uri + '"' + ">" + name + "</a>");
                xmlw.writeEndElement(); //notes
            }
        }
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
        GlobalId pid = PidUtil.parseAsGlobalID(persistentProtocol, persistentAuthority, persistentId);
        String pidString;
        if(pid != null) {
            pidString = pid.asString();
        } else {
            pidString = EMPTY_PID;
        }

        xmlw.writeStartElement("docDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
        XmlWriterUtil.writeFullElement(xmlw, "titl", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.title), datasetDto.getMetadataLanguage());
        xmlw.writeStartElement("IDNo");
        XmlWriterUtil.writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(pidString);
        xmlw.writeEndElement(); // IDNo
        xmlw.writeEndElement(); // titlStmt
        xmlw.writeStartElement("distStmt");
        //The doc is always published by the Dataverse Repository
        if (!StringUtils.isEmpty(datasetDto.getPublisher())) {
            xmlw.writeStartElement("distrbtr");
            xmlw.writeAttribute("source", "archive");
            xmlw.writeCharacters(datasetDto.getPublisher());
            xmlw.writeEndElement(); // distrbtr
        }
        XmlWriterUtil.writeFullElement(xmlw, "distDate", datasetDto.getPublicationDate());
        
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
        xmlw.writeAttribute("source","archive");
        xmlw.writeStartElement("version");
        if (datasetVersionDTO.getReleaseTime() != null) {
            XmlWriterUtil.writeAttribute(xmlw, "date", datasetVersionDTO.getReleaseTime().substring(0, 10));
        }
        XmlWriterUtil.writeAttribute(xmlw, "type", datasetVersionDTO.getVersionState().toString());
        if (datasetVersionDTO.getVersionNumber() != null) {
            xmlw.writeCharacters(datasetVersionDTO.getVersionNumber().toString());
        }
        xmlw.writeEndElement(); // version
        if (!StringUtils.isBlank(datasetVersionDTO.getVersionNote())) {
            xmlw.writeStartElement("notes");
            xmlw.writeCharacters(datasetVersionDTO.getVersionNote());
            xmlw.writeEndElement(); // notes
        }
        
        xmlw.writeEndElement(); // verStmt
    }
    
    /* From the DDI 2.5 schema: 
            <xs:sequence>
               <xs:element ref="timePrd" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="collDate" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="nation" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="geogCover" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="geogUnit" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="geoBndBox" minOccurs="0"/>
               <xs:element ref="boundPoly" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="anlyUnit" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="universe" minOccurs="0" maxOccurs="unbounded"/>
               <xs:element ref="dataKind" minOccurs="0" maxOccurs="unbounded"/>
            </xs:sequence>
    */
    private static void writeSummaryDescriptionElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String lang) throws XMLStreamException {
        xmlw.writeStartElement("sumDscr");
        FieldDTO timePeriodCoveredDTO = null;
        FieldDTO dateOfCollectionDTO = null;
        FieldDTO geographicCoverageDTO = null;
        FieldDTO geographicBoundingBoxDTO = null;
        FieldDTO unitOfAnalysisDTO = null;
        FieldDTO universeDTO = null;
        FieldDTO kindOfDataDTO = null;

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();

            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        timePeriodCoveredDTO = fieldDTO;
                    }

                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        dateOfCollectionDTO = fieldDTO;
                    }

                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())) {
                        kindOfDataDTO = fieldDTO;
                    }
                }
            }

            if ("geospatial".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.geographicCoverage.equals(fieldDTO.getTypeName())) {
                        geographicCoverageDTO = fieldDTO;
                    }
                    if (DatasetFieldConstant.geographicBoundingBox.equals(fieldDTO.getTypeName())) {

                        geographicBoundingBoxDTO = fieldDTO;

                    }
                }
            }

            if ("socialscience".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.universe.equals(fieldDTO.getTypeName())) {
                        universeDTO = fieldDTO;
                    }
                    if (DatasetFieldConstant.unitOfAnalysis.equals(fieldDTO.getTypeName())) {
                        unitOfAnalysisDTO = fieldDTO;
                    }
                }
            }
        }
        /* Finally, we can write the fields we have collected, in the correct order: -L.A.*/

        if (timePeriodCoveredDTO != null) {
            String dateValStart = "";
            String dateValEnd = "";
            Integer per = 0;
            for (HashSet<FieldDTO> foo : timePeriodCoveredDTO.getMultipleCompound()) {
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
                    writeDateElement(xmlw, "timePrd", "P" + per.toString(), "start", dateValStart);
                }
                if (!dateValEnd.isEmpty()) {
                    writeDateElement(xmlw, "timePrd", "P" + per.toString(), "end", dateValEnd);
                }
            }
        }

        if (dateOfCollectionDTO != null) {
            String dateValStart = "";
            String dateValEnd = "";
            Integer coll = 0;
            for (HashSet<FieldDTO> foo : dateOfCollectionDTO.getMultipleCompound()) {
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
                    writeDateElement(xmlw, "collDate", "P" + coll.toString(), "start", dateValStart);
                }
                if (!dateValEnd.isEmpty()) {
                    writeDateElement(xmlw, "collDate", "P" + coll.toString(), "end", dateValEnd);
                }
            }
        }

        /* <nation> and <geogCover> come next, in that order. -L.A. */
        if (geographicCoverageDTO != null) {

            List<String> nationList = new ArrayList<>();
            List<String> geogCoverList = new ArrayList<>();

            for (HashSet<FieldDTO> foo : geographicCoverageDTO.getMultipleCompound()) {
                for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                    FieldDTO next = iterator.next();
                    /* our "country" field maps 1:1 to the DDI "<nation>": */
                    if (DatasetFieldConstant.country.equals(next.getTypeName())) {
                        nationList.add(next.getSinglePrimitive());
                    }
                    /* city, state and otherGeographicCoverage all exported as "<geogCover>": */
                    if (DatasetFieldConstant.city.equals(next.getTypeName())
                            || DatasetFieldConstant.state.equals(next.getTypeName())
                            || DatasetFieldConstant.otherGeographicCoverage.equals(next.getTypeName())) {
                        geogCoverList.add(next.getSinglePrimitive());
                    }
                }
            }

            /**
             * And now we can write all the fields encountered, first the
             * "<nation>" entries, then all the "<geogCover>" ones:
             */
            for (String nationEntry : nationList) {
                XmlWriterUtil.writeFullElement(xmlw, "nation", nationEntry);
            }
            for (String geogCoverEntry : geogCoverList) {
                XmlWriterUtil.writeFullElement(xmlw, "geogCover", geogCoverEntry);
            }
        }

        XmlWriterUtil.writeFullElementList(xmlw, "geogUnit", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.geographicUnit));

        /* Only 1 geoBndBox is allowed in the DDI.
           So, I'm just going to arbitrarily use the first one, and ignore the rest! -L.A. */
        if (geographicBoundingBoxDTO != null) {
            HashSet<FieldDTO> bndBoxSet = geographicBoundingBoxDTO.getMultipleCompound().get(0);
            xmlw.writeStartElement("geoBndBox");
            HashMap<String, String> geoBndBoxMap = new HashMap<>();
            for (FieldDTO next : bndBoxSet) {
                if (DatasetFieldConstant.westLongitude.equals(next.getTypeName())) {
                    geoBndBoxMap.put("westBL", next.getSinglePrimitive());
                }
                if (DatasetFieldConstant.eastLongitude.equals(next.getTypeName())) {
                    geoBndBoxMap.put("eastBL", next.getSinglePrimitive());
                }
                if (DatasetFieldConstant.northLatitude.equals(next.getTypeName())) {
                    geoBndBoxMap.put("northBL", next.getSinglePrimitive());
                }
                if (DatasetFieldConstant.southLatitude.equals(next.getTypeName())) {
                    geoBndBoxMap.put("southBL", next.getSinglePrimitive());
                }
            }

            /* Once again, order is important! */
 /*
                        <xs:sequence>
                            <xs:element ref="westBL"/>
                            <xs:element ref="eastBL"/>
                            <xs:element ref="southBL"/>
                            <xs:element ref="northBL"/>
                        </xs:sequence>
             */
            if (geoBndBoxMap.get("westBL") != null) {
                XmlWriterUtil.writeFullElement(xmlw, "westBL", geoBndBoxMap.get("westBL"));
            }
            if (geoBndBoxMap.get("eastBL") != null) {
                XmlWriterUtil.writeFullElement(xmlw, "eastBL", geoBndBoxMap.get("eastBL"));
            }
            if (geoBndBoxMap.get("southBL") != null) {
                XmlWriterUtil.writeFullElement(xmlw, "southBL", geoBndBoxMap.get("southBL"));
            }
            if (geoBndBoxMap.get("northBL") != null) {
                XmlWriterUtil.writeFullElement(xmlw, "northBL", geoBndBoxMap.get("northBL"));
            }

            xmlw.writeEndElement();
        }

        /* analyUnit: */
        if (unitOfAnalysisDTO != null) {
            XmlWriterUtil.writeI18NElementList(xmlw, "anlyUnit", unitOfAnalysisDTO.getMultipleVocab(), "unitOfAnalysis", unitOfAnalysisDTO.getTypeClass(), "socialscience", lang);

        }

        /* universe: */
        if (universeDTO != null) {
            writeMultipleElement(xmlw, "universe", universeDTO, lang);
        }

        /* finally, any "kind of data" entries: */
        if (kindOfDataDTO != null) {
            writeMultipleElement(xmlw, "dataKind", kindOfDataDTO, lang);
        }

        xmlw.writeEndElement(); //sumDscr     
    }
    
    private static void writeMultipleElement(XMLStreamWriter xmlw, String element, FieldDTO fieldDTO, String lang) throws XMLStreamException {
        for (String value : fieldDTO.getMultiplePrimitive()) {
            //Write multiple lang vals for controlled vocab, otherwise don't include any lang tag
            XmlWriterUtil.writeFullElement(xmlw, element, value, fieldDTO.isControlledVocabularyField() ? lang : null);
        }
    }
    
    private static void writeDateElement(XMLStreamWriter xmlw, String element, String cycle, String event, String dateIn) throws XMLStreamException {

        xmlw.writeStartElement(element);
        XmlWriterUtil.writeAttribute(xmlw, "cycle",  cycle);
        XmlWriterUtil.writeAttribute(xmlw, "event", event);
        XmlWriterUtil.writeAttribute(xmlw, "date", dateIn);
        xmlw.writeCharacters(dateIn);
        xmlw.writeEndElement(); 

    }
    
    /**
     * Again, <dataColl> is an xs:sequence - order is important and must follow
     * the schema. -L.A.
     * <xs:sequence>
     * <xs:element ref="timeMeth" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="dataCollector" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="collectorTraining" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="frequenc" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="sampProc" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="sampleFrame" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="targetSampleSize" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="deviat" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="collMode" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="resInstru" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="instrumentDevelopment" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="sources" minOccurs="0"/>
     * <xs:element ref="collSitu" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="actMin" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="ConOps" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="weight" minOccurs="0" maxOccurs="unbounded"/>
     * <xs:element ref="cleanOps" minOccurs="0" maxOccurs="unbounded"/>
     * </xs:sequence>
     */
    private static void writeMethodElement(XMLStreamWriter xmlw , DatasetVersionDTO version, String lang) throws XMLStreamException{
        xmlw.writeStartElement("method");
        xmlw.writeStartElement("dataColl");
        XmlWriterUtil.writeI18NElement(xmlw, "timeMeth", version, DatasetFieldConstant.timeMethod,lang);
        XmlWriterUtil.writeI18NElement(xmlw, "dataCollector", version, DatasetFieldConstant.dataCollector, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "collectorTraining", version, DatasetFieldConstant.collectorTraining, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "frequenc", version, DatasetFieldConstant.frequencyOfDataCollection, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "sampProc", version, DatasetFieldConstant.samplingProcedure, lang);

        writeTargetSampleElement(xmlw, version);

        XmlWriterUtil.writeI18NElement(xmlw, "deviat", version, DatasetFieldConstant.deviationsFromSampleDesign, lang);

        /* <collMode> comes before <sources>: */
        FieldDTO collModeFieldDTO = dto2FieldDTO(version, DatasetFieldConstant.collectionMode, "socialscience");
        if (collModeFieldDTO != null) {
            // This field was made multiple as of 5.10
            // Below is a backward compatibility check allowing export to work in 
            // an instance where the metadata block has not been updated yet.
            if (collModeFieldDTO.getMultiple()) {
                XmlWriterUtil.writeI18NElementList(xmlw, "collMode", collModeFieldDTO.getMultipleVocab(), DatasetFieldConstant.collectionMode, collModeFieldDTO.getTypeClass(), "socialscience", lang);
            } else {
                XmlWriterUtil.writeI18NElement(xmlw, "collMode", version, DatasetFieldConstant.collectionMode, lang);
            }
        }
        /* and so does <resInstru>: */
        XmlWriterUtil.writeI18NElement(xmlw, "resInstru", version, DatasetFieldConstant.researchInstrument, lang);
        xmlw.writeStartElement("sources");
        XmlWriterUtil.writeFullElementList(xmlw, "dataSrc", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        XmlWriterUtil.writeI18NElement(xmlw, "srcOrig", version, DatasetFieldConstant.originOfSources, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "srcChar", version, DatasetFieldConstant.characteristicOfSources, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "srcDocu", version, DatasetFieldConstant.accessToSources, lang);
        xmlw.writeEndElement(); //sources

        
        XmlWriterUtil.writeI18NElement(xmlw, "collSitu", version, DatasetFieldConstant.dataCollectionSituation, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "actMin", version, DatasetFieldConstant.actionsToMinimizeLoss, lang);
        /* "<ConOps>" has the uppercase C: */
        XmlWriterUtil.writeI18NElement(xmlw, "ConOps", version, DatasetFieldConstant.controlOperations, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "weight", version, DatasetFieldConstant.weighting, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "cleanOps", version, DatasetFieldConstant.cleaningOperations, lang);

        xmlw.writeEndElement(); //dataColl
        /* <notes> before <anlyInfo>: */
        writeNotesElement(xmlw, version);

        xmlw.writeStartElement("anlyInfo");
        //XmlWriterUtil.writeFullElement(xmlw, "anylInfo", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        XmlWriterUtil.writeI18NElement(xmlw, "respRate", version, DatasetFieldConstant.responseRate, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "EstSmpErr", version, DatasetFieldConstant.samplingErrorEstimates, lang);
        XmlWriterUtil.writeI18NElement(xmlw, "dataAppr", version, DatasetFieldConstant.otherDataAppraisal, lang);
        xmlw.writeEndElement(); //anlyInfo
        
        xmlw.writeEndElement();//method
    }
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String lang) throws XMLStreamException{ 
        
        //Key Words and Topic Classification
        Locale defaultLocale = Locale.getDefault();
        xmlw.writeStartElement("subject");
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if (CITATION_BLOCK_NAME.equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())) {
                        XmlWriterUtil.writeI18NElementList(xmlw, "keyword", fieldDTO.getMultipleVocab(), "subject",
                                fieldDTO.getTypeClass(), "citation", lang);
                    }

                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        boolean isCVV = false;
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            String keywordVocab = "";
                            String keywordURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    if (next.isControlledVocabularyField()) {
                                        isCVV = true;
                                    }
                                    keywordValue = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocab.equals(next.getTypeName())) {
                                    keywordVocab = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocabURI.equals(next.getTypeName())) {
                                    keywordURI = next.getSinglePrimitive();
                                }
                            }
                            if (!keywordValue.isEmpty()) {
                                xmlw.writeStartElement("keyword");
                                XmlWriterUtil.writeAttribute(xmlw, "vocab", keywordVocab);
                                XmlWriterUtil.writeAttribute(xmlw, "vocabURI", keywordURI);
                                if (lang != null && isCVV) {
                                    XmlWriterUtil.writeAttribute(xmlw, "xml:lang", defaultLocale.getLanguage());
                                    xmlw.writeCharacters(ControlledVocabularyValue.getLocaleStrValue(keywordValue,
                                            DatasetFieldConstant.keywordValue, CITATION_BLOCK_NAME, defaultLocale,
                                            true));
                                } else {
                                    xmlw.writeCharacters(keywordValue);
                                }
                                xmlw.writeEndElement(); // Keyword
                                if (lang != null && isCVV && !defaultLocale.getLanguage().equals(lang)) {
                                    String translatedValue = ControlledVocabularyValue.getLocaleStrValue(keywordValue,
                                            DatasetFieldConstant.keywordValue, CITATION_BLOCK_NAME, new Locale(lang),
                                            false);
                                    if (translatedValue != null) {
                                        xmlw.writeStartElement("keyword");
                                        XmlWriterUtil.writeAttribute(xmlw, "vocab", keywordVocab);
                                        XmlWriterUtil.writeAttribute(xmlw, "vocabURI", keywordURI);
                                        XmlWriterUtil.writeAttribute(xmlw, "xml:lang", lang);
                                        xmlw.writeCharacters(translatedValue);
                                        xmlw.writeEndElement(); // Keyword
                                    }
                                }
                            }
                        }
                    }
                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        boolean isCVV = false;
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String topicClassificationValue = "";
                            String topicClassificationVocab = "";
                            String topicClassificationURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                    // Currently getSingleVocab() is the same as getSinglePrimitive() so this works
                                    // for either case
                                    topicClassificationValue = next.getSinglePrimitive();
                                    if (next.isControlledVocabularyField()) {
                                        isCVV = true;
                                    }
                                }
                                if (DatasetFieldConstant.topicClassVocab.equals(next.getTypeName())) {
                                    topicClassificationVocab = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.topicClassVocabURI.equals(next.getTypeName())) {
                                    topicClassificationURI = next.getSinglePrimitive();
                                }
                            }
                            if (!topicClassificationValue.isEmpty()) {
                                xmlw.writeStartElement("topcClas");
                                XmlWriterUtil.writeAttribute(xmlw, "vocab", topicClassificationVocab);
                                XmlWriterUtil.writeAttribute(xmlw, "vocabURI", topicClassificationURI);
                                if (lang != null && isCVV) {
                                    XmlWriterUtil.writeAttribute(xmlw, "xml:lang", defaultLocale.getLanguage());
                                    xmlw.writeCharacters(ControlledVocabularyValue.getLocaleStrValue(
                                            topicClassificationValue, DatasetFieldConstant.topicClassValue,
                                            CITATION_BLOCK_NAME, defaultLocale, true));
                                } else {
                                    xmlw.writeCharacters(topicClassificationValue);
                                }
                                xmlw.writeEndElement(); // topcClas
                                if (lang != null && isCVV && !defaultLocale.getLanguage().equals(lang)) {
                                    String translatedValue = ControlledVocabularyValue.getLocaleStrValue(
                                            topicClassificationValue, DatasetFieldConstant.topicClassValue,
                                            CITATION_BLOCK_NAME, new Locale(lang), false);
                                    if (translatedValue != null) {
                                        xmlw.writeStartElement("topcClas");
                                        XmlWriterUtil.writeAttribute(xmlw, "vocab", topicClassificationVocab);
                                        XmlWriterUtil.writeAttribute(xmlw, "vocabURI", topicClassificationURI);
                                        XmlWriterUtil.writeAttribute(xmlw, "xml:lang", lang);
                                        xmlw.writeCharacters(translatedValue);
                                        xmlw.writeEndElement(); // topcClas
                                    }
                                }
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
                                XmlWriterUtil.writeAttribute(xmlw,"affiliation",authorAffiliation);
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
                                XmlWriterUtil.writeAttribute(xmlw,"role", contributorType);
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
                                XmlWriterUtil.writeAttribute(xmlw,"affiliation",datasetContactAffiliation);
                                XmlWriterUtil.writeAttribute(xmlw,"email",datasetContactEmail);
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
                            }
                            if (!producerName.isEmpty()) {
                                xmlw.writeStartElement("producer");
                                XmlWriterUtil.writeAttribute(xmlw, "affiliation", producerAffiliation);
                                XmlWriterUtil.writeAttribute(xmlw, "abbr", producerAbbreviation);
                                //XmlWriterUtil.writeAttribute(xmlw, "role", producerLogo);
                                xmlw.writeCharacters(producerName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                        
                    }
                }
            }
        }
        XmlWriterUtil.writeFullElement(xmlw, "prodDate", XmlWriterUtil.dto2Primitive(version, DatasetFieldConstant.productionDate));
        // productionPlace was made multiple as of 5.14:
        // (a quick backward compatibility check was added to dto2PrimitiveList(),
        // see the method for details)

        FieldDTO  prodPlac = dto2FieldDTO( version, DatasetFieldConstant.productionPlace, "citation"  );
        if (prodPlac != null) {
            writeMultipleElement(xmlw, "prodPlac", prodPlac, null);
        }
        writeSoftwareElement(xmlw, version);
  
        writeGrantElement(xmlw, version);
        xmlw.writeEndElement(); //prodStmt
    }
    
    private static void writeDistributorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String lang) throws XMLStreamException {
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
                            }
                            if (!distributorName.isEmpty()) {
                                xmlw.writeStartElement("distrbtr");
                                if(DvObjectContainer.isMetadataLanguageSet(lang)) {
                                    xmlw.writeAttribute("xml:lang", lang);
                                }
                                XmlWriterUtil.writeAttribute(xmlw, "affiliation", distributorAffiliation);
                                XmlWriterUtil.writeAttribute(xmlw, "abbr", distributorAbbreviation);
                                XmlWriterUtil.writeAttribute(xmlw, "URI", distributorURL);
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
                                /* <xs:sequence>
                                    <xs:element ref="titlStmt"/>
                                    <xs:element ref="rspStmt" minOccurs="0"/>
                                    <xs:element ref="prodStmt" minOccurs="0"/>
                                    <xs:element ref="distStmt" minOccurs="0"/>
                                    <xs:element ref="serStmt" minOccurs="0" maxOccurs="unbounded"/>
                                    <xs:element ref="verStmt" minOccurs="0" maxOccurs="unbounded"/>
                                    <xs:element ref="biblCit" minOccurs="0" maxOccurs="unbounded"/>
                                    <xs:element ref="holdings" minOccurs="0" maxOccurs="unbounded"/>
                                    <xs:element ref="notes" minOccurs="0" maxOccurs="unbounded"/>
                                    <xs:group ref="dc:elementsAndRefinementsGroup"/>
                                   </xs:sequence>
                                 (In other words - titlStmt is mandatory! -L.A.)
                                */
                                xmlw.writeStartElement("titlStmt");
                                XmlWriterUtil.writeFullElement(xmlw, "titl", citation);
                                if (IDNo != null && !IDNo.trim().equals("")) {

                                    xmlw.writeStartElement("IDNo");
                                    if (IDType != null && !IDType.trim().equals("")) {
                                        xmlw.writeAttribute("agency", IDType);
                                    }
                                    xmlw.writeCharacters(IDNo);
                                    xmlw.writeEndElement(); //IDNo
                                }
                                xmlw.writeEndElement(); // titlStmt


                                XmlWriterUtil.writeFullElement(xmlw,"biblCit",citation);
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
    
    private static void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String lang) throws XMLStreamException {
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
                                XmlWriterUtil.writeAttribute(xmlw,"date",descriptionDate);
                                if(DvObjectContainer.isMetadataLanguageSet(lang)) {
                                    xmlw.writeAttribute("xml:lang", lang);
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
                                XmlWriterUtil.writeAttribute(xmlw,"agency",grantAgency);
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
                                XmlWriterUtil.writeAttribute(xmlw,"agency",otherIdAgency);
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
                                XmlWriterUtil.writeAttribute(xmlw,"version",softwareVersion);
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
                        String seriesName = "";
                        String seriesInformation = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            xmlw.writeStartElement("serStmt");
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.seriesName.equals(next.getTypeName())) {
                                    seriesName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.seriesInformation.equals(next.getTypeName())) {
                                    seriesInformation = next.getSinglePrimitive();
                                }
                            }
                            if (!seriesName.isEmpty()) {
                                xmlw.writeStartElement("serName");
                                xmlw.writeCharacters(seriesName);
                                xmlw.writeEndElement(); //serName
                            }
                            if (!seriesInformation.isEmpty()) {
                                xmlw.writeStartElement("serInfo");
                                xmlw.writeCharacters(seriesInformation);
                                xmlw.writeEndElement(); //serInfo
                            }
                            xmlw.writeEndElement(); //serStmt
                        }
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
                        /* <sampleSize> must come before <sampleSizeFormula>! -L.A. */
                        if (!actualSize.isEmpty()) {
                            xmlw.writeStartElement("sampleSize");
                            xmlw.writeCharacters(actualSize);
                            xmlw.writeEndElement(); //sampleSize
                        }
                        if (!sizeFormula.isEmpty()) {
                            xmlw.writeStartElement("sampleSizeFormula");
                            xmlw.writeCharacters(sizeFormula);
                            xmlw.writeEndElement(); //sampleSizeFormula
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
                            XmlWriterUtil.writeAttribute(xmlw,"type",notesType);
                            XmlWriterUtil.writeAttribute(xmlw,"subject",notesSubject);
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
        String dataverseUrl = SystemConfig.getDataverseSiteUrlStatic();
        
        for (FileDTO fileDTo : fileDtos) {
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (fileDTo.getDataFile().getDataTables() == null || fileDTo.getDataFile().getDataTables().isEmpty()) {
                xmlw.writeStartElement("otherMat");
                XmlWriterUtil.writeAttribute(xmlw, "ID", "f" + fileDTo.getDataFile().getId());
                String pidURL = fileDTo.getDataFile().getPidURL();
                if (pidURL != null && !pidURL.isEmpty()){
                    xmlw.writeAttribute("URI", pidURL);
                } else {
                    xmlw.writeAttribute("URI", dataverseUrl + "/api/access/datafile/" + fileDTo.getDataFile().getId());
                }
                xmlw.writeAttribute("level", "datafile");
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
                    xmlw.writeAttribute("level", LEVEL_FILE);
                    xmlw.writeAttribute("type", NOTE_TYPE_CONTENTTYPE);
                    xmlw.writeAttribute("subject", NOTE_SUBJECT_CONTENTTYPE);
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
    
    private static void createOtherMatsFromFileMetadatas(XMLStreamWriter xmlw, JsonArray fileDetails) throws XMLStreamException {
        // The preferred URL for this dataverse, for cooking up the file access API links:
        String dataverseUrl = SystemConfig.getDataverseSiteUrlStatic();
        
        for (int i=0;i<fileDetails.size();i++) {
            JsonObject fileJson = fileDetails.getJsonObject(i);
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (!fileJson.containsKey("dataTables")) {
                xmlw.writeStartElement("otherMat");
                xmlw.writeAttribute("ID", "f" + fileJson.getJsonNumber(("id").toString()));
                if (fileJson.containsKey("pidUrl")){
                    XmlWriterUtil.writeAttribute(xmlw, "URI",  fileJson.getString("pidUrl"));
                }  else {
                    xmlw.writeAttribute("URI", dataverseUrl + "/api/access/datafile/" + fileJson.getJsonNumber("id").toString());
                }

                xmlw.writeAttribute("level", "datafile");
                xmlw.writeStartElement("labl");
                xmlw.writeCharacters(fileJson.getString("filename"));
                xmlw.writeEndElement(); // labl
                
                if (fileJson.containsKey("description")) {
                    xmlw.writeStartElement("txt");
                    xmlw.writeCharacters(fileJson.getString("description"));
                    xmlw.writeEndElement(); // txt
                }
                // there's no readily available field in the othermat section 
                // for the content type (aka mime type); so we'll store it in this
                // specially formatted notes section:
                if (fileJson.containsKey("contentType")) {
                    xmlw.writeStartElement("notes");
                    xmlw.writeAttribute("level", LEVEL_FILE);
                    xmlw.writeAttribute("type", NOTE_TYPE_CONTENTTYPE);
                    xmlw.writeAttribute("subject", NOTE_SUBJECT_CONTENTTYPE);
                    xmlw.writeCharacters(fileJson.getString("contentType"));
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
    

    
    private static List<String> dto2PrimitiveList(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    // This hack is here to make sure the export does not blow 
                    // up on an instance that upgraded to a Dataverse version
                    // where a certain primitive has been made multiple, but has
                    // not yet update the block. 
                    if (fieldDTO.getMultiple() != null && fieldDTO.getMultiple()) {
                        return fieldDTO.getMultiplePrimitive();
                    } else {
                        return Arrays.asList(fieldDTO.getSinglePrimitive());
                    }
                }
            }
        }
        return null;
    }
    
    private static FieldDTO dto2FieldDTO(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName, String metadataBlockName) {
        MetadataBlockDTO block = datasetVersionDTO.getMetadataBlocks().get(metadataBlockName);
        if (block != null) {
            for (FieldDTO fieldDTO : block.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO;
                }
            }
        }
        return null;
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
    
    public static void createDataDscr(XMLStreamWriter xmlw, JsonArray fileDetails) throws XMLStreamException {

        if (fileDetails.isEmpty()) {
            return;
        }

        boolean tabularData = false;

        // we're not writing the opening <dataDscr> tag until we find an actual 
        // tabular datafile.
        for (int i=0;i<fileDetails.size();i++) {
            JsonObject fileJson = fileDetails.getJsonObject(i);

            /**
             * Previously (in Dataverse 5.3 and below) the dataDscr section was
             * included for restricted files but that meant that summary
             * statistics were exposed. (To get at these statistics, API users
             * should instead use the "Data Variable Metadata Access" endpoint.)
             * These days we skip restricted files to avoid this exposure.
             */
            if (fileJson.containsKey("restricted") && fileJson.getBoolean("restricted")) {
                continue;
            }
            if(fileJson.containsKey("embargo")) {
             String dateString = fileJson.getJsonObject("embargo").getString("dateAvailable");
             LocalDate endDate = LocalDate.parse(dateString);
             if (endDate != null && endDate.isAfter(LocalDate.now())) {
                 //Embargo is active so skip
                 continue;
             }
            }
        
            if (fileJson.containsKey("dataTables")) {
                if (!tabularData) {
                    xmlw.writeStartElement("dataDscr");
                    tabularData = true;
                }
                if(fileJson.containsKey("varGroups")) {
                    JsonArray varGroups = fileJson.getJsonArray("varGroups");
                    for (int j=0;j<varGroups.size();j++){
                        createVarGroupDDI(xmlw, varGroups.getJsonObject(j));
                    }
                }
                JsonObject dataTable = fileJson.getJsonArray("dataTables").getJsonObject(0);
                JsonArray vars = dataTable.getJsonArray("dataVariables");
                if (vars != null) {
                    for (int j = 0; j < vars.size(); j++) {
                        createVarDDI(xmlw, vars.getJsonObject(j), fileJson.getJsonNumber("id").toString(),
                                fileJson.getJsonNumber("fileMetadataId").toString());
                    }
                }
            }
        }

        if (tabularData) {
            xmlw.writeEndElement(); // dataDscr
        }
    }
    private static void createVarGroupDDI(XMLStreamWriter xmlw, JsonObject varGrp) throws XMLStreamException {
        xmlw.writeStartElement("varGrp");
        xmlw.writeAttribute("ID", "VG" + varGrp.getJsonNumber("id").toString());
        String vars = "";
        JsonArray varsInGroup = varGrp.getJsonArray("dataVariableIds");
        for (int j=0;j<varsInGroup.size();j++){
            vars = vars + " v" + varsInGroup.getString(j);
        }
        vars = vars.trim();
        XmlWriterUtil.writeAttribute(xmlw, "var", vars );


        if (varGrp.containsKey("label")) {
            xmlw.writeStartElement("labl");
            xmlw.writeCharacters(varGrp.getString("label"));
            xmlw.writeEndElement(); // group label (labl)
        }

        xmlw.writeEndElement(); //varGrp
    }
    
    private static void createVarDDI(XMLStreamWriter xmlw, JsonObject dvar, String fileId, String fileMetadataId) throws XMLStreamException {
        xmlw.writeStartElement("var");
        xmlw.writeAttribute("ID", "v" + dvar.getJsonNumber("id").toString());
        XmlWriterUtil.writeAttribute(xmlw, "name", dvar.getString("name"));

        JsonObject vm = null;
        JsonArray vmArray = dvar.getJsonArray("variableMetadata"); 
        for (int i=0;i< vmArray.size();i++) {
            JsonObject curVm =vmArray.getJsonObject(i); 
            if (curVm.containsKey("fileMetadataId") && curVm.getString("fileMetadataId").equals(fileMetadataId) ){
                vm = curVm;
                break;
            }
        }

        if (dvar.containsKey("numberOfDecimalPoints")) {
            XmlWriterUtil.writeAttribute(xmlw, "dcml", dvar.getJsonNumber("numberOfDecimalPoints").toString());
        }

        if (dvar.getBoolean("isOrderedCategorical")) {
            xmlw.writeAttribute("nature", "ordinal");
        }

        if (dvar.containsKey("variableIntervalType")) {
            XmlWriterUtil.writeAttribute(xmlw, "intrvl", dvar.getString("variableIntervalType"));
        }

        if (vm != null) {
            if (vm.getBoolean("isWeightvar")) {
                xmlw.writeAttribute("wgt", "wgt");
            }
            if (vm.containsKey("isWeighted") && vm.containsKey("weightVariableId")) {
                xmlw.writeAttribute("wgt-var", "v"+vm.getString("weightVariableId"));
            }
        }

        // location
        xmlw.writeEmptyElement("location");
        if (dvar.containsKey("fileStartPosition")) {
            XmlWriterUtil.writeAttribute(xmlw, "StartPos", dvar.getJsonNumber("fileStartPosition").toString());
        }
        if (dvar.containsKey("fileEndPosition")) {
            XmlWriterUtil.writeAttribute(xmlw, "EndPos", dvar.getJsonNumber("fileEndPosition").toString());
        }
        if (dvar.containsKey("recordSegmentNumber")) {
            XmlWriterUtil.writeAttribute(xmlw, "RecSegNo", dvar.getJsonNumber("recordSegmentNumber").toString());
        }

        xmlw.writeAttribute("fileid", "f" + fileId);

        // labl
        if ((vm == null || !vm.containsKey("label"))) {
            if(dvar.containsKey("label")) {
                xmlw.writeStartElement("labl");
                xmlw.writeAttribute("level", "variable");
                xmlw.writeCharacters(dvar.getString("label"));
                xmlw.writeEndElement(); //labl
            }
        } else {
            xmlw.writeStartElement("labl");
            xmlw.writeAttribute("level", "variable");
            xmlw.writeCharacters(vm.getString("label"));
            xmlw.writeEndElement(); //labl
        }

        if (vm != null) {
            if (vm.containsKey("literalQuestion") || vm.containsKey("interviewInstruction") || vm.containsKey("postQuestion")) {
                xmlw.writeStartElement("qstn");
                if (vm.containsKey("literalQuestion")) {
                    xmlw.writeStartElement("qstnLit");
                    xmlw.writeCharacters(vm.getString("literalQuestion"));
                    xmlw.writeEndElement(); // qstnLit
                }
                if (vm.containsKey("interviewInstruction")) {
                    xmlw.writeStartElement("ivuInstr");
                    xmlw.writeCharacters(vm.getString("interviewInstruction"));
                    xmlw.writeEndElement(); //ivuInstr
                }
                if (vm.containsKey("postQuestion")) {
                    xmlw.writeStartElement("postQTxt");
                    xmlw.writeCharacters(vm.getString("postQuestion"));
                    xmlw.writeEndElement(); //ivuInstr
                }
                xmlw.writeEndElement(); //qstn
            }
        }

        // invalrng
        if (dvar.containsKey("invalidRanges")) {
            boolean invalrngAdded = false;
            JsonArray ranges = dvar.getJsonArray("invalidRanges");
            for (int i = 0; i < ranges.size(); i++) {
                JsonObject range = ranges.getJsonObject(0);
                // if (range.getBeginValueType() != null &&
                // range.getBeginValueType().getName().equals(DB_VAR_RANGE_TYPE_POINT)) {
                if (range.getBoolean("hasBeginValueType") && range.getBoolean("isBeginValueTypePoint")) {
                    if (range.containsKey("beginValue")) {
                        invalrngAdded = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "invalrng", invalrngAdded);
                        xmlw.writeEmptyElement("item");
                        XmlWriterUtil.writeAttribute(xmlw, "VALUE", range.getString("beginValue"));
                    }
                } else {
                    invalrngAdded = XmlWriterUtil.writeOpenTagIfNeeded(xmlw, "invalrng", invalrngAdded);
                    xmlw.writeEmptyElement("range");
                    if (range.getBoolean("hasBeginValueType") && range.containsKey("beginValue")) {
                        if (range.getBoolean("isBeginValueTypeMin")) {
                            XmlWriterUtil.writeAttribute(xmlw, "min", range.getString("beginValue"));
                        } else if (range.getBoolean("isBeginValueTypeMinExcl")) {
                            XmlWriterUtil.writeAttribute(xmlw, "minExclusive", range.getString("beginValue"));
                        }
                    }
                    if (range.getBoolean("hasEndValueType") && range.containsKey("endValue")) {
                        if (range.getBoolean("isEndValueTypeMax")) {
                            XmlWriterUtil.writeAttribute(xmlw, "max", range.getString("endValue"));
                        } else if (range.getBoolean("isEndValueTypeMaxExcl")) {
                            XmlWriterUtil.writeAttribute(xmlw, "maxExclusive", range.getString("endValue"));
                        }
                    }
                }
            }
            if (invalrngAdded) {
                xmlw.writeEndElement(); // invalrng
            }
        }

        //universe
        if (vm != null) {
            if (vm.containsKey("universe")) {
                xmlw.writeStartElement("universe");
                xmlw.writeCharacters(vm.getString("universe"));
                xmlw.writeEndElement(); //universe
            }
        }

        // sum stats
        if (dvar.containsKey("summaryStatistics")) {
            for (Entry<String, JsonValue> sumStat : dvar.getJsonObject("summaryStatistics").entrySet()) {
                xmlw.writeStartElement("sumStat");
                XmlWriterUtil.writeAttribute(xmlw, "type", sumStat.getKey());
                xmlw.writeCharacters(((JsonString)sumStat.getValue()).getString());
                xmlw.writeEndElement(); // sumStat
            }
        }

        // categories
        if (dvar.containsKey("variableCategories")) {
            JsonArray varCats = dvar.getJsonArray("variableCategories");
            for (int i = 0; i < varCats.size(); i++) {
                JsonObject varCat = varCats.getJsonObject(i);
                xmlw.writeStartElement("catgry");
                if (varCat.getBoolean("isMissing")) {
                    xmlw.writeAttribute("missing", "Y");
                }

                // catValu
                xmlw.writeStartElement("catValu");
                xmlw.writeCharacters(varCat.getString("value"));
                xmlw.writeEndElement(); // catValu

                // label
                if (varCat.containsKey("label")) {
                    xmlw.writeStartElement("labl");
                    xmlw.writeAttribute("level", "category");
                    xmlw.writeCharacters(varCat.getString("label"));
                    xmlw.writeEndElement(); // labl
                }

                // catStat
                if (varCat.containsKey("frequency")) {
                    xmlw.writeStartElement("catStat");
                    xmlw.writeAttribute("type", "freq");
                    Double freq = varCat.getJsonNumber("frequency").doubleValue();
                    // if frequency is actually a long value, we want to write "100" instead of
                    // "100.0"
                    if (Math.floor(freq) == freq) {
                        xmlw.writeCharacters(Long.valueOf(freq.longValue()).toString());
                    } else {
                        xmlw.writeCharacters(freq.toString());
                    }
                    xmlw.writeEndElement(); // catStat
                }

                // catStat weighted freq
                if (vm != null && vm.getBoolean("isWeighted")) {
                    JsonArray catMetas = vm.getJsonArray("categoryMetadatas");
                    for (int j = 0; i < catMetas.size(); j++) {
                        JsonObject cm = catMetas.getJsonObject(j);
                        if (cm.getString("categoryValue").equals(varCat.getString("value"))) {
                            xmlw.writeStartElement("catStat");
                            xmlw.writeAttribute("wgtd", "wgtd");
                            xmlw.writeAttribute("type", "freq");
                            xmlw.writeCharacters(cm.getJsonNumber("wFreq").toString());
                            xmlw.writeEndElement(); // catStat
                            break;
                        }
                    }
                }

                xmlw.writeEndElement(); // catgry
            }
        }


        // varFormat
        xmlw.writeEmptyElement("varFormat");
        if(dvar.containsKey("variableFormatType")) {
            XmlWriterUtil.writeAttribute(xmlw, "type", dvar.getString("variableFormatType").toLowerCase());
        } else {
            throw new XMLStreamException("Illegal Variable Format Type!");
        }
        if(dvar.containsKey("format")) {
            XmlWriterUtil.writeAttribute(xmlw, "formatname", dvar.getString("format"));
        }
        //experiment writeAttribute(xmlw, "schema", dv.getFormatSchema());
        if(dvar.containsKey("formatCategory")) {
            XmlWriterUtil.writeAttribute(xmlw, "category", dvar.getString("formatCategory"));
        }

        // notes
        if (dvar.containsKey("UNF") && !dvar.getString("UNF").isBlank()) {
            xmlw.writeStartElement("notes");
            xmlw.writeAttribute("subject", "Universal Numeric Fingerprint");
            xmlw.writeAttribute("level", "variable");
            xmlw.writeAttribute("type", "Dataverse:UNF");
            xmlw.writeCharacters(dvar.getString("UNF"));
            xmlw.writeEndElement(); //notes
        }

        if (vm != null) {
            if (vm.containsKey("notes")) {
                xmlw.writeStartElement("notes");
                xmlw.writeCData(vm.getString("notes"));
                xmlw.writeEndElement(); //notes CDATA
            }
        }



        xmlw.writeEndElement(); //var

    }
    
    private static void createFileDscr(XMLStreamWriter xmlw, JsonArray fileDetails) throws XMLStreamException {
        String dataverseUrl = SystemConfig.getDataverseSiteUrlStatic();
        for (int i =0;i<fileDetails.size();i++) {
            JsonObject fileJson = fileDetails.getJsonObject(i);
            //originalFileFormat is one of several keys that only exist for tabular data
            if (fileJson.containsKey("originalFileFormat")) {
                JsonObject dt = null;
                if (fileJson.containsKey("dataTables")) {
                    dt = fileJson.getJsonArray("dataTables").getJsonObject(0);
                }
                xmlw.writeStartElement("fileDscr");
                String fileId = fileJson.getJsonNumber("id").toString();
                xmlw.writeAttribute("ID", "f" + fileId);
                xmlw.writeAttribute("URI", dataverseUrl + "/api/access/datafile/" + fileId);

                xmlw.writeStartElement("fileTxt");
                xmlw.writeStartElement("fileName");
                xmlw.writeCharacters(fileJson.getString("filename"));
                xmlw.writeEndElement(); // fileName

                if (dt != null && (dt.containsKey("caseQuantity") || dt.containsKey("varQuantity")
                        || dt.containsKey("recordsPerCase"))) {
                    xmlw.writeStartElement("dimensns");

                    if (dt.containsKey("caseQuantity")) {
                        xmlw.writeStartElement("caseQnty");
                        xmlw.writeCharacters(dt.getJsonNumber("caseQuantity").toString());
                        xmlw.writeEndElement(); // caseQnty
                    }

                    if (dt.containsKey("varQuantity")) {
                        xmlw.writeStartElement("varQnty");
                        xmlw.writeCharacters(dt.getJsonNumber("varQuantity").toString());
                        xmlw.writeEndElement(); // varQnty
                    }

                    if (dt.containsKey("recordsPerCase")) {
                        xmlw.writeStartElement("recPrCas");
                        xmlw.writeCharacters(dt.getJsonNumber("recordsPerCase").toString());
                        xmlw.writeEndElement(); // recPrCas
                    }

                    xmlw.writeEndElement(); // dimensns
                }

                xmlw.writeStartElement("fileType");
                xmlw.writeCharacters(fileJson.getString("contentType"));
                xmlw.writeEndElement(); // fileType

                xmlw.writeEndElement(); // fileTxt

                // various notes:
                // this specially formatted note section is used to store the UNF
                // (Universal Numeric Fingerprint) signature:
                if ((dt!=null) && (dt.containsKey("UNF") && !dt.getString("UNF").isBlank())) {
                    xmlw.writeStartElement("notes");
                    xmlw.writeAttribute("level", LEVEL_FILE);
                    xmlw.writeAttribute("type", NOTE_TYPE_UNF);
                    xmlw.writeAttribute("subject", NOTE_SUBJECT_UNF);
                    xmlw.writeCharacters(dt.getString("UNF"));
                    xmlw.writeEndElement(); // notes
                }

                // If any tabular tags are present, each is formatted in a 
                // dedicated note:
                if (fileJson.containsKey("tabularTags")) {
                    JsonArray tags = fileJson.getJsonArray("tabularTags");
                    for (int j = 0; j < tags.size(); j++) {
                        xmlw.writeStartElement("notes");
                        xmlw.writeAttribute("level", LEVEL_FILE);
                        xmlw.writeAttribute("type", NOTE_TYPE_TAG);
                        xmlw.writeAttribute("subject", NOTE_SUBJECT_TAG);
                        xmlw.writeCharacters(tags.getString(j));
                        xmlw.writeEndElement(); // notes
                    }
                }
                
                // Adding a dedicated node for the description entry (for 
                // non-tabular files we format it under the <txt> field)
                if (fileJson.containsKey("description")) {
                    xmlw.writeStartElement("notes");
                    xmlw.writeAttribute("level", LEVEL_FILE);
                    xmlw.writeAttribute("type", NOTE_TYPE_FILEDESCRIPTION);
                    xmlw.writeAttribute("subject", NOTE_SUBJECT_FILEDESCRIPTION);
                    xmlw.writeCharacters(fileJson.getString("description"));
                    xmlw.writeEndElement(); // notes
                }

                // TODO: add the remaining fileDscr elements!
                xmlw.writeEndElement(); // fileDscr
            }
        }
    }
    
    



    public static void datasetHtmlDDI(InputStream datafile, OutputStream outputStream) throws XMLStreamException {

        try {
            // Get secure DocumentBuilder from our utility class
            DocumentBuilder builder = XmlUtil.getSecureDocumentBuilder();
            if (builder == null) {
                logger.severe("Could not create secure document builder");
                return;
            }
            InputStream styleSheetInput = DdiExportUtil.class.getClassLoader().getResourceAsStream("edu/harvard/iq/dataverse/codebook2-0.xsl");

            Document document = builder.parse(datafile);

            // Use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            
            // Apply similar security settings to TransformerFactory
            tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
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
        } catch (IOException ioe) {
            // I/O error
            logger.warning("I/O error " + ioe.getMessage());
        }
    }

    public static void injectSettingsService(SettingsServiceBean settingsSvc) {
        settingsService=settingsSvc;
    }

}
