package edu.harvard.iq.dataverse.export.ddi;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.xml.XmlAttribute;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.DDI_NAMESPACE;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.DDI_SCHEMA_LOCATION;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.DDI_VERSION;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeAttribute;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullAttributesOnlyElement;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElement;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElementList;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElementWithAttributes;
import static java.util.stream.Collectors.toList;


@ApplicationScoped
public class DdiDatasetExportService {

    private DdiDataAccessWriter ddiDataAccessWriter;
    private DdiFileWriter ddiFileWriter;
    private DdiVariableWriter ddiVariableWriter;

    // -------------------- CONSTRUCTORS --------------------

    // JEE requirement
    DdiDatasetExportService() {}

    @Inject
    public DdiDatasetExportService(
            DdiDataAccessWriter ddiDataAccessWriter,
            DdiFileWriter ddiFileWriter,
            DdiVariableWriter ddiVariableWriter) {
        this.ddiDataAccessWriter = ddiDataAccessWriter;
        this.ddiFileWriter = ddiFileWriter;
        this.ddiVariableWriter = ddiVariableWriter;
    }

    // -------------------- LOGIC --------------------

    // "short" ddi, without the "<fileDscr>"  and "<dataDscr>/<var>" sections:
    public void datasetJson2ddi(DatasetDTO datasetDto, OutputStream outputStream,
                                Map<String, Map<String, String>> localizedVocabularyIndex)
            throws XMLStreamException {

        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        writeCodeBookAttributes(xmlw);

        writeDocDescElement(xmlw, datasetDto);
        createStdyDscr(xmlw, datasetDto, localizedVocabularyIndex);
        if(!datasetDto.getEmbargoActive()) {
            createOtherMatsFromFileDtos(xmlw, datasetDto.getDatasetVersion().getFiles());
        }
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
        xmlw.close();
    }

    // "full" ddi, with the the "<fileDscr>"  and "<dataDscr>/<var>" sections:
    public void datasetJson2ddi(DatasetDTO datasetDto, DatasetVersion version, OutputStream outputStream,
                                       Map<String, Map<String, String>> localizedVocabularyIndex)
            throws XMLStreamException {

        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        writeCodeBookAttributes(xmlw);

        writeDocDescElement(xmlw, datasetDto);
        createStdyDscr(xmlw, datasetDto, localizedVocabularyIndex);
        if(!datasetDto.getEmbargoActive()) {
            createFileDscr(xmlw, version.getFileMetadatas());
            createDataDscr(xmlw, version.getFileMetadatas());
            createOtherMatsFromFileMetadatas(xmlw, version.getFileMetadatas());
        }
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
        xmlw.close();
    }

    // -------------------- PRIVATE --------------------

    private void writeCodeBookAttributes(XMLStreamWriter xmlw) throws XMLStreamException {
        xmlw.writeDefaultNamespace(DDI_NAMESPACE);
        xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xmlw.writeAttribute("xsi:schemaLocation", DDI_NAMESPACE + " " + DDI_SCHEMA_LOCATION);
        xmlw.writeAttribute("version", DDI_VERSION);
    }


    private void writeDocDescElement(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();

        xmlw.writeStartElement("docDscr");
        xmlw.writeStartElement("citation");

        xmlw.writeStartElement("titlStmt");
        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        writeIDNoForDataset(xmlw, datasetDto);
        xmlw.writeEndElement(); // titlStmt

        xmlw.writeStartElement("distStmt");
        writeFullElement(xmlw, "distrbtr", datasetDto.getPublisher());
        writeFullElement(xmlw, "distDate", datasetDto.getPublicationDate());
        xmlw.writeEndElement(); // diststmt

        writeVersionStatement(xmlw, version);
        writeFullElement(xmlw, "biblCit", version.getCitation());

        xmlw.writeEndElement(); // citation
        xmlw.writeEndElement(); // docDscr

    }

