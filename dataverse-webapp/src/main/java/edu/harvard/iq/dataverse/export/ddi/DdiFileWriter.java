package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.xml.XmlAttribute;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.LEVEL_FILE;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_SUBJECT_CONTENTTYPE;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_SUBJECT_TAG;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_SUBJECT_UNF;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_TYPE_CONTENTTYPE;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_TYPE_TAG;
import static edu.harvard.iq.dataverse.export.ddi.DdiConstants.NOTE_TYPE_UNF;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeAttribute;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElement;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElementWithAttributes;

@ApplicationScoped
public class DdiFileWriter {

    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    // JEE requirement
    DdiFileWriter() {}

    @Inject
    public DdiFileWriter(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    // -------------------- LOGIC --------------------

    /**
     * Writes fileDTO into DDI {@code <otherMat>} tag.
     * <p>
     * TODO: see if there's more information that we could encode in this otherMat.
     * contentType? Unfs and such? (in the "short" DDI that is being used for
     * harvesting *all* files are encoded as otherMats; even tabular ones.
     */
    public void writeOtherMatFromFileDto(XMLStreamWriter xmlw, FileMetadataDTO fileDto)
            throws XMLStreamException {

        FileMetadataDTO.DataFileDTO dataFileDto = fileDto.getDataFile();
        String fileDdiId = "f" + dataFileDto.getId();
        String fileUrl = StringUtils.isNotEmpty(dataFileDto.getPidURL()) ?
                dataFileDto.getPidURL()
                : systemConfig.getDataverseSiteUrl() + "/api/access/datafile/" + dataFileDto.getId();

        writeOtherMatFile(xmlw, fileDdiId, fileUrl, dataFileDto.getFilename(), dataFileDto.getDescription(), dataFileDto.getContentType());
    }


    /**
     * An alternative version of the {@link #writeOtherMatFromFileDto(XMLStreamWriter, FileMetadataDTO)}
     * method - this one is used
     * when a "full" DDI is being cooked; just like the fileDscr and data/var sections methods,
     * it operates on the list of FileMetadata entities, not on File DTOs. This is because
     * DTOs do not support "tabular", variable-level metadata yet. And we need to be able to
     * tell if this file is in fact tabular data - so that we know if it needs an
     * otherMat, or a fileDscr section.
     * -- L.A. 4.5
     */
    public void writeOtherMatFromFileMetadata(XMLStreamWriter xmlw, FileMetadata fileMetadata)
            throws XMLStreamException {

        DataFile dataFile = fileMetadata.getDataFile();
        String fileDdiId = "f" + dataFile.getId();

        String fileUrl = dataFile.getGlobalId().isComplete() ?
                dataFile.getGlobalId().toURL().toString()
                : systemConfig.getDataverseSiteUrl() + "/api/access/datafile/" + dataFile.getId();

                writeOtherMatFile(xmlw, fileDdiId, fileUrl, fileMetadata.getLabel(), fileMetadata.getDescription(), dataFile.getContentType());
    }

    /**
     * Writes files (ONLY tabular) into DDI {@code <fileDscr>} tags.
     */
    public void writeFileDscr(XMLStreamWriter xmlw, FileMetadata fileMetadata) throws XMLStreamException {
        DataFile dataFile = fileMetadata.getDataFile();
        DataTable dt = dataFile.getDataTable();

        xmlw.writeStartElement("fileDscr");
        writeAttribute(xmlw, "ID", "f" + dataFile.getId());
        writeAttribute(xmlw, "URI", systemConfig.getDataverseSiteUrl() + "/api/access/datafile/" + dataFile.getId());

        xmlw.writeStartElement("fileTxt");
        writeFullElement(xmlw, "fileName", fileMetadata.getLabel());

        if (dt.getCaseQuantity() != null || dt.getVarQuantity() != null || dt.getRecordsPerCase() != null) {
            xmlw.writeStartElement("dimensns");

            if (dt.getCaseQuantity() != null) {
                writeFullElement(xmlw, "caseQnty", dt.getCaseQuantity().toString());
            }

            if (dt.getVarQuantity() != null) {
                writeFullElement(xmlw, "varQnty", dt.getVarQuantity().toString());
            }

            if (dt.getRecordsPerCase() != null) {
                writeFullElement(xmlw, "recPrCas", dt.getRecordsPerCase().toString());
            }

            xmlw.writeEndElement(); // dimensns
        }

        writeFullElement(xmlw, "fileType", dataFile.getContentType());

        xmlw.writeEndElement(); // fileTxt

        // various notes:
        // this specially formatted note section is used to store the UNF
        // (Universal Numeric Fingerprint) signature:
        writeFullElementWithAttributes(xmlw, "notes", dt.getUnf(),
                XmlAttribute.of("level", LEVEL_FILE),
                XmlAttribute.of("type", NOTE_TYPE_UNF),
                XmlAttribute.of("subject", NOTE_SUBJECT_UNF));

        for (DataFileTag tag: dataFile.getTags()) {
            writeFullElementWithAttributes(xmlw, "notes", tag.getTypeLabel(),
                    XmlAttribute.of("level", LEVEL_FILE),
                    XmlAttribute.of("type", NOTE_TYPE_TAG),
                    XmlAttribute.of("subject", NOTE_SUBJECT_TAG));
        }

        // TODO: add the remaining fileDscr elements!
        xmlw.writeEndElement(); // fileDscr
    }

    // -------------------- PRIVATE --------------------

    private void writeOtherMatFile(XMLStreamWriter xmlw, String fileDdiId, String fileUrl,
            String label, String description, String contentType) throws XMLStreamException {
        xmlw.writeStartElement("otherMat");
        writeAttribute(xmlw, "ID", fileDdiId);
        writeAttribute(xmlw, "URI", fileUrl);
        writeAttribute(xmlw, "level", "datafile");

        writeFullElement(xmlw, "labl", label);
        writeFullElement(xmlw, "txt", description);

        // there's no readily available field in the othermat section
        // for the content type (aka mime type); so we'll store it in this
        // specially formatted notes section:
        writeFullElementWithAttributes(xmlw, "notes", contentType,
                XmlAttribute.of("level", LEVEL_FILE),
                XmlAttribute.of("type", NOTE_TYPE_CONTENTTYPE),
                XmlAttribute.of("subject", NOTE_SUBJECT_CONTENTTYPE));

        xmlw.writeEndElement(); // otherMat
    }
}
