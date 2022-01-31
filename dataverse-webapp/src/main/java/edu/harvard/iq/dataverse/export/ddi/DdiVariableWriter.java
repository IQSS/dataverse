package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableRange;
import edu.harvard.iq.dataverse.util.xml.XmlAttribute;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeAttribute;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElement;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElementWithAttributes;

@ApplicationScoped
public class DdiVariableWriter {

    // -------------------- LOGIC --------------------

    /**
     * Writes data variable information of tabular file into DDI {@code <var>} tag
     * <p>
     * Note that these do NOT operate on DTO objects, but instead directly
     * on Dataverse DataVariable, DataTable, etc. objects.
     * This is because for this release (4.5) we are recycling the already available
     * code, and this is what we got. (We already have DTO objects for DataTable,
     * and DataVariable, etc., but currently we don't produce JSON for these objects
     * - we stop at DataFile. Eventually we want all of our objects to be exportable
     * as JSON, and then all the exports can go through the same DTO state...
     * But we don't have time for it now; plus, the structure of file-level metadata
     * is currently being re-designed, so we probably should not invest any time into
     * it right now). -- L.A. 4.5
     */
    public void createVarDDI(XMLStreamWriter xmlw, DataVariable dv, FileMetadata fileMetadata) throws XMLStreamException {
        xmlw.writeStartElement("var");
        writeAttribute(xmlw, "ID", "v" + dv.getId().toString());
        writeAttribute(xmlw, "name", dv.getName());

        if (dv.getNumberOfDecimalPoints() != null) {
            writeAttribute(xmlw, "dcml", dv.getNumberOfDecimalPoints().toString());
        }

        if (dv.isOrderedCategorical()) {
            writeAttribute(xmlw, "nature", "ordinal");
        }

        writeAttribute(xmlw, "intrvl", dv.getIntervalLabel());


        writeVarLocation(xmlw, dv);

        writeFullElementWithAttributes(xmlw, "labl", dv.getLabel(),
                XmlAttribute.of("level", "variable"));

        writeInvalrng(xmlw, dv);

        writeVarUniverse(xmlw, dv, fileMetadata);

        writeVarSumStat(xmlw, dv);

        writeVarCatgry(xmlw, dv);

        writeVarFormat(xmlw, dv);

        writeFullElementWithAttributes(xmlw, "notes", dv.getUnf(),
                XmlAttribute.of("subject", "Universal Numeric Fingerprint"),
                XmlAttribute.of("level", "variable"),
                XmlAttribute.of("type", "Dataverse:UNF"));

        xmlw.writeEndElement(); //var

    }

    // -------------------- PRIVATE --------------------

    private void writeVarLocation(XMLStreamWriter xmlw, DataVariable dataVariable) throws XMLStreamException {
        xmlw.writeEmptyElement("location");
        if (dataVariable.getFileStartPosition() != null) {
            writeAttribute(xmlw, "StartPos", dataVariable.getFileStartPosition().toString());
        }
        if (dataVariable.getFileEndPosition() != null) {
            writeAttribute(xmlw, "EndPos", dataVariable.getFileEndPosition().toString());
        }
        if (dataVariable.getRecordSegmentNumber() != null) {
            writeAttribute(xmlw, "RecSegNo", dataVariable.getRecordSegmentNumber().toString());
        }

        writeAttribute(xmlw, "fileid", "f" + dataVariable.getDataTable().getDataFile().getId().toString());
    }

    private void writeInvalrng(XMLStreamWriter xmlw, DataVariable dataVariable) throws XMLStreamException {
        boolean invalrngAdded = false;
        for (VariableRange range : dataVariable.getInvalidRanges()) {
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
    }

    private void writeVarUniverse(XMLStreamWriter xmlw, DataVariable dataVariable, FileMetadata fileMetadata) throws XMLStreamException {
        //universe
        VariableMetadata vm = null;
        for (VariableMetadata vmIter : dataVariable.getVariableMetadatas()) {
            FileMetadata fm = vmIter.getFileMetadata();
            if (fm != null && fm.equals(fileMetadata)) {
                vm = vmIter;
                break;
            }
        }

        if (vm != null) {
            writeFullElement(xmlw, "universe", vm.getUniverse());
        }
    }

    private void writeVarSumStat(XMLStreamWriter xmlw, DataVariable dataVariable) throws XMLStreamException {
        for (SummaryStatistic sumStat : dataVariable.getSummaryStatistics()) {
            xmlw.writeStartElement("sumStat");
            if (sumStat.getTypeLabel() != null) {
                writeAttribute(xmlw, "type", sumStat.getTypeLabel());
            } else {
                writeAttribute(xmlw, "type", "unknown");
            }
            xmlw.writeCharacters(sumStat.getValue());
            xmlw.writeEndElement(); //sumStat
        }
    }

    private void writeVarCatgry(XMLStreamWriter xmlw, DataVariable dataVariable) throws XMLStreamException {
        for (VariableCategory cat : dataVariable.getCategories()) {
            xmlw.writeStartElement("catgry");
            if (cat.isMissing()) {
                writeAttribute(xmlw, "missing", "Y");
            }

            // catValu
            xmlw.writeStartElement("catValu");
            xmlw.writeCharacters(cat.getValue());
            xmlw.writeEndElement(); //catValu

            // label
            writeFullElementWithAttributes(xmlw, "labl", cat.getLabel(),
                    XmlAttribute.of("level", "category"));

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

            xmlw.writeEndElement(); //catgry
        }
    }

    private void writeVarFormat(XMLStreamWriter xmlw, DataVariable dataVariable) throws XMLStreamException {
        xmlw.writeEmptyElement("varFormat");
        if (dataVariable.isTypeNumeric()) {
            writeAttribute(xmlw, "type", "numeric");
        } else if (dataVariable.isTypeCharacter()) {
            writeAttribute(xmlw, "type", "character");
        } else {
            throw new XMLStreamException("Illegal Variable Format Type!");
        }
        writeAttribute(xmlw, "formatname", dataVariable.getFormat());
        writeAttribute(xmlw, "category", dataVariable.getFormatCategory());
    }

    private boolean checkParentElement(XMLStreamWriter xmlw, String elementName, boolean elementAdded) throws XMLStreamException {
        if (!elementAdded) {
            xmlw.writeStartElement(elementName);
        }

        return true;
    }
}
