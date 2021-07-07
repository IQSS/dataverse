package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.GlobalId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ejb.EJBException;
import javax.enterprise.inject.Alternative;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Based on DataCitation class created by
 * @author gdurand, qqmyers
 */
@Alternative @Priority(1)
public class StandardCitationFormatsConverter extends AbstractCitationFormatsConverter {
    private static final Logger logger = LoggerFactory.getLogger(StandardCitationFormatsConverter.class);

    // -------------------- LOGIC --------------------

    @Override
    public String toString(CitationData data, Locale locale, boolean escapeHtml) {
        CitationBuilder citation = new CitationBuilder(escapeHtml);

        citation.value(data.getAuthorsString()).endPart()
                .rawValue(data.getYear()).endPart();
        if (data.getFileTitle() != null && data.isDirect()) {
            citation.add("\"").value(data.getFileTitle()).endPart("\", ")
                    .add("<i>").value(data.getTitle()).endPart("</i>, ");
        } else {
            citation.add("\"").value(data.getTitle()).endPart("\", ");
        }
        String pid = Optional.ofNullable(data.getPersistentId())
                .map(GlobalId::toURL)
                .map(URL::toString)
                .orElse(StringUtils.EMPTY);

        citation.urlValue(pid, pid).endPartEmpty()
                .add(", ").value(data.getPublisher()).endPartEmpty()
                .add(", ").rawValue(data.getVersion()).endPartEmpty();

        if (!data.isDirect()) {
            citation.add("; ").value(data.getFileTitle()).endPart(" [fileName]");
        }
        return citation.toString();
    }

    @Override
    public String toBibtexString(CitationData data, Locale locale) {
        GlobalId pid = data.getPersistentId();
        BibTeXCitationBuilder bibtex = new BibTeXCitationBuilder()
                .add(data.getFileTitle() != null && data.isDirect() ? "@incollection{" : "@data{")
                .add(pid.getIdentifier() + "_" + data.getYear() + ",\r\n")
                .line("author", String.join(" and ", data.getAuthors()));

        if(data.getPublisher() != null) {
            bibtex.line("publisher", data.getPublisher());
        }

        if (data.getFileTitle() != null && data.isDirect()) {
            bibtex.line("title", data.getFileTitle())
                    .line("booktitle", data.getTitle());
        } else {
            bibtex.line("title",
                    data.getTitle()
                            .replaceFirst("\"", "``")
                            .replaceFirst("\"", "''"),
                    s -> bibtex.mapValue(s, "\"{", "}\","));
        }
        bibtex.line("year", data.getYear());

        if(data.getVersion() != null) {
            bibtex.line("version", data.getVersion());
        }

        bibtex.line("doi", pid.getAuthority() + "/" + pid.getIdentifier())
                .line("url", pid.toURL().toString(), s -> bibtex.mapValue(s, "{", "}"))
                .add("}\r\n");
        return bibtex.toString();
    }

    @Override
    public String toRISString(CitationData data, Locale locale) {
        RISCitationBuilder ris = new RISCitationBuilder();

        if(data.getPublisher() != null) {
            ris.line("Provider: " + data.getPublisher());
        }

        ris.line("Content: text/plain; charset=\"utf-8\"");
        // Using type DATA: see https://github.com/IQSS/dataverse/issues/4816
        if ((data.getFileTitle() != null) && data.isDirect()) {
            ris.line("TY  - DATA")
                    .line("T1", data.getFileTitle())
                    .line("T2", data.getTitle());
        } else {
            ris.line("TY  - DATA")
                    .line("T1", data.getTitle());
        }
        if (data.getSeriesTitle() != null) {
            ris.line("T3", data.getSeriesTitle());
        }
        ris.lines("AU", data.getAuthors())
                .lines("A2", extractProducerNames(data))
                .lines("A4", data.getFunders())
                .lines("C3", data.getKindsOfData())
                .lines("DA", data.getDatesOfCollection());
        if (data.getPersistentId() != null) {
            ris.line("DO", data.getPersistentId().toString());
        }
        ris.line("ET", data.getVersion())
                .lines("KW", data.getKeywords())
                .lines("LA", data.getLanguages())
                .line("PY", data.getYear())
                .lines("RI", data.getSpatialCoverages())
                .line("SE", Objects.toString(data.getDate(), null))
                .line("UR", data.getPersistentId().toURL().toString())
                .line("PB", data.getPublisher());
        if (data.getFileTitle() != null) {
            if (!data.isDirect()) {
                ris.line("C1", data.getFileTitle());
            }
        }
        ris.line("ER", ""); // closing element
        return ris.toString();
    }

