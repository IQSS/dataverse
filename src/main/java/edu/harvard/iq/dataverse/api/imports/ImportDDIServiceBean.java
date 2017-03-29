package edu.harvard.iq.dataverse.api.imports;

import static edu.harvard.iq.dataverse.export.ddi.DdiExportUtil.NOTE_TYPE_CONTENTTYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringUtils;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.api.dto.DataFileDTO;
import edu.harvard.iq.dataverse.api.dto.DataTableDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.util.StringUtil;

/**
 *
 * @author ellenk
 */
// TODO:
// does this need to be a service bean/stateless? - could be transformed into
// a util with static methods.
// (it would need to be passed the fields service beans as arguments)
// -- L.A. 4.5
@Stateless
public class ImportDDIServiceBean {
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
    public static final String NOTE_SUBJECT_VERSION_NOTE = "Version Note";

    public static final String NOTE_TYPE_ARCHIVE_NOTE = "DVN:ARCHIVE_NOTE";
    public static final String NOTE_SUBJECT_ARCHIVE_NOTE = "Archive Note";

    public static final String NOTE_TYPE_ARCHIVE_DATE = "DVN:ARCHIVE_DATE";
    public static final String NOTE_SUBJECT_ARCHIVE_DATE = "Archive Date";

    public static final String NOTE_TYPE_EXTENDED_METADATA = "DVN:EXTENDED_METADATA";

    public static final String NOTE_TYPE_LOCKSS_CRAWL = "LOCKSS:CRAWLING";
    public static final String NOTE_SUBJECT_LOCKSS_PERM = "LOCKSS Permission";

    public static final String NOTE_TYPE_REPLICATION_FOR = "DVN:REPLICATION_FOR";
    private XMLInputFactory xmlInputFactory = null;

    @EJB
    CustomFieldServiceBean customFieldService;

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    // TODO:
    // stop passing the xml source as a string; (it could be huge!) -- L.A. 4.5
    public DatasetDTO doImport(final ImportType importType, final String xmlToParse)
        throws XMLStreamException, ImportException {
        xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);
        final DatasetDTO datasetDTO = this.initializeDataset();

