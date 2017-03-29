package edu.harvard.iq.dataverse.export.ddi;

import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.LEVEL_FILE;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_SUBJECT_UNF;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_TAG;
import static edu.harvard.iq.dataverse.export.DDIExportServiceBean.NOTE_TYPE_UNF;
import static edu.harvard.iq.dataverse.util.SystemConfig.FQDN;
import static edu.harvard.iq.dataverse.util.SystemConfig.SITE_URL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.google.gson.Gson;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VariableRange;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());

    public static final String NOTE_TYPE_CONTENTTYPE = "DATAVERSE:CONTENTTYPE";
    public static final String NOTE_SUBJECT_CONTENTTYPE = "Content/MIME Type";

    public static String datasetDtoAsJson2ddi(final String datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson));
        final Gson gson = new Gson();
        final DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson, DatasetDTO.class);
        try {
            return dto2ddi(datasetDto);
        } catch (final XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    // "short" ddi, without the "<fileDscr>" and "<dataDscr>/<var>" sections:
    public static void datasetJson2ddi(final JsonObject datasetDtoAsJson, final OutputStream outputStream)
        throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        final Gson gson = new Gson();
        final DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        dtoddi(datasetDto, outputStream);
    }

    private static String dto2ddi(final DatasetDTO datasetDto) throws XMLStreamException {
        final OutputStream outputStream = new ByteArrayOutputStream();
        dtoddi(datasetDto, outputStream);
        final String xml = outputStream.toString();
        return XmlPrinter.prettyPrintXml(xml);
    }

    private static void dtoddi(final DatasetDTO datasetDto, final OutputStream outputStream) throws XMLStreamException {
        final XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("ddi:codebook:2_5");
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xsi:schemaLocation",
            "ddi:codebook:2_5 http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd");
        writeAttribute(xmlw, "version", "2.5");
        createStdyDscr(xmlw, datasetDto);
        createOtherMats(xmlw, datasetDto.getDatasetVersion().getFiles());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
    }

    // "full" ddi, with the the "<fileDscr>" and "<dataDscr>/<var>" sections:
    public static void datasetJson2ddi(final JsonObject datasetDtoAsJson, final DatasetVersion version,
        final OutputStream outputStream) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        final Gson gson = new Gson();
        final DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);

        final XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("ddi:codebook:2_5");
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xsi:schemaLocation",
            "ddi:codebook:2_5 http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd");
        writeAttribute(xmlw, "version", "2.5");
        createStdyDscr(xmlw, datasetDto);
        createFileDscr(xmlw, version);
        createDataDscr(xmlw, version);
        createOtherMatsFromFileMetadatas(xmlw, version.getFileMetadatas());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
    }

    /**
     * @todo This is just a stub, copied from DDIExportServiceBean. It should produce valid DDI based on
     *       http://guides.dataverse.org/en/latest/developers/tools.html#msv but it is incomplete and will be worked on
     *       as part of https://github.com/IQSS/dataverse/issues/2579 . We'll want to reference the DVN 3.x code for
     *       creating a complete DDI.
     *
     * @todo Rename this from "study" to "dataset".
     */
    private static void createStdyDscr(final XMLStreamWriter xmlw, final DatasetDTO datasetDto)
        throws XMLStreamException {
        final DatasetVersionDTO version = datasetDto.getDatasetVersion();
        final String persistentProtocol = datasetDto.getProtocol();
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

        final String persistentAuthority = datasetDto.getAuthority();
        final String persistentId = datasetDto.getIdentifier();
        // docDesc Block
        writeDocDescElement(xmlw, datasetDto);
        // stdyDesc Block
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

        xmlw.writeEndElement(); // titlStmt

        writeAuthorsElement(xmlw, version);
        writeProducersElement(xmlw, version);

        xmlw.writeStartElement("distStmt");
        writeFullElement(xmlw, "distrbtr", datasetDto.getPublisher());
        writeFullElement(xmlw, "distDate", datasetDto.getPublicationDate());
        xmlw.writeEndElement(); // diststmt

        xmlw.writeEndElement(); // citation
        // End Citation Block

        // Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");

        writeSubjectElement(xmlw, version); // Subject and Keywords
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

        // Social Science Metadata block

        writeMethodElement(xmlw, version);

        // Terms of Use and Access
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

    private static void writeDocDescElement(final XMLStreamWriter xmlw, final DatasetDTO datasetDto)
        throws XMLStreamException {
        final DatasetVersionDTO version = datasetDto.getDatasetVersion();
        final String persistentProtocol = datasetDto.getProtocol();
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

        final String persistentAuthority = datasetDto.getAuthority();
        final String persistentId = datasetDto.getIdentifier();

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

    private static void writeVersionStatement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        xmlw.writeStartElement("verStmt");
        writeAttribute(xmlw, "source", "DVN");
        xmlw.writeStartElement("version");
        writeAttribute(xmlw, "date", datasetVersionDTO.getReleaseTime().substring(0, 10));
        writeAttribute(xmlw, "type", datasetVersionDTO.getVersionState().toString());
        xmlw.writeCharacters(datasetVersionDTO.getVersionNumber().toString());
        xmlw.writeEndElement(); // version
        xmlw.writeEndElement(); // verStmt
    }

    private static void writeSummaryDescriptionElement(final XMLStreamWriter xmlw,
        final DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("sumDscr");
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                Integer per = 0;
                Integer coll = 0;
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            per++;
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            coll++;
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "dataKind", fieldDTO);
                    }
                }
            }

            if ("geospatial".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.geographicCoverage.equals(fieldDTO.getTypeName())) {
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                writeFullElementList(xmlw, "geogUnit",
                    dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.geographicUnit));
            }

            if ("socialscience".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.universe.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "universe", fieldDTO);
                    }
                    if (DatasetFieldConstant.unitOfAnalysis.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "anlyUnit", fieldDTO);
                    }
                }
            }
        }
        xmlw.writeEndElement(); // sumDscr
    }

    private static void writeMultipleElement(final XMLStreamWriter xmlw, final String element, final FieldDTO fieldDTO)
        throws XMLStreamException {
        for (final String value : fieldDTO.getMultiplePrimitive()) {
            writeFullElement(xmlw, element, value);
        }
    }

    private static void writeDateElement(final XMLStreamWriter xmlw, final String element, final String cycle,
        final String event, final String dateIn) throws XMLStreamException {

        xmlw.writeStartElement(element);
        writeAttribute(xmlw, "cycle", cycle);
        writeAttribute(xmlw, "event", event);
        writeAttribute(xmlw, "date", dateIn);
        xmlw.writeCharacters(dateIn);
        xmlw.writeEndElement();

    }

    private static void writeMethodElement(final XMLStreamWriter xmlw, final DatasetVersionDTO version)
        throws XMLStreamException {
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

        xmlw.writeEndElement(); // dataColl
        xmlw.writeStartElement("anlyInfo");
        writeFullElement(xmlw, "anylInfo", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));
        writeFullElement(xmlw, "respRate", dto2Primitive(version, DatasetFieldConstant.responseRate));
        writeFullElement(xmlw, "estSmpErr", dto2Primitive(version, DatasetFieldConstant.samplingErrorEstimates));
        writeFullElement(xmlw, "dataAppr", dto2Primitive(version, DatasetFieldConstant.otherDataAppraisal));
        xmlw.writeEndElement(); // anlyInfo
        writeNotesElement(xmlw, version);

        xmlw.writeEndElement();// method
    }

    private static void writeSubjectElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {

        // Key Words and Topic Classification

        xmlw.writeStartElement("subject");
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())) {
                        for (final String subject : fieldDTO.getMultipleVocab()) {
                            xmlw.writeStartElement("keyword");
                            xmlw.writeCharacters(subject);
                            xmlw.writeEndElement(); // Keyword
                        }
                    }

                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            String keywordVocab = "";
                            String keywordURI = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
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
                                if (!keywordVocab.isEmpty()) {
                                    writeAttribute(xmlw, "vocab", keywordVocab);
                                }
                                if (!keywordURI.isEmpty()) {
                                    writeAttribute(xmlw, "URI", keywordURI);
                                }
                                xmlw.writeCharacters(keywordValue);
                                xmlw.writeEndElement(); // Keyword
                            }

                        }
                    }
                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String topicClassificationValue = "";
                            String topicClassificationVocab = "";
                            String topicClassificationURI = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                    topicClassificationValue = next.getSinglePrimitive();
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
                                if (!topicClassificationVocab.isEmpty()) {
                                    writeAttribute(xmlw, "vocab", topicClassificationVocab);
                                }
                                if (!topicClassificationURI.isEmpty()) {
                                    writeAttribute(xmlw, "URI", topicClassificationURI);
                                }
                                xmlw.writeCharacters(topicClassificationValue);
                                xmlw.writeEndElement(); // topcClas
                            }
                        }
                    }
                }
            }
        }
        xmlw.writeEndElement(); // subject
    }

    private static void writeAuthorsElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {

        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("rspStmt");
                        String authorName = "";
                        String authorAffiliation = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    authorName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorAffiliation.equals(next.getTypeName())) {
                                    authorAffiliation = next.getSinglePrimitive();
                                }
                            }
                            if (!authorName.isEmpty()) {
                                xmlw.writeStartElement("AuthEnty");
                                if (!authorAffiliation.isEmpty()) {
                                    writeAttribute(xmlw, "affiliation", authorAffiliation);
                                }
                                xmlw.writeCharacters(authorName);
                                xmlw.writeEndElement(); // AuthEnty
                            }
                        }
                        xmlw.writeEndElement(); // rspStmt
                    }
                }
            }
        }
    }

    private static void writeContactsElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {

        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.datasetContact.equals(fieldDTO.getTypeName())) {
                        String datasetContactName = "";
                        String datasetContactAffiliation = "";
                        String datasetContactEmail = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.datasetContactName.equals(next.getTypeName())) {
                                    datasetContactName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.datasetContactAffiliation.equals(next.getTypeName())) {
                                    datasetContactAffiliation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.datasetContactEmail.equals(next.getTypeName())) {
                                    datasetContactEmail = next.getSinglePrimitive();
                                }
                            }
                            if (!datasetContactName.isEmpty()) {
                                xmlw.writeStartElement("contact");
                                if (!datasetContactAffiliation.isEmpty()) {
                                    writeAttribute(xmlw, "affiliation", datasetContactAffiliation);
                                }
                                if (!datasetContactEmail.isEmpty()) {
                                    writeAttribute(xmlw, "email", datasetContactEmail);
                                }
                                xmlw.writeCharacters(datasetContactName);
                                xmlw.writeEndElement(); // AuthEnty
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeProducersElement(final XMLStreamWriter xmlw, final DatasetVersionDTO version)
        throws XMLStreamException {
        xmlw.writeStartElement("prodStmt");
        for (final Map.Entry<String, MetadataBlockDTO> entry : version.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();

            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {

                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String producerName = "";
                            String producerAffiliation = "";
                            String producerAbbreviation = "";
                            String producerLogo = "";
                            String producerURL = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                                xmlw.writeEndElement(); // AuthEnty
                            }
                        }

                    }
                }
            }
        }
        writeFullElement(xmlw, "prodDate", dto2Primitive(version, DatasetFieldConstant.productionDate));
        writeFullElement(xmlw, "prodPlac", dto2Primitive(version, DatasetFieldConstant.productionPlace));

        writeGrantElement(xmlw, version);
        xmlw.writeEndElement(); // prodStmt
    }

    private static void writeDistributorsElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.distributor.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("distrbtr");
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String distributorName = "";
                            String distributorAffiliation = "";
                            String distributorAbbreviation = "";
                            String distributorURL = "";
                            String distributorLogoURL = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
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
                                xmlw.writeEndElement(); // AuthEnty
                            }
                        }
                        xmlw.writeEndElement(); // rspStmt
                    }
                }
            }
        }
    }

    private static void writeRelPublElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String pubString = "";
                            String citation = "";
                            String IDType = "";
                            String IDNo = "";
                            String url = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.publicationCitation.equals(next.getTypeName())) {
                                    citation = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDType.equals(next.getTypeName())) {
                                    IDType = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDNumber.equals(next.getTypeName())) {
                                    IDNo = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationURL.equals(next.getTypeName())) {
                                    url = next.getSinglePrimitive();
                                }
                            }
                            pubString = appendCommaSeparatedValue(citation, IDType);
                            pubString = appendCommaSeparatedValue(pubString, IDNo);
                            pubString = appendCommaSeparatedValue(pubString, url);
                            if (!pubString.isEmpty()) {
                                xmlw.writeStartElement("relPubl");
                                xmlw.writeCharacters(pubString);
                                xmlw.writeEndElement(); // relPubl
                            }
                        }
                    }
                }
            }
        }
    }

    private static String appendCommaSeparatedValue(final String inVal, final String next) {
        if (!next.isEmpty()) {
            if (!inVal.isEmpty()) {
                return inVal + ", " + next;
            } else {
                return next;
            }
        }
        return inVal;
    }

    private static void writeAbstractElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        String descriptionText = "";
                        String descriptionDate = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionText = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.descriptionDate.equals(next.getTypeName())) {
                                    descriptionDate = next.getSinglePrimitive();
                                }
                            }
                            if (!descriptionText.isEmpty()) {
                                xmlw.writeStartElement("abstract");
                                if (!descriptionDate.isEmpty()) {
                                    writeAttribute(xmlw, "date", descriptionDate);
                                }
                                xmlw.writeCharacters(descriptionText);
                                xmlw.writeEndElement(); // abstract
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeGrantElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String grantNumber = "";
                            String grantAgency = "";
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                // XXX: #45 - Map Funding Agency to Grant Agency, fix DDI Exporter
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.grantNumberValue.equals(next.getTypeName())) {
                                    grantNumber = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.grantNumberAgency.equals(next.getTypeName())) {
                                    grantAgency = next.getSinglePrimitive();
                                }
                            }
                            if (!grantAgency.isEmpty()) {
                                xmlw.writeStartElement("grantNo");
                                writeAttribute(xmlw, "agency", grantAgency); // grant/funding agency
                                xmlw.writeCharacters(grantNumber);
                                xmlw.writeEndElement(); // grantno
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeOtherIdElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.otherId.equals(fieldDTO.getTypeName())) {
                        String otherId = "";
                        String otherIdAgency = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.otherIdValue.equals(next.getTypeName())) {
                                    otherId = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.otherIdAgency.equals(next.getTypeName())) {
                                    otherIdAgency = next.getSinglePrimitive();
                                }
                            }
                            if (!otherId.isEmpty()) {
                                xmlw.writeStartElement("IDNo");
                                if (!otherIdAgency.isEmpty()) {
                                    writeAttribute(xmlw, "agency", otherIdAgency);
                                }
                                xmlw.writeCharacters(otherId);
                                xmlw.writeEndElement(); // IDNo
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeSoftwareElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.software.equals(fieldDTO.getTypeName())) {
                        String softwareName = "";
                        String softwareVersion = "";
                        for (final HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                final FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.softwareName.equals(next.getTypeName())) {
                                    softwareName = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.softwareVersion.equals(next.getTypeName())) {
                                    softwareVersion = next.getSinglePrimitive();
                                }
                            }
                            if (!softwareName.isEmpty()) {
                                xmlw.writeStartElement("software");
                                if (!softwareVersion.isEmpty()) {
                                    writeAttribute(xmlw, "version", softwareVersion);
                                }
                                xmlw.writeCharacters(softwareName);
                                xmlw.writeEndElement(); // software
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeSeriesElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.series.equals(fieldDTO.getTypeName())) {
                        xmlw.writeStartElement("serStmt");
                        String seriesName = "";
                        String seriesInformation = "";
                        final Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            final FieldDTO next = iterator.next();
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
                            xmlw.writeEndElement(); // grantno
                        }
                        if (!seriesInformation.isEmpty()) {
                            xmlw.writeStartElement("serInfo");
                            xmlw.writeCharacters(seriesInformation);
                            xmlw.writeEndElement(); // grantno
                        }
                        xmlw.writeEndElement(); // serStmt
                    }
                }
            }
        }
    }

    private static void writeTargetSampleElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("socialscience".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.targetSampleSize.equals(fieldDTO.getTypeName())) {
                        String sizeFormula = "";
                        String actualSize = "";
                        final Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            final FieldDTO next = iterator.next();
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
                            xmlw.writeEndElement(); // sampleSizeFormula
                        }
                        if (!actualSize.isEmpty()) {
                            xmlw.writeStartElement("sampleSize");
                            xmlw.writeCharacters(actualSize);
                            xmlw.writeEndElement(); // sampleSize
                        }
                    }
                }
            }
        }
    }

    private static void writeNotesElement(final XMLStreamWriter xmlw, final DatasetVersionDTO datasetVersionDTO)
        throws XMLStreamException {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final String key = entry.getKey();
            final MetadataBlockDTO value = entry.getValue();
            if ("socialscience".equals(key)) {
                for (final FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.socialScienceNotes.equals(fieldDTO.getTypeName())) {
                        String notesText = "";
                        String notesType = "";
                        String notesSubject = "";
                        final Set<FieldDTO> foo = fieldDTO.getSingleCompound();
                        for (final Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                            final FieldDTO next = iterator.next();
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
                            if (!notesType.isEmpty()) {
                                writeAttribute(xmlw, "type", notesType);
                            }
                            if (!notesSubject.isEmpty()) {
                                writeAttribute(xmlw, "subject", notesSubject);
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
    private static void createOtherMats(final XMLStreamWriter xmlw, final List<FileDTO> fileDtos)
        throws XMLStreamException {
        // The preferred URL for this dataverse, for cooking up the file access API links:
        final String dataverseUrl = getDataverseSiteUrl();

        for (final FileDTO fileDTo : fileDtos) {
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (fileDTo.getDataFile().getDataTables() == null || fileDTo.getDataFile().getDataTables().isEmpty()) {
                xmlw.writeStartElement("otherMat");
                writeAttribute(xmlw, "ID", "f" + fileDTo.getDataFile().getId());
                writeAttribute(xmlw, "URI", dataverseUrl + "/api/access/datafile/" + fileDTo.getDataFile().getId());
                writeAttribute(xmlw, "level", "datafile");
                xmlw.writeStartElement("labl");
                xmlw.writeCharacters(fileDTo.getDataFile().getFilename());
                xmlw.writeEndElement(); // labl
                writeFileDescription(xmlw, fileDTo);
                // there's no readily available field in the othermat section
                // for the content type (aka mime type); so we'll store it in this
                // specially formatted notes section:
                final String contentType = fileDTo.getDataFile().getContentType();
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

    private static void createOtherMatsFromFileMetadatas(final XMLStreamWriter xmlw,
        final List<FileMetadata> fileMetadatas) throws XMLStreamException {
        // The preferred URL for this dataverse, for cooking up the file access API links:
        final String dataverseUrl = getDataverseSiteUrl();

        for (final FileMetadata fileMetadata : fileMetadatas) {
            // We'll continue using the scheme we've used before, in DVN2-3: non-tabular files are put into otherMat,
            // tabular ones - in fileDscr sections. (fileDscr sections have special fields for numbers of variables
            // and observations, etc.)
            if (fileMetadata.getDataFile() != null && !fileMetadata.getDataFile().isTabularData()) {
                xmlw.writeStartElement("otherMat");
                writeAttribute(xmlw, "ID", "f" + fileMetadata.getDataFile().getId());
                writeAttribute(xmlw, "URI",
                    dataverseUrl + "/api/access/datafile/" + fileMetadata.getDataFile().getId());
                writeAttribute(xmlw, "level", "datafile");
                xmlw.writeStartElement("labl");
                xmlw.writeCharacters(fileMetadata.getLabel());
                xmlw.writeEndElement(); // labl

                final String description = fileMetadata.getDescription();
                if (description != null) {
                    xmlw.writeStartElement("txt");
                    xmlw.writeCharacters(description);
                    xmlw.writeEndElement(); // txt
                }
                // there's no readily available field in the othermat section
                // for the content type (aka mime type); so we'll store it in this
                // specially formatted notes section:
                final String contentType = fileMetadata.getDataFile().getContentType();
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

    private static void writeFileDescription(final XMLStreamWriter xmlw, final FileDTO fileDTo)
        throws XMLStreamException {
        xmlw.writeStartElement("txt");
        final String description = fileDTo.getDataFile().getDescription();
        if (description != null) {
            xmlw.writeCharacters(description);
        }
        xmlw.writeEndElement(); // txt
    }

    private static String dto2Primitive(final DatasetVersionDTO datasetVersionDTO, final String datasetFieldTypeName) {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final MetadataBlockDTO value = entry.getValue();
            for (final FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getSinglePrimitive();
                }
            }
        }
        return null;
    }

    private static List<String> dto2PrimitiveList(final DatasetVersionDTO datasetVersionDTO,
        final String datasetFieldTypeName) {
        for (final Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            final MetadataBlockDTO value = entry.getValue();
            for (final FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getMultiplePrimitive();
                }
            }
        }
        return null;
    }

    private static void writeFullElementList(final XMLStreamWriter xmlw, final String name, final List<String> values)
        throws XMLStreamException {
        // For the simplest Elements we can
        if (values != null && !values.isEmpty()) {
            for (final String value : values) {
                xmlw.writeStartElement(name);
                xmlw.writeCharacters(value);
                xmlw.writeEndElement(); // labl
            }
        }
    }

    private static void writeFullElement(final XMLStreamWriter xmlw, final String name, final String value)
        throws XMLStreamException {
        // For the simplest Elements we can
        if (!StringUtilisEmpty(value)) {
            xmlw.writeStartElement(name);
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }
    }

    private static void writeAttribute(final XMLStreamWriter xmlw, final String name, final String value)
        throws XMLStreamException {
        if (!StringUtilisEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }

    private static boolean StringUtilisEmpty(final String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    private static void saveJsonToDisk(final String datasetVersionAsJson) throws IOException {
        Files.write(Paths.get("/tmp/out.json"), datasetVersionAsJson.getBytes());
    }

    /**
     * The "official", designated URL of the site; can be defined as a complete URL; or derived from the "official"
     * hostname. If none of these options is set, defaults to the InetAddress.getLocalHOst() and https;
     */
    private static String getDataverseSiteUrl() {
        final String hostUrl = System.getProperty(SITE_URL);
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty(FQDN);
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (final UnknownHostException e) {
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

    private static void createDataDscr(final XMLStreamWriter xmlw, final DatasetVersion datasetVersion)
        throws XMLStreamException {

        if (datasetVersion.getFileMetadatas() == null || datasetVersion.getFileMetadatas().isEmpty()) {
            return;
        }

        boolean tabularData = false;

        // we're not writing the opening <dataDscr> tag until we find an actual
        // tabular datafile.
        for (final FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            final DataFile dataFile = fileMetadata.getDataFile();

            if (dataFile != null && dataFile.isTabularData()) {
                if (!tabularData) {
                    xmlw.writeStartElement("dataDscr");
                    tabularData = true;
                }

                final List<DataVariable> vars = dataFile.getDataTable().getDataVariables();

                for (final DataVariable var : vars) {
                    createVarDDI(xmlw, var);
                }
            }
        }

        if (tabularData) {
            xmlw.writeEndElement(); // dataDscr
        }
    }

    private static void createVarDDI(final XMLStreamWriter xmlw, final DataVariable dv) throws XMLStreamException {
        xmlw.writeStartElement("var");
        writeAttribute(xmlw, "ID", "v" + dv.getId().toString());
        writeAttribute(xmlw, "name", dv.getName());

        if (dv.getNumberOfDecimalPoints() != null) {
            writeAttribute(xmlw, "dcml", dv.getNumberOfDecimalPoints().toString());
        }

        if (dv.isOrderedCategorical()) {
            writeAttribute(xmlw, "nature", "ordinal");
        }

        if (dv.getInterval() != null) {
            final String interval = dv.getIntervalLabel();
            if (interval != null) {
                writeAttribute(xmlw, "intrvl", interval);
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
        if (!StringUtilisEmpty(dv.getLabel())) {
            xmlw.writeStartElement("labl");
            writeAttribute(xmlw, "level", "variable");
            xmlw.writeCharacters(dv.getLabel());
            xmlw.writeEndElement(); // labl
        }

        // invalrng
        boolean invalrngAdded = false;
        for (final VariableRange range : dv.getInvalidRanges()) {
            // if (range.getBeginValueType() != null &&
            // range.getBeginValueType().getName().equals(DB_VAR_RANGE_TYPE_POINT)) {
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

        // universe
        if (!StringUtilisEmpty(dv.getUniverse())) {
            xmlw.writeStartElement("universe");
            xmlw.writeCharacters(dv.getUniverse());
            xmlw.writeEndElement(); // universe
        }

        // sum stats
        for (final SummaryStatistic sumStat : dv.getSummaryStatistics()) {
            xmlw.writeStartElement("sumStat");
            if (sumStat.getTypeLabel() != null) {
                writeAttribute(xmlw, "type", sumStat.getTypeLabel());
            } else {
                writeAttribute(xmlw, "type", "unknown");
            }
            xmlw.writeCharacters(sumStat.getValue());
            xmlw.writeEndElement(); // sumStat
        }

        // categories
        for (final VariableCategory cat : dv.getCategories()) {
            xmlw.writeStartElement("catgry");
            if (cat.isMissing()) {
                writeAttribute(xmlw, "missing", "Y");
            }

            // catValu
            xmlw.writeStartElement("catValu");
            xmlw.writeCharacters(cat.getValue());
            xmlw.writeEndElement(); // catValu

            // label
            if (!StringUtilisEmpty(cat.getLabel())) {
                xmlw.writeStartElement("labl");
                writeAttribute(xmlw, "level", "category");
                xmlw.writeCharacters(cat.getLabel());
                xmlw.writeEndElement(); // labl
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
                xmlw.writeEndElement(); // catStat
            }

            xmlw.writeEndElement(); // catgry
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
        // experiment writeAttribute(xmlw, "schema", dv.getFormatSchema());
        writeAttribute(xmlw, "category", dv.getFormatCategory());

        // notes
        if (dv.getUnf() != null && !"".equals(dv.getUnf())) {
            xmlw.writeStartElement("notes");
            writeAttribute(xmlw, "subject", "Universal Numeric Fingerprint");
            writeAttribute(xmlw, "level", "variable");
            writeAttribute(xmlw, "type", "Dataverse:UNF");
            xmlw.writeCharacters(dv.getUnf());
            xmlw.writeEndElement(); // notes
        }

        xmlw.writeEndElement(); // var

    }

    private static void createFileDscr(final XMLStreamWriter xmlw, final DatasetVersion datasetVersion)
        throws XMLStreamException {
        final String dataverseUrl = getDataverseSiteUrl();
        for (final FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            final DataFile dataFile = fileMetadata.getDataFile();

            if (dataFile != null && dataFile.isTabularData()) {
                final DataTable dt = dataFile.getDataTable();
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

    private static boolean checkParentElement(final XMLStreamWriter xmlw, final String elementName,
        final boolean elementAdded) throws XMLStreamException {
        if (!elementAdded) {
            xmlw.writeStartElement(elementName);
        }

        return true;
    }

}