    @Override
    public String toEndNoteString(CitationData data, Locale locale) {
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlw = null;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            xmlw = xmlOutputFactory.createXMLStreamWriter(buffer);
            createEndNoteXML(data, xmlw);
            return buffer.toString();
        } catch (XMLStreamException | IOException e) {
            logger.error("", e);
            throw new EJBException("Error occurred during creating endnote xml.", e);
        } finally {
            try {
                if (xmlw != null) {
                    xmlw.close();
                }
            } catch (XMLStreamException xse) {
                logger.warn("Exception while closing XMLStreamWriter", xse);
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private void createEndNoteXML(CitationData data, XMLStreamWriter xmlw) throws XMLStreamException {
        EndNoteCitationBuilder xml = new EndNoteCitationBuilder(xmlw);
        xml.start()
                .startTag("xml")
                .startTag("records")
                .startTag("record")

    /*
        "Ref-type" indicates which of the (numerous!) available EndNote schemas this record will be interpreted as.

        This is relatively important. Certain fields with generic names like "custom1" and "custom2" become very
     specific things in specific schemas; for example, custom1 shows as "legal notice" in "Journal Article"
     (ref-type 84), or as "year published" in "Government Document".

        We don't want the UNF to show as a "legal notice"!

        We have found a ref-type that works ok for our purposes - "Dataset" (type 59). In this one, the fields
     Custom1 and Custom2 are not translated and just show as is. And "Custom1" still beats "legal notice".

        -- L.A. 12.12.2014 beta 10 and see https://github.com/IQSS/dataverse/issues/4816
    */

                .startTag("ref-type")
                .addAttribute("name", "Dataset")
                .addValue("59")
                .endTag() // ref-type
                .startTag("contributors")
                .addTagCollection("authors", "author", data.getAuthors())
                .addTagCollection("secondary-authors", "author", extractProducerNames(data))
                .addTagCollection("subsidiary-authors", "author", data.getFunders())
                .endTag(); // contributors

        xml.startTag("titles");
        if ((data.getFileTitle() != null) && data.isDirect()) {
            xml.addTagWithValue("title", data.getFileTitle())
                    .addTagWithValue("secondary-title", data.getTitle());
        } else {
            xml.addTagWithValue("title", data.getTitle());
        }
        if (data.getSeriesTitle() != null) {
            xml.addTagWithValue("tertiary-title", data.getSeriesTitle());
        }
        xml.endTag(); // titles

        if(data.getDate() != null) {
            xml.addTagWithValue("section", new SimpleDateFormat("yyyy-MM-dd").format(data.getDate()));
        }
        xml.startTag("dates")
            .addTagWithValue("year", data.getYear())
            .addTagCollection("pub-dates", "date", data.getDatesOfCollection())
            .endTag() // dates
            .addTagWithValue("edition", data.getVersion())
            .addTagCollection("keywords", "keyword", data.getKeywords())
            .addTagCollection(StringUtils.EMPTY, "custom3", data.getKindsOfData())
            .addTagCollection(StringUtils.EMPTY, "language", data.getLanguages())
            .addTagWithValue("publisher", data.getPublisher())
            .addTagCollection(StringUtils.EMPTY, "reviewed-item", data.getSpatialCoverages())
            .startTag("urls")
            .startTag("related-urls")
            .addTagWithValue("url", data.getPersistentId().toURL().toString())
            .endTag() // related-urls
            .endTag(); // urls

        // a DataFile citation also includes the filename and (for Tabular files)
        // the UNF signature, that we put into the custom1 and custom2 fields respectively:
        if (data.getFileTitle() != null) {
            xml.addTagWithValue("custom1", data.getFileTitle());
        }
        if (data.getPersistentId() != null) {
            GlobalId pid = data.getPersistentId();
            xml.addTagWithValue("electronic-resource-num",
                    pid.getProtocol() + "/" + pid.getAuthority() + "/" + pid.getIdentifier());
        }
        xml.endTag() // record
                .endTag() // records
                .endTag() // xml
                .end();
    }

    private List<String> extractProducerNames(CitationData data) {
        return data.getProducers().stream()
                .map(CitationData.Producer::getName)
                .collect(Collectors.toList());
    }
}