        // Read docDescr and studyDesc into DTO objects.
        // TODO: the fileMap is likely not needed.
        final Map fileMap = mapDDI(importType, xmlToParse, datasetDTO);
        if (!isMigrationImport(importType)) {
            // For migration, this filemetadata is copied in a separate SQL step
        }
        return datasetDTO;
    }

    public void importFileMetadata(final DatasetVersion dv, final String xmlToParse) {

    }

    private boolean isHarvestImport(final ImportType importType) {
        return importType.equals(ImportType.HARVEST) || importType.equals(ImportType.HARVEST_WITH_FILES);
    }

    private boolean isHarvestWithFilesImport(final ImportType importType) {
        return importType.equals(ImportType.HARVEST_WITH_FILES);
    }

    private boolean isNewImport(final ImportType importType) {
        return importType.equals(ImportType.NEW);
    }

    private boolean isMigrationImport(final ImportType importType) {
        return importType.equals(ImportType.MIGRATION);
    }

    public Map mapDDI(final ImportType importType, final String xmlToParse, final DatasetDTO datasetDTO)
        throws XMLStreamException, ImportException {

        final Map filesMap = new HashMap();
        final StringReader reader = new StringReader(xmlToParse);
        XMLStreamReader xmlr = null;
        final XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlr = xmlFactory.createXMLStreamReader(reader);
        processDDI(importType, xmlr, datasetDTO, filesMap);

        return filesMap;
    }

    public Map mapDDI(final ImportType importType, final File ddiFile, final DatasetDTO datasetDTO)
        throws ImportException {
        FileInputStream in = null;
        XMLStreamReader xmlr = null;
        final Map filesMap = new HashMap();

        try {
            in = new FileInputStream(ddiFile);
            xmlr = xmlInputFactory.createXMLStreamReader(in);
            processDDI(importType, xmlr, datasetDTO, filesMap);
        } catch (final FileNotFoundException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI: File Not Found!");
        } catch (final XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI.", ex);
        } finally {
            try {
                if (xmlr != null) {
                    xmlr.close();
                }
            } catch (final XMLStreamException ex) {
            }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ex) {
            }
        }

        return filesMap;
    }

    private void processDDI(final ImportType importType, final XMLStreamReader xmlr, final DatasetDTO datasetDTO,
        final Map filesMap) throws XMLStreamException, ImportException {

        // make sure we have a codeBook
        // while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
        xmlr.nextTag();
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, "codeBook");

        // Some DDIs provide an ID in the <codeBook> section.
        // We are going to treat it as just another otherId.
        // (we've seen instances where this ID was the only ID found in
        // in a harvested DDI).

        final String codeBookLevelId = xmlr.getAttributeValue(null, "ID");

        // (but first we will parse and process the entire DDI - and only
        // then add this codeBook-level id to the list of identifiers; i.e.,
        // we don't want it to be the first on the list, if one or more
        // ids are available in the studyDscr section - those should take
        // precedence!)
        // In fact, we should only use these IDs when no ID is available down
        // in the study description section!

        processCodeBook(importType, xmlr, datasetDTO, filesMap);
        final MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");

        if (codeBookLevelId != null && !codeBookLevelId.equals("")) {
            if (citationBlock.getField("otherId") == null) {
                // this means no ids were found during the parsing of the
                // study description section. we'll use the one we found in
                // the codeBook entry:
                final FieldDTO otherIdValue = FieldDTO.createPrimitiveFieldDTO("otherIdValue", codeBookLevelId);
                final FieldDTO otherId = FieldDTO.createCompoundFieldDTO("otherId", otherIdValue);
                citationBlock.getFields().add(otherId);

            }
        }
        if (isHarvestImport(importType)) {
            datasetDTO.getDatasetVersion().setVersionState(VersionState.RELEASED);

        }

    }

    public DatasetDTO initializeDataset() {
        final DatasetDTO datasetDTO = new DatasetDTO();
        final DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
        datasetDTO.setDatasetVersion(datasetVersionDTO);
        final HashMap<String, MetadataBlockDTO> metadataBlocks = new HashMap<>();
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
    private void processCodeBook(final ImportType importType, final XMLStreamReader xmlr, final DatasetDTO datasetDTO,
        final Map filesMap) throws XMLStreamException, ImportException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("docDscr")) {
                    processDocDscr(xmlr, datasetDTO);
                } else if (xmlr.getLocalName().equals("stdyDscr")) {
                    processStdyDscr(importType, xmlr, datasetDTO);
                } else if (xmlr.getLocalName().equals("fileDscr") /* && !isMigrationImport(importType) */) {
                    // EMK TODO: add this back in for ImportType.NEW
                    // XXX: add it for migration import, experimental!
                    processFileDscr(xmlr, datasetDTO, filesMap);

                } else if (xmlr.getLocalName().equals("otherMat")
                    && (isNewImport(importType) || isHarvestWithFilesImport(importType))) {
                    processOtherMat(xmlr, datasetDTO, filesMap);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("codeBook"))
                    return;
            }
        }
    }

    private void processDocDscr(final XMLStreamReader xmlr, final DatasetDTO datasetDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {

                if (xmlr.getLocalName().equals("IDNo") && StringUtil.isEmpty(datasetDTO.getIdentifier())) {
                    // this will set a StudyId if it has not yet been set; it will get overridden by a metadata
                    // id in the StudyDscr section, if one exists
                    if (AGENCY_HANDLE.equals(xmlr.getAttributeValue(null, "agency"))) {
                        parseStudyIdHandle(parseText(xmlr), datasetDTO);
                    }
                    // EMK TODO: we need to save this somewhere when we add harvesting infrastructure
                } /*
                   * else if ( xmlr.getLocalName().equals("holdings") &&
                   * StringUtil.isEmpty(datasetDTO..getHarvestHoldings()) ) { metadata.setHarvestHoldings(
                   * xmlr.getAttributeValue(null, "URI") ); }
                   */
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("docDscr"))
                    return;
            }
        }
    }

    private String parseText(final XMLStreamReader xmlr) throws XMLStreamException {
        return parseText(xmlr, true);
    }

    private String parseText(final XMLStreamReader xmlr, final boolean scrubText) throws XMLStreamException {
        String tempString = getElementText(xmlr);
        if (scrubText) {
            tempString = tempString.trim().replace('\n', ' ');
        }
        return tempString;
    }

    private String parseDate(final XMLStreamReader xmlr, final String endTag) throws XMLStreamException {
        String date = xmlr.getAttributeValue(null, "date");
        if (date == null) {
            date = parseText(xmlr);
        }
        return date;
    }

    /*
     * We had to add this method because the ref getElementText has a bug where it would append a null before the text,
     * if there was an escaped apostrophe; it appears that the code finds an null ENTITY_REFERENCE in this case which
     * seems like a bug; the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
    private String getElementText(final XMLStreamReader xmlr) throws XMLStreamException {
        if (xmlr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", xmlr.getLocation());
        }
        int eventType = xmlr.next();
        final StringBuffer content = new StringBuffer();
        while (eventType != XMLStreamConstants.END_ELEMENT) {
            if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.SPACE
            /* || eventType == XMLStreamConstants.ENTITY_REFERENCE */) {
                content.append(xmlr.getText());
            } else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType == XMLStreamConstants.COMMENT
                || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                // skipping
            } else if (eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", xmlr.getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type " + eventType, xmlr.getLocation());
            }
            eventType = xmlr.next();
        }
        return content.toString();
    }

    private void processStdyDscr(final ImportType importType, final XMLStreamReader xmlr, final DatasetDTO datasetDTO)
        throws XMLStreamException, ImportException {

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("citation"))
                    processCitation(importType, xmlr, datasetDTO);
                else if (xmlr.getLocalName().equals("stdyInfo"))
                    processStdyInfo(xmlr, datasetDTO.getDatasetVersion());
                else if (xmlr.getLocalName().equals("method"))
                    processMethod(xmlr, datasetDTO.getDatasetVersion());

                else if (xmlr.getLocalName().equals("dataAccs"))
                    processDataAccs(xmlr, datasetDTO.getDatasetVersion());

                else if (xmlr.getLocalName().equals("othrStdyMat"))
                    processOthrStdyMat(xmlr, datasetDTO.getDatasetVersion());
                else if (xmlr.getLocalName().equals("notes"))
                    processNotes(xmlr, datasetDTO.getDatasetVersion());

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("stdyDscr"))
                    return;
            }
        }
    }

    private void processOthrStdyMat(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO)
        throws XMLStreamException {
        final List<HashSet<FieldDTO>> publications = new ArrayList<>();
        final boolean replicationForFound = false;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("relMat")) {
                    // this code is still here to handle imports from old DVN created ddis
                    if (!replicationForFound && REPLICATION_FOR_TYPE.equals(xmlr.getAttributeValue(null, "type"))) {
                        if (!SOURCE_DVN_3_0.equals(xmlr.getAttributeValue(null, "source"))) {
                            // this is a ddi from pre 3.0, so we should add a publication
                            /*
                             * StudyRelPublication rp = new StudyRelPublication();
                             * metadata.getStudyRelPublications().add(rp); rp.setMetadata(metadata); rp.setText(
                             * parseText( xmlr, "relMat" ) ); rp.setReplicationData(true); replicationForFound = true;
                             */
                            final HashSet<FieldDTO> set = new HashSet<>();
                            addToSet(set, DatasetFieldConstant.publicationCitation, parseText(xmlr, "relMat"));
                            if (!set.isEmpty()) {
                                publications.add(set);
                            }
                            if (publications.size() > 0)
                                getCitation(dvDTO).addField(FieldDTO
                                    .createMultipleCompoundFieldDTO(DatasetFieldConstant.publication, publications));
                        }
                    } else {

                        final List<String> relMaterial = new ArrayList<>();
                        relMaterial.add(parseText(xmlr, "relMat"));
                        getCitation(dvDTO).addField(FieldDTO
                            .createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedMaterial, relMaterial));
                    }
                } else if (xmlr.getLocalName().equals("relStdy")) {
                    final List<String> relStudy = new ArrayList<>();
                    relStudy.add(parseText(xmlr, "relStdy"));
                    getCitation(dvDTO).addField(
                        FieldDTO.createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedDatasets, relStudy));
                } else if (xmlr.getLocalName().equals("relPubl")) {
                    final HashSet<FieldDTO> set = new HashSet<>();

                    // call new parse text logic
                    final Object rpFromDDI = parseTextNew(xmlr, "relPubl");
                    if (rpFromDDI instanceof Map) {
                        final Map rpMap = (Map) rpFromDDI;
                        addToSet(set, DatasetFieldConstant.publicationCitation, (String) rpMap.get("text"));
                        addToSet(set, DatasetFieldConstant.publicationIDNumber, (String) rpMap.get("idNumber"));
                        addToSet(set, DatasetFieldConstant.publicationURL, (String) rpMap.get("url"));
                        if (rpMap.get("idType") != null) {
                            set.add(FieldDTO.createVocabFieldDTO(DatasetFieldConstant.publicationIDType,
                                ((String) rpMap.get("idType")).toLowerCase()));
                        }
                        // rp.setText((String) rpMap.get("text"));
                        // rp.setIdType((String) rpMap.get("idType"));
                        // rp.setIdNumber((String) rpMap.get("idNumber"));
                        // rp.setUrl((String) rpMap.get("url"));
                        // TODO: ask about where/whether we want to save this
                        // if (!replicationForFound && rpMap.get("replicationData") != null) {
                        // rp.setReplicationData(true);
                        /// replicationForFound = true;
                        // }
                    } else {
                        addToSet(set, DatasetFieldConstant.publicationCitation, (String) rpFromDDI);
                        // rp.setText( (String) rpFromDDI );
                    }
                    publications.add(set);
                    if (publications.size() > 0) {
                        getCitation(dvDTO).addField(
                            FieldDTO.createMultipleCompoundFieldDTO(DatasetFieldConstant.publication, publications));
                    }

                } else if (xmlr.getLocalName().equals("otherRefs")) {

                    final List<String> otherRefs = new ArrayList<>();
                    otherRefs.add(parseText(xmlr, "otherRefs"));
                    getCitation(dvDTO).addField(
                        FieldDTO.createMultiplePrimitiveFieldDTO(DatasetFieldConstant.otherReferences, otherRefs));

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {

                if (xmlr.getLocalName().equals("othrStdyMat")) {
                    return;
                }
            }
        }
    }

    private void processCitation(final ImportType importType, final XMLStreamReader xmlr, final DatasetDTO datasetDTO)
        throws XMLStreamException, ImportException {
        final DatasetVersionDTO dvDTO = datasetDTO.getDatasetVersion();
        final MetadataBlockDTO citation = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("titlStmt"))
                    processTitlStmt(xmlr, datasetDTO);
                else if (xmlr.getLocalName().equals("rspStmt"))
                    processRspStmt(xmlr, citation);
                else if (xmlr.getLocalName().equals("prodStmt"))
                    processProdStmt(xmlr, citation);
                else if (xmlr.getLocalName().equals("distStmt"))
                    processDistStmt(xmlr, citation);
                else if (xmlr.getLocalName().equals("serStmt"))
                    processSerStmt(xmlr, citation);
                else if (xmlr.getLocalName().equals("verStmt"))
                    processVerStmt(importType, xmlr, dvDTO);
                else if (xmlr.getLocalName().equals("notes")) {
                    final String _note = parseNoteByType(xmlr, NOTE_TYPE_UNF);
                    if (_note != null) {
                        datasetDTO.getDatasetVersion().setUNF(parseUNF(_note));
                    } else {

                        processNotes(xmlr, dvDTO);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation"))
                    return;
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
    private void processStdyInfo(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        final List<HashSet<FieldDTO>> descriptions = new ArrayList<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("subject")) {
                    processSubject(xmlr, getCitation(dvDTO));
                } else if (xmlr.getLocalName().equals("abstract")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "dsDescriptionDate", xmlr.getAttributeValue(null, "date"));
                    addToSet(set, "dsDescriptionValue", parseText(xmlr, "abstract"));
                    if (!set.isEmpty()) {
                        descriptions.add(set);
                    }

                } else if (xmlr.getLocalName().equals("sumDscr"))
                    processSumDscr(xmlr, dvDTO);

                else if (xmlr.getLocalName().equals("notes"))
                    processNotes(xmlr, dvDTO);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("stdyInfo")) {
                    if (descriptions.size() > 0) {
                        getCitation(dvDTO).getFields()
                            .add(FieldDTO.createMultipleCompoundFieldDTO("dsDescription", descriptions));
                    }
                    return;
                }
            }
        }
    }

    private void processSubject(final XMLStreamReader xmlr, final MetadataBlockDTO citation) throws XMLStreamException {
        final List<HashSet<FieldDTO>> keywords = new ArrayList<>();
        final List<HashSet<FieldDTO>> topicClasses = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {

                if (xmlr.getLocalName().equals("keyword")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "keywordVocabulary", xmlr.getAttributeValue(null, "vocab"));
                    addToSet(set, "keywordVocabularyURI", xmlr.getAttributeValue(null, "vocabURI"));
                    addToSet(set, "keywordValue", parseText(xmlr));
                    if (!set.isEmpty()) {
                        keywords.add(set);
                    }
                } else if (xmlr.getLocalName().equals("topcClas")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "topicClassVocab", xmlr.getAttributeValue(null, "vocab"));
                    addToSet(set, "topicClassVocabURI", xmlr.getAttributeValue(null, "vocabURI"));
                    addToSet(set, "topicClassValue", parseText(xmlr));
                    if (!set.isEmpty()) {
                        topicClasses.add(set);
                    }

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("subject")) {
                    if (keywords.size() > 0) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("keyword", keywords));
                    }
                    if (topicClasses.size() > 0) {
                        citation.getFields()
                            .add(FieldDTO.createMultipleCompoundFieldDTO("topicClassification", topicClasses));
                    }
                    return;
                }
            } else {
                // citation.getFields().add(FieldDTO.createPrimitiveFieldDTO( "subject",xmlr.getElementText()));

            }
        }
    }

    /**
     * Process the notes portion of the DDI doc -- if there is one Return a formatted string
     *
     * @param xmlr
     * @return
     */
    private String formatNotesfromXML(final XMLStreamReader xmlr) throws XMLStreamException {

        if (xmlr == null) {
            throw new NullPointerException("XMLStreamReader xmlr cannot be null");
        }
        // System.out.println("formatNotesfromXML");
        // Initialize array of strings
        final List<String> noteValues = new ArrayList<>();
        String attrVal;

        // Check for "subject"
        attrVal = xmlr.getAttributeValue(null, "subject");
        if (attrVal != null) {
            noteValues.add("Subject: " + attrVal);
        }

        // Check for "type"
        attrVal = xmlr.getAttributeValue(null, "type");
        if (attrVal != null) {
            noteValues.add("Type: " + attrVal);
        }

        // Add notes, if they exist
        attrVal = parseText(xmlr, "notes");
        if ((attrVal != null) && (!attrVal.isEmpty())) {
            noteValues.add("Notes: " + attrVal);
        }

        // Nothing to add
        if (noteValues.isEmpty()) {
            // System.out.println("nuthin'");
            return null;
        }

        // System.out.println(StringUtils.join(noteValues, " ") + ";");
        return StringUtils.join(noteValues, " ") + ";";

        /*
         * Examples of xml: <notes type="Statistics" subject="Babylon"> </notes> <notes type="Note Type"
         * subject="Note Subject">Note Text</notes> <notes type="Note Type 2" subject="Note Subject 2">Note Text
         * 2</notes> <notes>Note Text 3</notes>
         */

        /*
         * // Original, changed b/c of string 'null' appearing in final output String note =
         * " Subject: "+xmlr.getAttributeValue(null, "subject")+" " + " Type: "+xmlr.getAttributeValue(null, "type")+" "
         * + " Notes: "+parseText(xmlr, "notes")+";"; addNote(note, dvDTO);
         */
    }

    private void processNotes(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {

        final String formattedNotes = this.formatNotesfromXML(xmlr);

        if (formattedNotes != null) {
            this.addNote(formattedNotes, dvDTO);
        }
    }

    private void addNote(final String noteText, final DatasetVersionDTO dvDTO) {
        final MetadataBlockDTO citation = getCitation(dvDTO);
        FieldDTO field = citation.getField("notesText");
        if (field == null) {
            field = FieldDTO.createPrimitiveFieldDTO("notesText", "");
            citation.getFields().add(field);
        }
        String noteValue = field.getSinglePrimitive();
        noteValue += noteText;
        field.setSinglePrimitive(noteValue);
    }

    private void processSumDscr(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        final List<String> geoUnit = new ArrayList<>();
        final List<String> unitOfAnalysis = new ArrayList<>();
        final List<String> universe = new ArrayList<>();
        final List<String> kindOfData = new ArrayList<>();
        final List<HashSet<FieldDTO>> geoBoundBox = new ArrayList<>();
        final List<HashSet<FieldDTO>> geoCoverages = new ArrayList<>();
        FieldDTO timePeriodStart = null;
        FieldDTO timePeriodEnd = null;
        FieldDTO dateOfCollectionStart = null;
        FieldDTO dateOfCollectionEnd = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("timePrd")) {

                    final String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr)
                        || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        timePeriodStart =
                            FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredStart", parseDate(xmlr, "timePrd"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        timePeriodEnd =
                            FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredEnd", parseDate(xmlr, "timePrd"));
                    }
                } else if (xmlr.getLocalName().equals("collDate")) {
                    final String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr)
                        || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        dateOfCollectionStart =
                            FieldDTO.createPrimitiveFieldDTO("dateOfCollectionStart", parseDate(xmlr, "collDate"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        dateOfCollectionEnd =
                            FieldDTO.createPrimitiveFieldDTO("dateOfCollectionEnd", parseDate(xmlr, "collDate"));
                    }

                } else if (xmlr.getLocalName().equals("nation")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createVocabFieldDTO("country", parseText(xmlr)));
                    geoCoverages.add(set);
                } else if (xmlr.getLocalName().equals("geogCover")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
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
                    if (timePeriodStart != null || timePeriodEnd != null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("timePeriodCovered",
                            timePeriodStart, timePeriodEnd));
                    }
                    if (dateOfCollectionStart != null || dateOfCollectionEnd != null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("dateOfCollection",
                            dateOfCollectionStart, dateOfCollectionEnd));
                    }

                    if (geoUnit.size() > 0) {
                        getGeospatial(dvDTO)
                            .addField(FieldDTO.createMultiplePrimitiveFieldDTO("geographicUnit", geoUnit));
                    }
                    if (unitOfAnalysis.size() > 0) {
                        getSocialScience(dvDTO)
                            .addField(FieldDTO.createMultiplePrimitiveFieldDTO("unitOfAnalysis", unitOfAnalysis));
                    }
                    if (universe.size() > 0) {
                        getSocialScience(dvDTO)
                            .addField(FieldDTO.createMultiplePrimitiveFieldDTO("universe", universe));
                    }
                    if (kindOfData.size() > 0) {
                        getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("kindOfData", kindOfData));
                    }
                    if (geoCoverages.size() > 0) {
                        getGeospatial(dvDTO)
                            .addField(FieldDTO.createMultipleCompoundFieldDTO("geographicCoverage", geoCoverages));
                    }
                    if (geoBoundBox.size() > 0) {
                        getGeospatial(dvDTO)
                            .addField(FieldDTO.createMultipleCompoundFieldDTO("geographicBoundingBox", geoBoundBox));
                    }
                    return;
                }
            }
        }
    }

    private HashSet<FieldDTO> processGeoBndBox(final XMLStreamReader xmlr) throws XMLStreamException {
        final HashSet<FieldDTO> set = new HashSet<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("westBL")) {
                    addToSet(set, "westLongitude", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("eastBL")) {
                    addToSet(set, "eastLongitude", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("southBL")) {
                    addToSet(set, "southLongitude", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("northBL")) {
                    addToSet(set, "northLongitude", parseText(xmlr));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("geoBndBox"))
                    break;
            }
        }
        return set;
    }

    private void processMethod(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO)
        throws XMLStreamException, ImportException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("dataColl")) {
                    processDataColl(xmlr, dvDTO);
                } else if (xmlr.getLocalName().equals("notes")) {

                    final String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_EXTENDED_METADATA.equalsIgnoreCase(noteType)) {
                        processCustomField(xmlr, dvDTO);
                    } else {
                        addNote("Subject: Study Level Error Note, Notes: " + parseText(xmlr, "notes") + ";", dvDTO);

                    }
                } else if (xmlr.getLocalName().equals("anlyInfo")) {
                    processAnlyInfo(xmlr, getSocialScience(dvDTO));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("method"))
                    return;
            }
        }
    }

    private void processCustomField(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO)
        throws XMLStreamException, ImportException {
        final String subject = xmlr.getAttributeValue(null, "subject");
        if (!subject.isEmpty()) {
            // Syntax of subject attribute:
            // TEMPLATE:Contains Custom Fields;FIELD:Customfield1
            // first parse by semicolon
            final String template = subject.substring(subject.indexOf(":") + 1, subject.indexOf(";"));
            final String sourceField = subject.substring(subject.lastIndexOf(":") + 1);
            final String fieldValue = parseText(xmlr);

            final CustomFieldMap map = customFieldService.findByTemplateField(template.trim(), sourceField.trim());

            if (map == null) {
                throw new ImportException(
                    "Did not find mapping for template: " + template + ", sourceField: " + sourceField);
            }
            if (map.getTargetDatasetField().endsWith("#IGNORE")) {
                // if the target field is #IGNORE, that means we don't want to
                // copy this field from 3.6 to 4.0
                return;
            }

            // 1. Get datasetFieldType for the targetField
            // 2. find the metadatablock for this field type
            // 3. If this metadatablock doesn't exist in DTO, create it
            // 4. add field to mdatadatablock
            final DatasetFieldType dsfType = datasetFieldService.findByName(map.getTargetDatasetField());
            if (dsfType == null) {
                throw new ImportException("Did not find datasetField for target: " + map.getTargetDatasetField());
            }
            final String metadataBlockName = dsfType.getMetadataBlock().getName();
            MetadataBlockDTO customBlock = dvDTO.getMetadataBlocks().get(metadataBlockName);
            if (customBlock == null) {
                customBlock = new MetadataBlockDTO();
                customBlock.setDisplayName(metadataBlockName);
                dvDTO.getMetadataBlocks().put(metadataBlockName, customBlock);
            }
            if (dsfType.isChild()) {
                handleChildField(customBlock, dsfType, fieldValue);
            } else {
                if (dsfType.isAllowMultiples()) {
                    final List<String> valList = new ArrayList<>();
                    valList.add(fieldValue);
                    if (dsfType.isAllowControlledVocabulary()) {
                        customBlock.addField(FieldDTO.createMultipleVocabFieldDTO(dsfType.getName(), valList));
                    } else if (dsfType.isPrimitive()) {
                        customBlock.addField(FieldDTO.createMultiplePrimitiveFieldDTO(dsfType.getName(), valList));
                    } else {
                        throw new ImportException("Unsupported custom field type: " + dsfType);
                    }
                } else {
                    if (dsfType.isAllowControlledVocabulary()) {
                        customBlock.addField(FieldDTO.createVocabFieldDTO(dsfType.getName(), fieldValue));
                    } else if (dsfType.isPrimitive()) {
                        customBlock.addField(FieldDTO.createPrimitiveFieldDTO(dsfType.getName(), fieldValue));
                    } else {
                        throw new ImportException("Unsupported custom field type: " + dsfType);
                    }
                }
            }
        }
    }

    private void handleChildField(final MetadataBlockDTO customBlock, final DatasetFieldType dsfType,
        final String fieldValue) throws ImportException {
        final DatasetFieldType parent = dsfType.getParentDatasetFieldType();

        // Create child Field
        FieldDTO child = null;
        if (dsfType.isAllowControlledVocabulary()) {
            child = FieldDTO.createVocabFieldDTO(dsfType.getName(), fieldValue);
        } else if (dsfType.isPrimitive()) {
            child = FieldDTO.createPrimitiveFieldDTO(dsfType.getName(), fieldValue);
        } else {
            throw new ImportException("Unsupported custom child field type: " + dsfType);
        }
        // Create compound field with this child as its only element
        FieldDTO compound = null;
        if (parent.isAllowMultiples()) {
            compound = FieldDTO.createMultipleCompoundFieldDTO(parent.getName(), child);
        } else {
            compound = FieldDTO.createCompoundFieldDTO(parent.getName(), child);
        }
        customBlock.addField(compound);

    }

    private void processSources(final XMLStreamReader xmlr, final MetadataBlockDTO citation) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // citation dataSources
                String parsedText;
                if (xmlr.getLocalName().equals("dataSrc")) {
                    parsedText = parseText(xmlr, "dataSrc");
                    if (!parsedText.isEmpty()) {
                        citation.addField(
                            FieldDTO.createMultiplePrimitiveFieldDTO("dataSources", Arrays.asList(parsedText)));
                    }
                    // citation originOfSources
                } else if (xmlr.getLocalName().equals("srcOrig")) {
                    parsedText = parseText(xmlr, "srcOrig");
                    if (!parsedText.isEmpty()) {
                        citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("originOfSources", parsedText));
                    }
                    // citation characteristicOfSources
                } else if (xmlr.getLocalName().equals("srcChar")) {
                    parsedText = parseText(xmlr, "srcChar");
                    if (!parsedText.isEmpty()) {
                        citation.getFields()
                            .add(FieldDTO.createPrimitiveFieldDTO("characteristicOfSources", parsedText));
                    }
                    // citation accessToSources
                } else if (xmlr.getLocalName().equals("srcDocu")) {
                    parsedText = parseText(xmlr, "srcDocu");
                    if (!parsedText.isEmpty()) {
                        citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("accessToSources", parsedText));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("sources"))
                    return;
            }
        }
    }

    private void processAnlyInfo(final XMLStreamReader xmlr, final MetadataBlockDTO socialScience)
        throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // socialscience responseRate
                if (xmlr.getLocalName().equals("respRate")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("responseRate", parseText(xmlr, "respRate")));
                    // socialscience samplingErrorEstimates
                } else if (xmlr.getLocalName().equals("EstSmpErr")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("samplingErrorEstimates", parseText(xmlr, "EstSmpErr")));
                    // socialscience otherDataAppraisal
                } else if (xmlr.getLocalName().equals("dataAppr")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("otherDataAppraisal", parseText(xmlr, "dataAppr")));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("anlyInfo"))
                    return;
            }
        }
    }

    private void processDataColl(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        final MetadataBlockDTO socialScience = getSocialScience(dvDTO);
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // timeMethod
                if (xmlr.getLocalName().equals("timeMeth")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("timeMethod", parseText(xmlr, "timeMeth")));
                } else if (xmlr.getLocalName().equals("dataCollector")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("dataCollector", parseText(xmlr, "dataCollector")));
                    // frequencyOfDataCollection
                } else if (xmlr.getLocalName().equals("frequenc")) {
                    socialScience.getFields().add(
                        FieldDTO.createPrimitiveFieldDTO("frequencyOfDataCollection", parseText(xmlr, "frequenc")));
                    // samplingProcedure
                } else if (xmlr.getLocalName().equals("sampProc")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("samplingProcedure", parseText(xmlr, "sampProc")));
                    // targetSampleSize
                } else if (xmlr.getLocalName().equals("targetSampleSize")) {
                    processTargetSampleSize(xmlr, socialScience);
                    // devationsFromSamplingDesign
                } else if (xmlr.getLocalName().equals("deviat")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("deviationsFromSampleDesign", parseText(xmlr, "deviat")));
                    // collectionMode
                } else if (xmlr.getLocalName().equals("collMode")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("collectionMode", parseText(xmlr, "collMode")));
                    // researchInstrument
                } else if (xmlr.getLocalName().equals("resInstru")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("researchInstrument", parseText(xmlr, "resInstru")));
                } else if (xmlr.getLocalName().equals("sources")) {
                    processSources(xmlr, getCitation(dvDTO));
                } else if (xmlr.getLocalName().equals("collSitu")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("dataCollectionSituation", parseText(xmlr, "collSitu")));
                } else if (xmlr.getLocalName().equals("actMin")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("actionsToMinimizeLoss", parseText(xmlr, "actMin")));
                } else if (xmlr.getLocalName().equals("ConOps")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("controlOperations", parseText(xmlr, "ConOps")));
                } else if (xmlr.getLocalName().equals("weight")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("weighting", parseText(xmlr, "weight")));
                } else if (xmlr.getLocalName().equals("cleanOps")) {
                    socialScience.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("cleaningOperations", parseText(xmlr, "cleanOps")));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("dataColl"))
                    return;
            }
        }
    }

    private void processTargetSampleSize(final XMLStreamReader xmlr, final MetadataBlockDTO socialScience)
        throws XMLStreamException {
        FieldDTO sampleSize = null;
        FieldDTO sampleSizeFormula = null;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("sampleSize")) {
                    sampleSize =
                        FieldDTO.createPrimitiveFieldDTO("targetSampleActualSize", parseText(xmlr, "sampleSize"));
                } else if (xmlr.getLocalName().equals("sampleSizeFormula")) {
                    sampleSizeFormula = FieldDTO.createPrimitiveFieldDTO("targetSampleSizeFormula",
                        parseText(xmlr, "sampleSizeFormula"));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("targetSampleSize")) {
                    if (sampleSize != null || sampleSizeFormula != null) {
                        socialScience.getFields()
                            .add(FieldDTO.createCompoundFieldDTO("targetSampleSize", sampleSize, sampleSizeFormula));
                    }
                    return;
                }
            }
        }

    }

    /*
     * EMK TODO: In DVN 3.6, users were allowed to enter their own version date, and in addition the app assigned a
     * version date when the version is released. So DDI's that we have to migrate, we can see this: <verStmt> <version
     * date="2004-04-04">1</version> </verStmt> <verStmt source="DVN"> <version date="2014-05-21"
     * type="RELEASED">1</version> </verStmt> Question: what to do with these two different dates? Need to review with
     * Eleni Note: we should use the verStmt with source="DVN" as the 'official' version statement DDI's that we are
     * migrating should have one and only one DVN version statement
     */
    private void processVerStmt(final ImportType importType, final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO)
        throws XMLStreamException {
        if (isMigrationImport(importType) || isHarvestImport(importType)) {
            if (!"DVN".equals(xmlr.getAttributeValue(null, "source"))) {
                for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if (xmlr.getLocalName().equals("version")) {
                            addNote("Version Date: " + xmlr.getAttributeValue(null, "date"), dvDTO);
                            addNote("Version Text: " + parseText(xmlr), dvDTO);
                        } else if (xmlr.getLocalName().equals("notes")) {
                            processNotes(xmlr, dvDTO);
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        if (xmlr.getLocalName().equals("verStmt"))
                            return;
                    }
                }
            } else {
                // this is the DVN version info; get version number for StudyVersion object
                for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if (xmlr.getLocalName().equals("version")) {
                            dvDTO.setReleaseDate(xmlr.getAttributeValue(null, "date"));
                            String versionState = xmlr.getAttributeValue(null, "type");
                            if (versionState != null) {
                                if (versionState.equals("ARCHIVED")) {
                                    versionState = "RELEASED";
                                } else if (versionState.equals("IN_REVIEW")) {
                                    versionState = DatasetVersion.VersionState.DRAFT.toString();
                                    dvDTO.setInReview(true);
                                }
                                dvDTO.setVersionState(Enum.valueOf(VersionState.class, versionState));
                            }
                            parseVersionNumber(dvDTO, parseText(xmlr));
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        if (xmlr.getLocalName().equals("verStmt"))
                            return;
                    }
                }
            }

        }
        if (isNewImport(importType)) {
            // If this is a new, Draft version, versionNumber and minor versionNumber are null.
            dvDTO.setVersionState(VersionState.DRAFT);
        }
    }

    private void processDataAccs(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("setAvail")) {
                    processSetAvail(xmlr, dvDTO);
                } else if (xmlr.getLocalName().equals("useStmt")) {
                    processUseStmt(xmlr, dvDTO);
                } else if (xmlr.getLocalName().equals("notes")) {
                    final String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_TERMS_OF_USE.equalsIgnoreCase(noteType)) {
                        if (LEVEL_DVN.equalsIgnoreCase(xmlr.getAttributeValue(null, "level"))) {
                            dvDTO.setTermsOfUse(parseText(xmlr, "notes"));
                        }
                    } else {
                        processNotes(xmlr, dvDTO);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("dataAccs")) {
                    return;
                }
            }
        }
    }

    private void processSetAvail(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("accsPlac")) {
                    dvDTO.setDataAccessPlace(parseText(xmlr, "accsPlac"));
                } else if (xmlr.getLocalName().equals("origArch")) {
                    dvDTO.setOriginalArchive(parseText(xmlr, "origArch"));
                } else if (xmlr.getLocalName().equals("avlStatus")) {
                    dvDTO.setAvailabilityStatus(parseText(xmlr, "avlStatus"));
                } else if (xmlr.getLocalName().equals("collSize")) {
                    dvDTO.setSizeOfCollection(parseText(xmlr, "collSize"));
                } else if (xmlr.getLocalName().equals("complete")) {
                    dvDTO.setStudyCompletion(parseText(xmlr, "complete"));
                } else if (xmlr.getLocalName().equals("notes")) {
                    processNotes(xmlr, dvDTO);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("setAvail"))
                    return;
            }
        }
    }

    private void processUseStmt(final XMLStreamReader xmlr, final DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("confDec")) {
                    dvDTO.setConfidentialityDeclaration(parseText(xmlr, "confDec"));
                } else if (xmlr.getLocalName().equals("specPerm")) {
                    dvDTO.setSpecialPermissions(parseText(xmlr, "specPerm"));
                } else if (xmlr.getLocalName().equals("restrctn")) {
                    dvDTO.setRestrictions(parseText(xmlr, "restrctn"));
                } else if (xmlr.getLocalName().equals("contact")) {
                    dvDTO.setContactForAccess(parseText(xmlr, "contact"));
                } else if (xmlr.getLocalName().equals("citReq")) {
                    dvDTO.setCitationRequirements(parseText(xmlr, "citReq"));
                } else if (xmlr.getLocalName().equals("deposReq")) {
                    dvDTO.setDepositorRequirements(parseText(xmlr, "deposReq"));
                } else if (xmlr.getLocalName().equals("conditions")) {
                    dvDTO.setConditions(parseText(xmlr, "conditions"));
                } else if (xmlr.getLocalName().equals("disclaimer")) {
                    dvDTO.setDisclaimer(parseText(xmlr, "disclaimer"));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("useStmt"))
                    return;
            }
        }
    }

    /**
     * Separate the versionNumber into two parts - before the first '.' is the versionNumber, and after is the
     * minorVersionNumber. If no minorVersionNumber exists, set to "0".
     *
     * @param dvDTO
     * @param versionNumber
     */
    private void parseVersionNumber(final DatasetVersionDTO dvDTO, final String versionNumber) {
        final int firstIndex = versionNumber.indexOf('.');
        if (firstIndex == -1) {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber));
            dvDTO.setMinorVersionNumber("0");
        } else {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber.substring(0, firstIndex - 1)));
            dvDTO.setMinorVersionNumber(versionNumber.substring(firstIndex + 1));
        }

    }

    private void processSerStmt(final XMLStreamReader xmlr, final MetadataBlockDTO citation) throws XMLStreamException {
        FieldDTO seriesName = null;
        FieldDTO seriesInformation = null;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("serName")) {
                    seriesName = FieldDTO.createPrimitiveFieldDTO("seriesName", parseText(xmlr));

                } else if (xmlr.getLocalName().equals("serInfo")) {
                    seriesInformation = FieldDTO.createPrimitiveFieldDTO("seriesInformation", parseText(xmlr));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("serStmt")) {
                    citation.getFields().add(FieldDTO.createCompoundFieldDTO("series", seriesName, seriesInformation));
                    return;
                }
            }
        }
    }

    private void processDistStmt(final XMLStreamReader xmlr, final MetadataBlockDTO citation)
        throws XMLStreamException {
        final List<HashSet<FieldDTO>> distributors = new ArrayList<>();
        final List<HashSet<FieldDTO>> datasetContacts = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("distrbtr")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "distributorAbbreviation", xmlr.getAttributeValue(null, "abbr"));
                    addToSet(set, "distributorAffiliation", xmlr.getAttributeValue(null, "affiliation"));

                    final Map<String, String> distDetails = parseCompoundText(xmlr, "distrbtr");
                    addToSet(set, "distributorName", distDetails.get("name"));
                    addToSet(set, "distributorURL", distDetails.get("url"));
                    addToSet(set, "distributorLogoURL", distDetails.get("logo"));
                    distributors.add(set);

                } else if (xmlr.getLocalName().equals("contact")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "datasetContactEmail", xmlr.getAttributeValue(null, "email"));
                    addToSet(set, "datasetContactAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    addToSet(set, "datasetContactName", parseText(xmlr));
                    datasetContacts.add(set);

                } else if (xmlr.getLocalName().equals("depositr")) {
                    final Map<String, String> depDetails = parseCompoundText(xmlr, "depositr");
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("depositor", depDetails.get("name")));
                } else if (xmlr.getLocalName().equals("depDate")) {
                    citation.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("dateOfDeposit", parseDate(xmlr, "depDate")));

                } else if (xmlr.getLocalName().equals("distDate")) {
                    citation.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("distributionDate", parseDate(xmlr, "distDate")));

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

    private void processProdStmt(final XMLStreamReader xmlr, final MetadataBlockDTO citation)
        throws XMLStreamException {
        final List<HashSet<FieldDTO>> producers = new ArrayList<>();
        final List<HashSet<FieldDTO>> grants = new ArrayList<>();
        final List<HashSet<FieldDTO>> software = new ArrayList<>();
        final List<HashSet<FieldDTO>> fundAg = new ArrayList<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("producer")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "producerAbbreviation", xmlr.getAttributeValue(null, "abbr"));
                    addToSet(set, "producerAffiliation", xmlr.getAttributeValue(null, "affiliation"));

                    final Map<String, String> prodDetails = parseCompoundText(xmlr, "producer");
                    addToSet(set, "producerName", prodDetails.get("name"));
                    addToSet(set, "producerURL", prodDetails.get("url"));
                    addToSet(set, "producerLogoURL", prodDetails.get("logo"));
                    if (!set.isEmpty())
                        producers.add(set);
                } else if (xmlr.getLocalName().equals("prodDate")) {
                    citation.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("productionDate", parseDate(xmlr, "prodDate")));
                } else if (xmlr.getLocalName().equals("prodPlac")) {
                    citation.getFields()
                        .add(FieldDTO.createPrimitiveFieldDTO("productionPlace", parseDate(xmlr, "prodPlac")));
                } else if (xmlr.getLocalName().equals("software")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "softwareVersion", xmlr.getAttributeValue(null, "version"));
                    // XXX: #50 - Fix mapping for Software Name
                    addToSet(set, "softwareName", parseText(xmlr));
                    if (!set.isEmpty()) {
                        software.add(set);
                    }
                } else if (xmlr.getLocalName().equals("fundAg")) {
                    // XXX: #45 - Map Funding Agency to Grant Agency
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "grantNumberAgency", parseText(xmlr));
                    addToSet(set, "grantNumberValue", "");
                    if (!set.isEmpty()) {
                        fundAg.add(set);
                    }
                } else if (xmlr.getLocalName().equals("grantNo")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "grantNumberAgency", xmlr.getAttributeValue(null, "agency"));
                    addToSet(set, "grantNumberValue", parseText(xmlr));
                    if (!set.isEmpty()) {
                        grants.add(set);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("prodStmt")) {
                    if (software.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("software", software));
                    }
                    if (grants.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("grantNumber", grants));
                    }
                    if (fundAg.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("grantNumber", fundAg));
                    }
                    if (producers.size() > 0) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("producer", producers));
                    }
                    return;
                }
            }
        }
    }

    private void processTitlStmt(final XMLStreamReader xmlr, final DatasetDTO datasetDTO)
        throws XMLStreamException, ImportException {
        final MetadataBlockDTO citation = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
        final List<HashSet<FieldDTO>> otherIds = new ArrayList<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("titl")) {
                    final FieldDTO field = FieldDTO.createPrimitiveFieldDTO("title", parseText(xmlr));
                    citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("subTitl")) {
                    final FieldDTO field = FieldDTO.createPrimitiveFieldDTO("subtitle", parseText(xmlr));
                    citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("altTitl")) {
                    final FieldDTO field = FieldDTO.createPrimitiveFieldDTO("alternativeTitle", parseText(xmlr));
                    citation.getFields().add(field);
                } else if (xmlr.getLocalName().equals("IDNo")) {
                    if (AGENCY_HANDLE.equals(xmlr.getAttributeValue(null, "agency"))) {
                        parseStudyIdHandle(parseText(xmlr), datasetDTO);
                    } else if (AGENCY_DOI.equals(xmlr.getAttributeValue(null, "agency"))) {
                        parseStudyIdDOI(parseText(xmlr), datasetDTO);
                    } else {
                        final HashSet<FieldDTO> set = new HashSet<>();
                        addToSet(set, "otherIdAgency", xmlr.getAttributeValue(null, "agency"));
                        addToSet(set, "otherIdValue", parseText(xmlr));
                        if (!set.isEmpty()) {
                            otherIds.add(set);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("titlStmt")) {
                    if (otherIds.size() > 0) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("otherId", otherIds));
                    }
                    return;
                }
            }
        }
    }

    private void processRspStmt(final XMLStreamReader xmlr, final MetadataBlockDTO citation) throws XMLStreamException {

        final List<HashSet<FieldDTO>> authors = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("AuthEnty")) {
                    final HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "authorAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    addToSet(set, "authorName", parseText(xmlr));
                    if (!set.isEmpty()) {
                        authors.add(set);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("rspStmt")) {
                    if (authors.size() > 0) {
                        final FieldDTO author = FieldDTO.createMultipleCompoundFieldDTO("author", authors);
                        citation.getFields().add(author);
                    }

                    return;
                }
            }
        }
    }

    private Map<String, String> parseCompoundText(final XMLStreamReader xmlr, final String endTag)
        throws XMLStreamException {
        final Map<String, String> returnMap = new HashMap<>();
        String text = "";

        while (true) {
            final int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                if (text != "") {
                    text += "\n";
                }
                text += xmlr.getText().trim().replace('\n', ' ');
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("ExtLink")) {
                    final String mapKey = ("image".equalsIgnoreCase(xmlr.getAttributeValue(null, "role"))
                        || "logo".equalsIgnoreCase(xmlr.getAttributeValue(null, "title"))) ? "logo" : "url";
                    returnMap.put(mapKey, xmlr.getAttributeValue(null, "URI"));
                    parseText(xmlr, "ExtLink"); // this line effectively just skips though until the end of the tag
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag))
                    break;
            }
        }

        returnMap.put("name", text);
        return returnMap;
    }

    private String parseText(final XMLStreamReader xmlr, final String endTag) throws XMLStreamException {
        return (String) parseTextNew(xmlr, endTag);
    }

    private Object parseTextNew(final XMLStreamReader xmlr, final String endTag) throws XMLStreamException {
        String returnString = "";
        Map returnMap = null;

        while (true) {
            if (!returnString.equals("")) {
                returnString += "\n";
            }
            final int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                returnString += xmlr.getText().trim().replace('\n', ' ');
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("p")) {
                    returnString += "<p>" + parseText(xmlr, "p") + "</p>";
                } else if (xmlr.getLocalName().equals("emph")) {
                    returnString += "<em>" + parseText(xmlr, "emph") + "</em>";
                } else if (xmlr.getLocalName().equals("hi")) {
                    returnString += "<strong>" + parseText(xmlr, "hi") + "</strong>";
                } else if (xmlr.getLocalName().equals("ExtLink")) {
                    final String uri = xmlr.getAttributeValue(null, "URI");
                    final String text = parseText(xmlr, "ExtLink").trim();
                    returnString += "<a href=\"" + uri + "\">" + (StringUtil.isEmpty(text) ? uri : text) + "</a>";
                } else if (xmlr.getLocalName().equals("list")) {
                    returnString += parseText_list(xmlr);
                } else if (xmlr.getLocalName().equals("citation")) {
                    if (SOURCE_DVN_3_0.equals(xmlr.getAttributeValue(null, "source"))) {
                        returnMap = parseDVNCitation(xmlr);
                    } else {
                        returnString += parseText_citation(xmlr);
                    }
                } else {
                    throw new EJBException(
                        "ERROR occurred in mapDDI (parseText): tag not yet supported: <" + xmlr.getLocalName() + ">");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag))
                    break;
            }
        }

        if (returnMap != null) {
            // this is one of our new citation areas for DVN3.0
            return returnMap;
        }

        // otherwise it's a standard section and just return the String like we always did
        return returnString.trim();
    }

    private String parseNoteByType(final XMLStreamReader xmlr, final String type) throws XMLStreamException {
        if (type.equalsIgnoreCase(xmlr.getAttributeValue(null, "type"))) {
            return parseText(xmlr);
        } else {
            return null;
        }
    }

    private String parseText_list(final XMLStreamReader xmlr) throws XMLStreamException {
        String listString = null;
        String listCloseTag = null;

        // check type
        final String listType = xmlr.getAttributeValue(null, "type");
        if ("bulleted".equals(listType)) {
            listString = "<ul>\n";
            listCloseTag = "</ul>";
        } else if ("ordered".equals(listType)) {
            listString = "<ol>\n";
            listCloseTag = "</ol>";
        } else {
            // this includes the default list type of "simple"
            throw new EJBException(
                "ERROR occurred in mapDDI (parseText): ListType of types other than {bulleted, ordered} not currently supported.");
        }

        while (true) {
            final int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("itm")) {
                    listString += "<li>" + parseText(xmlr, "itm") + "</li>\n";
                } else {
                    throw new EJBException(
                        "ERROR occurred in mapDDI (parseText): ListType does not currently supported contained LabelType.");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("list"))
                    break;
            }
        }

        return (listString + listCloseTag);
    }

    private String parseText_citation(final XMLStreamReader xmlr) throws XMLStreamException {
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
                            if (xmlr.getLocalName().equals("titlStmt"))
                                break;
                        }
                    }
                } else if (xmlr.getLocalName().equals("holdings")) {
                    final String uri = xmlr.getAttributeValue(null, "URI");
                    final String holdingsText = parseText(xmlr);

                    if (!StringUtil.isEmpty(uri) || !StringUtil.isEmpty(holdingsText)) {
                        holdings += addHoldings ? ", " : "";
                        addHoldings = true;

                        if (StringUtil.isEmpty(uri)) {
                            holdings += holdingsText;
                        } else if (StringUtil.isEmpty(holdingsText)) {
                            holdings += "<a href=\"" + uri + "\">" + uri + "</a>";
                        } else {
                            // both uri and text have values
                            holdings += "<a href=\"" + uri + "\">" + holdingsText + "</a>";
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation"))
                    break;
            }
        }

        if (addHoldings) {
            citation += " (" + holdings + ")";
        }

        return citation;
    }

    private String parseUNF(final String unfString) {
        if (unfString.indexOf("UNF:") != -1) {
            return unfString.substring(unfString.indexOf("UNF:"));
        } else {
            return null;
        }
    }

    private Map parseDVNCitation(final XMLStreamReader xmlr) throws XMLStreamException {
        final Map returnValues = new HashMap();

        while (true) {
            final int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("IDNo")) {
                    returnValues.put("idType", xmlr.getAttributeValue(null, "agency"));
                    returnValues.put("idNumber", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("biblCit")) {
                    returnValues.put("text", parseText(xmlr));
                } else if (xmlr.getLocalName().equals("holdings")) {
                    returnValues.put("url", xmlr.getAttributeValue(null, "URI"));
                } else if (xmlr.getLocalName().equals("notes")) {
                    if (NOTE_TYPE_REPLICATION_FOR.equals(xmlr.getAttributeValue(null, "type"))) {
                        returnValues.put("replicationData", new Boolean(true));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("citation"))
                    break;
            }
        }

        return returnValues;
    }

    private void parseStudyIdHandle(final String _id, final DatasetDTO datasetDTO) {

        final int index1 = _id.indexOf(':');
        final int index2 = _id.indexOf('/');
        if (index1 == -1) {
            throw new EJBException("Error parsing (Handle) IdNo: " + _id + ". ':' not found in string");
        } else {
            datasetDTO.setProtocol(_id.substring(0, index1));
        }
        if (index2 == -1) {
            throw new EJBException("Error parsing (Handle) IdNo: " + _id + ". '/' not found in string");

        } else {
            datasetDTO.setAuthority(_id.substring(index1 + 1, index2));
        }
        datasetDTO.setDoiSeparator("/");
        datasetDTO.setProtocol("hdl");
        datasetDTO.setIdentifier(_id.substring(index2 + 1));
    }

    private void parseStudyIdDOI(final String _id, final DatasetDTO datasetDTO) throws ImportException {
        final int index1 = _id.indexOf(':');
        final int index2 = _id.lastIndexOf('/');
        if (index1 == -1) {
            throw new EJBException("Error parsing (DOI) IdNo: " + _id + ". ':' not found in string");
        }

        if (index2 == -1) {
            throw new ImportException("Error parsing (DOI) IdNo: " + _id + ". '/' not found in string");

        } else {
            datasetDTO.setAuthority(_id.substring(index1 + 1, index2));
        }
        datasetDTO.setProtocol("doi");
        datasetDTO.setDoiSeparator("/");

        datasetDTO.setIdentifier(_id.substring(index2 + 1));
    }

    // Helper methods
    private MetadataBlockDTO getCitation(final DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("citation");
    }

    private MetadataBlockDTO getGeospatial(final DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("geospatial");
    }

    private MetadataBlockDTO getSocialScience(final DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("socialscience");
    }

    private void addToSet(final HashSet<FieldDTO> set, final String typeName, final String value) {
        if (value != null && !value.trim().isEmpty()) {
            set.add(FieldDTO.createPrimitiveFieldDTO(typeName, value));
        }
    }

    private void processOtherMat(final XMLStreamReader xmlr, final DatasetDTO datasetDTO, final Map filesMap)
        throws XMLStreamException {
        final FileMetadataDTO fmdDTO = new FileMetadataDTO();

        if (datasetDTO.getDatasetVersion().getFileMetadatas() == null) {
            datasetDTO.getDatasetVersion().setFileMetadatas(new ArrayList<>());
        }
        datasetDTO.getDatasetVersion().getFileMetadatas().add(fmdDTO);

        final DataFileDTO dfDTO = new DataFileDTO();
        // if (datasetDTO.getDataFiles() == null) {
        // datasetDTO.setDataFiles(new ArrayList<>());
        // }
        // datasetDTO.getDataFiles().add(dfDTO);

        dfDTO.setStorageIdentifier(xmlr.getAttributeValue(null, "URI"));
        fmdDTO.setDataFile(dfDTO);

        // TODO: handle categories; note that multiple categories are allowed in Dataverse 4;
        String catName = null;
        String icpsrDesc = null;
        String icpsrId = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("labl")) {
                    // this is the file name:
                    fmdDTO.setLabel(parseText(xmlr));
                    // TODO: in DVN3 we used to make an attempt to determine the file type
                    // based on the file name.
                } else if (xmlr.getLocalName().equals("txt")) {
                    fmdDTO.setDescription(parseText(xmlr));
                } else if (xmlr.getLocalName().equals("notes")) {
                    final String noteType = xmlr.getAttributeValue(null, "type");
                    if ("vdc:category".equalsIgnoreCase(noteType)) {
                        catName = parseText(xmlr);
                    } else if ("icpsr:category".equalsIgnoreCase(noteType)) {
                        final String subjectType = xmlr.getAttributeValue(null, "subject");
                        if ("description".equalsIgnoreCase(subjectType)) {
                            icpsrDesc = parseText(xmlr);
                        } else if ("id".equalsIgnoreCase(subjectType)) {
                            icpsrId = parseText(xmlr);
                        }
                    } else if (NOTE_TYPE_CONTENTTYPE.equalsIgnoreCase(noteType)) {
                        final String contentType = parseText(xmlr);
                        if (!StringUtil.isEmpty(contentType)) {
                            dfDTO.setContentType(contentType);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {// </codeBook>
                if (xmlr.getLocalName().equals("otherMat")) {
                    // post process
                    if (fmdDTO.getLabel() == null || fmdDTO.getLabel().trim().equals("")) {
                        fmdDTO.setLabel("harvested file");
                    }

                    // TODO: categories:
                    return;
                }
            }
        }
    }

    private void processFileDscr(final XMLStreamReader xmlr, final DatasetDTO datasetDTO, final Map filesMap)
        throws XMLStreamException {
        final FileMetadataDTO fmdDTO = new FileMetadataDTO();

        datasetDTO.getDatasetVersion().getFileMetadatas().add(fmdDTO);

        // StudyFile sf = new OtherFile(studyVersion.getStudy()); // until we connect the sf and dt, we have to assume
        // it's an other file
        // as an experiment, I'm going to do it the other way around:
        // assume that every fileDscr is a subsettable file now, and convert them
        // to otherFiles later if no variables are referencing it -- L.A.

        // TabularDataFile sf = new TabularDataFile(studyVersion.getStudy());
        final DataFileDTO dfDTO = new DataFileDTO();
        final DataTableDTO dtDTO = new DataTableDTO();
        dfDTO.getDataTables().add(dtDTO);
        fmdDTO.setDataFile(dfDTO);
        datasetDTO.getDataFiles().add(dfDTO);

        // EMK TODO: ask Gustavo about this property
        // dfDTO.setFileSystemLocation( xmlr.getAttributeValue(null, "URI"));
        String ddiFileId = xmlr.getAttributeValue(null, "ID");

        /// the following Strings are used to determine the category

        String catName = null;
        String icpsrDesc = null;
        String icpsrId = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("fileTxt")) {
                    final String tempDDIFileId = processFileTxt(xmlr, fmdDTO, dtDTO);
                    ddiFileId = ddiFileId != null ? ddiFileId : tempDDIFileId;
                } else if (xmlr.getLocalName().equals("notes")) {
                    final String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_UNF.equalsIgnoreCase(noteType)) {
                        final String unf = parseUNF(parseText(xmlr));
                        dfDTO.setUNF(unf);
                        dtDTO.setUnf(unf);
                    } else if ("vdc:category".equalsIgnoreCase(noteType)) {
                        catName = parseText(xmlr);
                    } else if ("icpsr:category".equalsIgnoreCase(noteType)) {
                        final String subjectType = xmlr.getAttributeValue(null, "subject");
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
                    if (fmdDTO.getLabel() == null || fmdDTO.getLabel().trim().equals("")) {
                        fmdDTO.setLabel("file");
                    }

                    fmdDTO.setCategory(determineFileCategory(catName, icpsrDesc, icpsrId));

                    if (ddiFileId != null) {
                        final List filesMapEntry = new ArrayList();
                        filesMapEntry.add(fmdDTO);
                        filesMapEntry.add(dtDTO);
                        filesMap.put(ddiFileId, filesMapEntry);
                    }

                    return;
                }
            }
        }
    }

    private String determineFileCategory(String catName, final String icpsrDesc, final String icpsrId) {
        if (catName == null) {
            catName = icpsrDesc;

            if (catName != null) {
                if (icpsrId != null && !icpsrId.trim().equals("")) {
                    catName = icpsrId + ". " + catName;
                }
            }
        }

        return (catName != null ? catName : "");
    }

    /**
     * sets fmdDTO.label, fmdDTO.description, fmdDTO.studyfile.subsettableFileType
     *
     * @param xmlr
     * @param fmdDTO
     * @param dtDTO
     * @return fmdDTO.label (ddiFileId)
     * @throws XMLStreamException
     */
    private String processFileTxt(final XMLStreamReader xmlr, final FileMetadataDTO fmdDTO, final DataTableDTO dtDTO)
        throws XMLStreamException {
        String ddiFileId = null;
        final DataFileDTO dfDTO = fmdDTO.getDataFile();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("fileName")) {
                    ddiFileId = xmlr.getAttributeValue(null, "ID");
                    fmdDTO.setLabel(parseText(xmlr));
                    /* sf.setFileType( FileUtil.determineFileType( fmdDTO.getLabel() ) ); */

                } else if (xmlr.getLocalName().equals("fileType")) {
                    final String contentType = parseText(xmlr);
                    if (!StringUtil.isEmpty(contentType)) {
                        dfDTO.setContentType(contentType);
                    }
                } else if (xmlr.getLocalName().equals("fileCont")) {
                    fmdDTO.setDescription(parseText(xmlr));
                } else if (xmlr.getLocalName().equals("dimensns"))
                    processDimensns(xmlr, dtDTO);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("fileTxt")) {
                    // If we still don't know the content type of this file
                    // (i.e., if there was no "<fileType>" tag explicitly specifying
                    // the type), we can try and make an educated guess. We already
                    // now that this is a subsettable file. And now that the
                    // "<dimensns>" section has been parsed, we can further
                    // decide if it's a tab, or a fixed field:
                    if (StringUtil.isEmpty(dfDTO.getContentType())) {
                        String subsettableFileType = "text/tab-separated-values";
                        if (dtDTO.getRecordsPerCase() != null) {
                            subsettableFileType = "text/x-fixed-field";
                        }
                    }
                    // EMK TODO: ask Gustavo & Leonid what should be used here instead of setFileType
                    // dfDTO.setFileType( subsettableFileType );

                    return ddiFileId;
                }
            }
        }
        return ddiFileId;
    }

    /**
     * Set dtDTO. caseQuantity, varQuantity, recordsPerCase
     *
     * @param xmlr
     * @param dtDTO
     * @throws XMLStreamException
     */
    private void processDimensns(final XMLStreamReader xmlr, final DataTableDTO dtDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("caseQnty")) {
                    try {
                        dtDTO.setCaseQuantity(new Long(parseText(xmlr)));
                    } catch (final NumberFormatException ex) {
                    }
                } else if (xmlr.getLocalName().equals("varQnty")) {
                    try {
                        dtDTO.setVarQuantity(new Long(parseText(xmlr)));
                    } catch (final NumberFormatException ex) {
                    }
                } else if (xmlr.getLocalName().equals("recPrCas")) {
                    try {
                        dtDTO.setRecordsPerCase(new Long(parseText(xmlr)));
                    } catch (final NumberFormatException ex) {
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {// </codeBook>
                if (xmlr.getLocalName().equals("dimensns"))
                    return;
            }
        }
    }

}