    /**
     * @todo This is just a stub, copied from DDIExportServiceBean. It should
     * produce valid DDI based on
     * http://guides.dataverse.org/en/latest/developers/tools.html#msv but it is
     * incomplete and will be worked on as part of
     * https://github.com/IQSS/dataverse/issues/2579 . We'll want to reference
     * the DVN 3.x code for creating a complete DDI.
     * @todo Rename this from "study" to "dataset".
     */
    private void createStdyDscr(XMLStreamWriter xmlw, DatasetDTO datasetDto,
                                       Map<String, Map<String, String>> localizedVocabularyIndex) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();

        //stdyDesc Block
        xmlw.writeStartElement("stdyDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");

        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        writeFullElement(xmlw, "subTitl", dto2Primitive(version, DatasetFieldConstant.subTitle));
        writeFullElement(xmlw, "altTitl", dto2Primitive(version, DatasetFieldConstant.alternativeTitle));

        writeIDNoForDataset(xmlw, datasetDto);
        writeOtherIdElement(xmlw, version);

        xmlw.writeEndElement(); // titlStmt

        writeAuthorsElement(xmlw, version);
        writeProducersElement(xmlw, version);

        xmlw.writeStartElement("distStmt");
        writeFullElementWithAttributes(xmlw, "distrbtr", datasetDto.getPublisher(),
                XmlAttribute.of("source", "archive"));
        writeDistributorsElement(xmlw, version);
        writeContactsElement(xmlw, version);
        writeFullElement(xmlw, "depositr", dto2Primitive(version, DatasetFieldConstant.depositor));
        writeFullElement(xmlw, "depDate", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));
        writeFullElement(xmlw, "distDate", dto2Primitive(version, DatasetFieldConstant.distributionDate));
        xmlw.writeEndElement(); // diststmt

        writeSeriesElement(xmlw, version);

        xmlw.writeEndElement(); // citation
        //End Citation Block

        writeStdyInfo(xmlw, version);

        writeMethodElement(xmlw, version, localizedVocabularyIndex);
        ddiDataAccessWriter.writeDataAccess(xmlw , datasetDto);
        writeOtherStudyMaterial(xmlw , version);

        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.datasetLevelErrorNotes));

        xmlw.writeEndElement(); // stdyDscr

    }

    private void writeVersionStatement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("verStmt");
        writeAttribute(xmlw, "source", "archive");

        writeFullElementWithAttributes(xmlw, "version", datasetVersionDTO.getVersionNumber().toString(),
                XmlAttribute.of("date", datasetVersionDTO.getReleaseTime().substring(0, 10)),
                XmlAttribute.of("type", datasetVersionDTO.getVersionState()));

        xmlw.writeEndElement(); // verStmt
    }

    private void writeIDNoForDataset(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
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

        writeFullElementWithAttributes(xmlw, "IDNo", persistentProtocol + ":" + persistentAuthority + "/" + persistentId,
                XmlAttribute.of("agency", persistentAgency));
    }

    private void writeOtherIdElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.otherId)) {

            String otherId = extractFieldWithTypeAsString(foo, DatasetFieldConstant.otherIdValue);
            String otherIdAgency = extractFieldWithTypeAsString(foo, DatasetFieldConstant.otherIdAgency);

            writeFullElementWithAttributes(xmlw, "IDNo", otherId,
                    XmlAttribute.of("agency", otherIdAgency));
        }
    }

    private void writeAuthorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        xmlw.writeStartElement("rspStmt");

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.author)) {
            String authorName = extractFieldWithTypeAsString(foo, DatasetFieldConstant.authorName);
            String authorAffiliation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.authorAffiliation);

            writeFullElementWithAttributes(xmlw, "AuthEnty", authorName,
                    XmlAttribute.of("affiliation", authorAffiliation));
        }

        xmlw.writeEndElement(); //rspStmt
    }

    private void writeProducersElement(XMLStreamWriter xmlw, DatasetVersionDTO version) throws XMLStreamException {

        xmlw.writeStartElement("prodStmt");

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(version, DatasetFieldConstant.producer)) {

            String producerName = extractFieldWithTypeAsString(foo, DatasetFieldConstant.producerName);
            String producerAffiliation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.producerAffiliation);
            String producerAbbreviation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.producerAbbreviation);
            String producerLogo = extractFieldWithTypeAsString(foo, DatasetFieldConstant.producerLogo);

            writeFullElementWithAttributes(xmlw, "producer", producerName,
                    XmlAttribute.of("affiliation", producerAffiliation),
                    XmlAttribute.of("abbr", producerAbbreviation),
                    XmlAttribute.of("role", producerLogo));
        }

        writeFullElement(xmlw, "prodDate", dto2Primitive(version, DatasetFieldConstant.productionDate));
        writeFullElement(xmlw, "prodPlac", dto2Primitive(version, DatasetFieldConstant.productionPlace));
        writeSoftwareElement(xmlw, version);

        writeGrantElement(xmlw, version);
        xmlw.writeEndElement(); //prodStmt
    }

    private void writeSoftwareElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.software)) {

            String softwareName = extractFieldWithTypeAsString(foo, DatasetFieldConstant.softwareName);
            String softwareVersion = extractFieldWithTypeAsString(foo, DatasetFieldConstant.softwareVersion);

            writeFullElementWithAttributes(xmlw, "software", softwareName,
                    XmlAttribute.of("version", softwareVersion));
        }
    }

    private void writeGrantElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.grantNumber)) {

            String grantNumber = extractFieldWithTypeAsString(foo, DatasetFieldConstant.grantNumberValue);
            String grantAgency = extractFieldWithTypeAsString(foo, DatasetFieldConstant.grantNumberAgency);

            writeFullElementWithAttributes(xmlw, "grantNo", grantNumber,
                    XmlAttribute.of("agency", grantAgency));
        }
    }

    private void writeDistributorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.distributor)) {

            String distributorName = extractFieldWithTypeAsString(foo, DatasetFieldConstant.distributorName);
            String distributorAffiliation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.distributorAffiliation);
            String distributorAbbreviation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.distributorAbbreviation);
            String distributorURL = extractFieldWithTypeAsString(foo, DatasetFieldConstant.distributorURL);

            writeFullElementWithAttributes(xmlw, "distrbtr", distributorName,
                    XmlAttribute.of("affiliation", distributorAffiliation),
                    XmlAttribute.of("abbr", distributorAbbreviation),
                    XmlAttribute.of("URI", distributorURL));
        }
    }

    private void writeContactsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.datasetContact)) {

            String datasetContactName = extractFieldWithTypeAsString(foo, DatasetFieldConstant.datasetContactName);
            String datasetContactAffiliation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.datasetContactAffiliation);
            String datasetContactEmail = extractFieldWithTypeAsString(foo, DatasetFieldConstant.datasetContactEmail);

            // TODO: Since datasetContactEmail is a required field but datasetContactName is not consider not checking if datasetContactName is empty so we can write out datasetContactEmail.
            writeFullElementWithAttributes(xmlw, "contact", datasetContactName,
                    XmlAttribute.of("affiliation", datasetContactAffiliation),
                    XmlAttribute.of("email", datasetContactEmail));
        }
    }

    private void writeSeriesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        Set<DatasetFieldDTO> childrenOfSeries = extractChildrenOfSingleCompoundField(datasetVersionDTO, DatasetFieldConstant.series);

        if (!childrenOfSeries.isEmpty()) {
            xmlw.writeStartElement("serStmt");
            writeFullElement(xmlw, "serName", extractFieldWithTypeAsString(childrenOfSeries, DatasetFieldConstant.seriesName));
            writeFullElement(xmlw, "serInfo", extractFieldWithTypeAsString(childrenOfSeries, DatasetFieldConstant.seriesInformation));
            xmlw.writeEndElement();
        }
    }

    private void writeStdyInfo(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("stdyInfo");

        writeSubjectElement(xmlw, version); //Subject and Keywords
        writeAbstractElement(xmlw, version); // Description

        writeSummaryDescriptionElement(xmlw, version);
        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.notesText));

        xmlw.writeEndElement();
    }

    private void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        //Key Words and Topic Classification

        xmlw.writeStartElement("subject");

        writeFullElementList(xmlw, "keyword", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.subject));

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.keyword)) {

            String keywordValue = extractFieldWithTypeAsString(foo, DatasetFieldConstant.keywordValue);
            String keywordVocab = extractFieldWithTypeAsString(foo, DatasetFieldConstant.keywordVocab);
            String keywordURI = extractFieldWithTypeAsString(foo, DatasetFieldConstant.keywordVocabURI);

            writeFullElementWithAttributes(xmlw, "keyword", keywordValue,
                    XmlAttribute.of("vocab", keywordVocab),
                    XmlAttribute.of("vocabURI", keywordURI));

        }

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.topicClassification)) {

            String topicClassificationValue = extractFieldWithTypeAsString(foo, DatasetFieldConstant.topicClassValue);
            String topicClassificationVocab = extractFieldWithTypeAsString(foo, DatasetFieldConstant.topicClassVocab);
            String topicClassificationURI = extractFieldWithTypeAsString(foo, DatasetFieldConstant.topicClassVocabURI);

            writeFullElementWithAttributes(xmlw, "topcClas", topicClassificationValue,
                    XmlAttribute.of("vocab", topicClassificationVocab),
                    XmlAttribute.of("vocabURI", topicClassificationURI));
        }

        xmlw.writeEndElement(); // subject
    }

    private void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.description)) {

            String descriptionText = extractFieldWithTypeAsString(foo, DatasetFieldConstant.descriptionText);
            String descriptionDate = extractFieldWithTypeAsString(foo, DatasetFieldConstant.descriptionDate);

            writeFullElementWithAttributes(xmlw, "abstract", descriptionText,
                    XmlAttribute.of("date", descriptionDate));
        }
    }

    private void writeSummaryDescriptionElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("sumDscr");

        Integer per = 0;
        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.timePeriodCovered)) {
            String dateValStart = extractFieldWithTypeAsString(foo, DatasetFieldConstant.timePeriodCoveredStart);
            String dateValEnd = extractFieldWithTypeAsString(foo, DatasetFieldConstant.timePeriodCoveredEnd);
            per++;

            writeDateElement(xmlw, "timePrd", "P" + per.toString(), "start", dateValStart);
            writeDateElement(xmlw, "timePrd", "P" + per.toString(), "end", dateValEnd);
        }

        Integer coll = 0;
        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.dateOfCollection)) {
            String dateValStart = extractFieldWithTypeAsString(foo, DatasetFieldConstant.dateOfCollectionStart);
            String dateValEnd = extractFieldWithTypeAsString(foo, DatasetFieldConstant.dateOfCollectionEnd);
            coll++;

            writeDateElement(xmlw, "collDate", "P" + coll.toString(), "start", dateValStart);
            writeDateElement(xmlw, "collDate", "P" + coll.toString(), "end", dateValEnd);
        }


        List<Set<DatasetFieldDTO>> childrenOfGeographicCoverages = extractChildrenOfMultipleCompoundField(
                datasetVersionDTO, DatasetFieldConstant.geographicCoverage);

        for (Set<DatasetFieldDTO> foo : childrenOfGeographicCoverages) {
            writeFullElement(xmlw, "nation", extractFieldWithTypeAsString(foo, DatasetFieldConstant.country));
        }

        for (Set<DatasetFieldDTO> foo : childrenOfGeographicCoverages) {

            writeFullElement(xmlw, "geogCover", extractFieldWithTypeAsString(foo, DatasetFieldConstant.city));
            writeFullElement(xmlw, "geogCover", extractFieldWithTypeAsString(foo, DatasetFieldConstant.state));
            writeFullElement(xmlw, "geogCover", extractFieldWithTypeAsString(foo, DatasetFieldConstant.otherGeographicCoverage));

        }

        writeFullElementList(xmlw, "geogUnit", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.geographicUnit));

        Set<DatasetFieldDTO> childrenOfFirstGeographicBoundingBox = extractChildrenOfMultipleCompoundField(
                datasetVersionDTO, DatasetFieldConstant.geographicBoundingBox)
                .stream().findFirst().orElse(Collections.emptySet());

        if (!childrenOfFirstGeographicBoundingBox.isEmpty()) {

            xmlw.writeStartElement("geoBndBox");

            writeFullElement(xmlw, "westBL", extractFieldWithTypeAsString(childrenOfFirstGeographicBoundingBox, DatasetFieldConstant.westLongitude));
            writeFullElement(xmlw, "eastBL", extractFieldWithTypeAsString(childrenOfFirstGeographicBoundingBox, DatasetFieldConstant.eastLongitude));
            writeFullElement(xmlw, "southBL", extractFieldWithTypeAsString(childrenOfFirstGeographicBoundingBox, DatasetFieldConstant.southLatitude));
            writeFullElement(xmlw, "northBL", extractFieldWithTypeAsString(childrenOfFirstGeographicBoundingBox, DatasetFieldConstant.northLatitude));

            xmlw.writeEndElement();

        }


        writeFullElementList(xmlw, "anlyUnit", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.unitOfAnalysis));

        writeFullElementList(xmlw, "universe", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.universe));

        writeFullElementList(xmlw, "dataKind", dto2PrimitiveList(datasetVersionDTO, DatasetFieldConstant.kindOfData));

        xmlw.writeEndElement(); //sumDscr
    }

    private void writeMethodElement(XMLStreamWriter xmlw, DatasetVersionDTO version,
                                           Map<String, Map<String, String>> localizedVocabularyIndex) throws XMLStreamException {
        xmlw.writeStartElement("method");

        writeDataColl(xmlw, version, localizedVocabularyIndex);

        writeNotesElement(xmlw, version);

        writeAnlyInfo(xmlw, version);

        xmlw.writeEndElement();//method
    }

    private void writeDataColl(XMLStreamWriter xmlw, DatasetVersionDTO version, Map<String, Map<String, String>> localizedVocabularyIndex) throws XMLStreamException {
        xmlw.writeStartElement("dataColl");

        writeFullElementList(xmlw, "timeMeth",
                dtoVocab2LocalizedPrimitiveList(version, DatasetFieldConstant.timeMethod, localizedVocabularyIndex));

        writeFullElement(xmlw, "dataCollector", dto2Primitive(version, DatasetFieldConstant.dataCollector));
        writeFullElement(xmlw, "collectorTraining", dto2Primitive(version, DatasetFieldConstant.collectorTraining));
        writeFullElement(xmlw, "frequenc", dto2Primitive(version, DatasetFieldConstant.frequencyOfDataCollection));

        writeFullElement(xmlw, "sampProc", StringUtils.join(
                dtoVocab2LocalizedPrimitiveList(version, DatasetFieldConstant.samplingProcedure, localizedVocabularyIndex), ", "));

        writeTargetSampleElement(xmlw, version);

        writeFullElement(xmlw, "deviat", dto2Primitive(version, DatasetFieldConstant.deviationsFromSampleDesign));

        writeFullElementList(xmlw, "collMode",
                dtoVocab2LocalizedPrimitiveList(version, DatasetFieldConstant.collectionMode, localizedVocabularyIndex));

        writeFullElement(xmlw, "resInstru", StringUtils.join(
                dtoVocab2LocalizedPrimitiveList(version, DatasetFieldConstant.researchInstrument, localizedVocabularyIndex), ", "));

        writeSources(xmlw, version);

        writeFullElement(xmlw, "collSitu", dto2Primitive(version, DatasetFieldConstant.dataCollectionSituation));
        writeFullElement(xmlw, "actMin", dto2Primitive(version, DatasetFieldConstant.actionsToMinimizeLoss));
        writeFullElement(xmlw, "ConOps", dto2Primitive(version, DatasetFieldConstant.controlOperations));

        writeFullElementList(xmlw, "weight",
                dtoVocab2LocalizedPrimitiveList(version, DatasetFieldConstant.weighting, localizedVocabularyIndex));

        writeFullElement(xmlw, "cleanOps", dto2Primitive(version, DatasetFieldConstant.cleaningOperations));

        xmlw.writeEndElement(); //dataColl
    }

    private void writeTargetSampleElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        Set<DatasetFieldDTO> childrenOfTargetSampleSize = extractChildrenOfSingleCompoundField(datasetVersionDTO, DatasetFieldConstant.targetSampleSize);

        if (!childrenOfTargetSampleSize.isEmpty()) {
            xmlw.writeStartElement("targetSampleSize");
            writeFullElement(xmlw, "sampleSizeFormula", extractFieldWithTypeAsString(childrenOfTargetSampleSize, DatasetFieldConstant.targetSampleSizeFormula));
            writeFullElement(xmlw, "sampleSize", extractFieldWithTypeAsString(childrenOfTargetSampleSize, DatasetFieldConstant.targetSampleActualSize));
            xmlw.writeEndElement();
        }
    }

    private void writeSources(XMLStreamWriter xmlw, DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("sources");
        writeFullElementList(xmlw, "dataSrc", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        writeFullElement(xmlw, "srcOrig", dto2Primitive(version, DatasetFieldConstant.originOfSources));
        writeFullElement(xmlw, "srcChar", dto2Primitive(version, DatasetFieldConstant.characteristicOfSources));
        writeFullElement(xmlw, "srcDocu", dto2Primitive(version, DatasetFieldConstant.accessToSources));
        xmlw.writeEndElement(); //sources
    }

    private void writeNotesElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        Set<DatasetFieldDTO> childrenOfSocialNotes = extractChildrenOfSingleCompoundField(datasetVersionDTO, DatasetFieldConstant.socialScienceNotes);

        String notesText = extractFieldWithTypeAsString(childrenOfSocialNotes, DatasetFieldConstant.socialScienceNotesText);
        String notesType = extractFieldWithTypeAsString(childrenOfSocialNotes, DatasetFieldConstant.socialScienceNotesType);
        String notesSubject = extractFieldWithTypeAsString(childrenOfSocialNotes, DatasetFieldConstant.socialScienceNotesSubject);

        writeFullElementWithAttributes(xmlw, "notes", notesText,
                XmlAttribute.of("type", notesType),
                XmlAttribute.of("subject", notesSubject));
    }

    private void writeAnlyInfo(XMLStreamWriter xmlw, DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("anlyInfo");
        writeFullElement(xmlw, "respRate", dto2Primitive(version, DatasetFieldConstant.responseRate));
        writeFullElement(xmlw, "EstSmpErr", dto2Primitive(version, DatasetFieldConstant.samplingErrorEstimates));
        writeFullElement(xmlw, "dataAppr", dto2Primitive(version, DatasetFieldConstant.otherDataAppraisal));
        xmlw.writeEndElement(); //anlyInfo
    }

    private void writeOtherStudyMaterial(XMLStreamWriter xmlw , DatasetVersionDTO version) throws XMLStreamException {
        xmlw.writeStartElement("othrStdyMat");
        writeRelMatElement(xmlw, version);
        writeRelStdyElement(xmlw, version);
        writeRelPublElement(xmlw, version);
        writeFullElementList(xmlw, "othRefs", dto2PrimitiveList(version, DatasetFieldConstant.otherReferences));
        xmlw.writeEndElement(); //othrStdyMat
    }

    private void writeRelPublElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> foo : extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.publication)) {
            String citation = extractFieldWithTypeAsString(foo, DatasetFieldConstant.publicationCitation);
            String idType = extractFieldWithTypeAsString(foo, DatasetFieldConstant.publicationIDType);
            String idNumber = extractFieldWithTypeAsString(foo, DatasetFieldConstant.publicationIDNumber);
            String url = extractFieldWithTypeAsString(foo, DatasetFieldConstant.publicationURL);

            writeFullRelationElement(xmlw, "relPubl", citation, idType, idNumber, url);
        }
    }

    private void writeRelMatElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> relatedMaterial: extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.relatedMaterial)) {

            String citation = extractFieldWithTypeAsString(relatedMaterial, DatasetFieldConstant.relatedMaterialCitation);
            String idType = extractFieldWithTypeAsString(relatedMaterial, DatasetFieldConstant.relatedMaterialIDType);
            String idNumber = extractFieldWithTypeAsString(relatedMaterial, DatasetFieldConstant.relatedMaterialIDNumber);
            String url = extractFieldWithTypeAsString(relatedMaterial, DatasetFieldConstant.relatedMaterialURL);

            writeFullRelationElement(xmlw, "relMat", citation, idType, idNumber, url);
        }

    }

    private void writeRelStdyElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {

        for (Set<DatasetFieldDTO> relatedDataset: extractChildrenOfMultipleCompoundField(datasetVersionDTO, DatasetFieldConstant.relatedDataset)) {

            String citation = extractFieldWithTypeAsString(relatedDataset, DatasetFieldConstant.relatedDatasetCitation);
            String idType = extractFieldWithTypeAsString(relatedDataset, DatasetFieldConstant.relatedDatasetIDType);
            String idNumber = extractFieldWithTypeAsString(relatedDataset, DatasetFieldConstant.relatedDatasetIDNumber);
            String url = extractFieldWithTypeAsString(relatedDataset, DatasetFieldConstant.relatedDatasetURL);

            writeFullRelationElement(xmlw, "relStdy", citation, idType, idNumber, url);
        }

    }

    private void writeFullRelationElement(XMLStreamWriter xmlw, String relationTag, String citation, String idType, String idNumber, String url) throws XMLStreamException {
        if (StringUtils.isNotEmpty(citation)) {
            xmlw.writeStartElement(relationTag);
            xmlw.writeStartElement("citation");
            if (StringUtils.isNotBlank(idNumber)) {
                xmlw.writeStartElement("titlStmt");
                xmlw.writeEmptyElement("titl");
                writeFullElementWithAttributes(xmlw, "IDNo", idNumber,
                        XmlAttribute.of("agency", idType));
                xmlw.writeEndElement(); // titlStmt
            }

            writeFullElement(xmlw,"biblCit",citation);
            xmlw.writeEndElement(); //citation
            writeFullAttributesOnlyElement(xmlw, "ExtLink", XmlAttribute.of("URI", url));
            xmlw.writeEndElement(); //relationTag
        }
    }

    private void createOtherMatsFromFileDtos(XMLStreamWriter xmlw, List<FileMetadataDTO> files) throws XMLStreamException {
        for (FileMetadataDTO file: files) {
            ddiFileWriter.writeOtherMatFromFileDto(xmlw, file);
        }
    }

    private void createOtherMatsFromFileMetadatas(XMLStreamWriter xmlw, List<FileMetadata> files) throws XMLStreamException {
        List<FileMetadata> nonTabularFiles = files.stream().filter(file -> !file.getDataFile().isTabularData()).collect(toList());
        for (FileMetadata file: nonTabularFiles) {
            ddiFileWriter.writeOtherMatFromFileMetadata(xmlw, file);
        }
    }

    private void createFileDscr(XMLStreamWriter xmlw, List<FileMetadata> files) throws XMLStreamException {
        List<FileMetadata> tabularFiles = files.stream().filter(file -> file.getDataFile().isTabularData()).collect(toList());
        for (FileMetadata file: tabularFiles) {
            ddiFileWriter.writeFileDscr(xmlw, file);
        }
    }

    private void createDataDscr(XMLStreamWriter xmlw, List<FileMetadata> files) throws XMLStreamException {
        List<FileMetadata> tabularFiles = files.stream().filter(file -> file.getDataFile().isTabularData()).collect(toList());

        if (!tabularFiles.isEmpty()) {
            xmlw.writeStartElement("dataDscr");
            for (FileMetadata file : tabularFiles) {
                for (DataVariable variable : file.getDataFile().getDataTable().getDataVariables()) {
                    ddiVariableWriter.createVarDDI(xmlw, variable, file);
                }
            }
            xmlw.writeEndElement();
        }
    }


    private void writeDateElement(XMLStreamWriter xmlw, String element, String cycle, String event, String dateIn) throws XMLStreamException {
        writeFullElementWithAttributes(xmlw, element, dateIn,
                XmlAttribute.of("cycle", cycle),
                XmlAttribute.of("event", event),
                XmlAttribute.of("date", dateIn));
    }

    private String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        return findFieldWithType(datasetVersionDTO, datasetFieldTypeName)
                .map(DatasetFieldDTO::getSinglePrimitive)
                .orElse(StringUtils.EMPTY);
    }

    private List<String> dto2PrimitiveList(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        return findFieldWithType(datasetVersionDTO, datasetFieldTypeName)
                .map(DatasetFieldDTO::getMultiplePrimitive)
                .orElse(Collections.emptyList());
    }

    private List<String> dtoVocab2LocalizedPrimitiveList(DatasetVersionDTO version, String datasetFieldType,
                                            Map<String, Map<String, String>> localizedVocabularyIndex) {
        return getValuesAsList(version, datasetFieldType).stream()
                .map(value -> extractLocalizedVocabularyValue(localizedVocabularyIndex, datasetFieldType, value))
                .collect(toList());
    }

    private String extractLocalizedVocabularyValue(Map<String, Map<String, String>> localizedVocabularyIndex,
                                            String fieldType, String vocabularyKey) {
        return Optional.ofNullable(localizedVocabularyIndex.get(fieldType))
                        .map(i -> i.get(vocabularyKey))
                        .orElse(vocabularyKey);
    }

    private List<String> getValuesAsList(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {

        return findFieldWithType(datasetVersionDTO, datasetFieldTypeName)
                .map(f -> f.getMultiple()
                        ? f.getMultiplePrimitive()
                        : Lists.newArrayList(f.getSinglePrimitive()))
                .orElse(Collections.emptyList());
    }

    private List<Set<DatasetFieldDTO>> extractChildrenOfMultipleCompoundField(DatasetVersionDTO datasetVersionDTO, String fieldType) {
        return findFieldWithType(datasetVersionDTO, fieldType)
                .map(DatasetFieldDTO::getMultipleCompound)
                .orElse(Collections.emptyList());
    }

    private Set<DatasetFieldDTO> extractChildrenOfSingleCompoundField(DatasetVersionDTO datasetVersionDTO, String fieldType) {
        return findFieldWithType(datasetVersionDTO, fieldType)
                .map(DatasetFieldDTO::getSingleCompound)
                .orElse(Collections.emptySet());
    }

    private String extractFieldWithTypeAsString(Set<DatasetFieldDTO> compoundFieldChildren, String fieldType) {
        return compoundFieldChildren.stream()
                .filter(f -> f.getTypeName().equals(fieldType))
                .map(DatasetFieldDTO::getSinglePrimitive)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }

    private Optional<DatasetFieldDTO> findFieldWithType(DatasetVersionDTO datasetVersionDTO, String fieldType) {
        return datasetVersionDTO.getMetadataBlocks().values().stream()
                .flatMap(metadataBlock -> metadataBlock.getFields().stream())
                .filter(field -> field.getTypeName().equals(fieldType))
                .findFirst();
    }


}
