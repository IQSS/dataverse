package edu.harvard.iq.dataverse.citation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

abstract class AbstractCitationFormatsConverter implements CitationFormatsConverter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractCitationFormatsConverter.class);

    // -------------------- INNER CLASSES --------------------

    static class CitationBuilder {
        private boolean escapeHtml;
        private List<String> elements = new ArrayList<>();

        // -------------------- CONSTRUCTORS --------------------

        public CitationBuilder(boolean escapeHtml) {
            this.escapeHtml = escapeHtml;
        }

        // -------------------- LOGIC --------------------

        public CitationBuilder add(String... values) {
            elements.addAll(Arrays.asList(values));
            return this;
        }

        public CitationBuilder addFormatted(String value) {
            return addFormatted(value, "");
        }

        public CitationBuilder addFormatted(String value, String delimiter) {
            return addFormatted(value, delimiter, delimiter);
        }

        public CitationBuilder addFormatted(String value, String startDelimiter, String endDelimiter) {
            if (isNotEmpty(value)) {
                elements.add(startDelimiter
                        + (escapeHtml ? StringEscapeUtils.escapeHtml4(value) : value)
                        + endDelimiter);
            }
            return this;
        }

        public String escapeHtmlIfNeeded(String value) {
            if (isNotEmpty(value)) {
                return escapeHtml ? StringEscapeUtils.escapeHtml4(value) : value;
            }
            return null;
        }

        public String formatURL(String text, String url) {
            if (text == null) {
                return null;
            }
            return escapeHtml && url != null
                    ? "<a href=\"" + url + "\" target=\"_blank\">" + StringEscapeUtils.escapeHtml4(text) + "</a>"
                    : text;
        }

        public String join(String separator, Predicate<String> filter) {
            String joined = elements.stream()
                    .filter(filter)
                    .collect(Collectors.joining(separator));
            elements.clear();
            elements.add(joined);
            return joined;
        }
    }

    static class BibTeXCitationBuilder {
        private StringBuilder sb = new StringBuilder();

        // -------------------- LOGIC --------------------

        public BibTeXCitationBuilder line(String label, String value) {
            return line(label, value, s -> mapValue(s, "{", "},"));
        }

        public BibTeXCitationBuilder line(String label, String value, Function<String, String> valueMapper) {
            sb.append(label)
                    .append(" = ")
                    .append(valueMapper.apply(value))
                    .append("\r\n");
            return this;
        }

        public String mapValue(String value, String startDelimiter, String endDelimiter) {
            return startDelimiter + value + endDelimiter;
        }

        public BibTeXCitationBuilder add(String text) {
            sb.append(text);
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    static class RISCitationBuilder {
        private StringBuilder sb = new StringBuilder();

        // -------------------- LOGIC --------------------

        public RISCitationBuilder line(String value) {
            sb.append(value)
                    .append("\r\n");
            return this;
        }

        public RISCitationBuilder line(String label, String value) {
            sb.append(label)
                    .append("  - ");
            return line(value);
        }

        public RISCitationBuilder lines(String label, Collection<String> values) {
            values.forEach(v -> line(label, v));
            return this;
        }

        public RISCitationBuilder add(String text) {
            sb.append(text);
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    static class EndNoteCitationBuilder {
        private XMLStreamWriter writer;

        // -------------------- CONSTRUCTORS --------------------

        public EndNoteCitationBuilder(XMLStreamWriter writer) {
            this.writer = writer;
        }

        // -------------------- LOGIC --------------------

        public EndNoteCitationBuilder start() throws XMLStreamException {
            writer.writeStartDocument();
            return this;
        }

        public void end() throws XMLStreamException {
            writer.writeEndDocument();
            writer = null;
        }

        public EndNoteCitationBuilder addTagWithValue(String tag, String value) throws XMLStreamException {
            writer.writeStartElement(tag);
            writer.writeCharacters(value);
            writer.writeEndElement();
            return this;
        }

        public EndNoteCitationBuilder addTagCollection(String collectionTag, String itemTag, Collection<String> values)
                throws XMLStreamException {
            if (values.isEmpty()) {
                return this;
            }
            if (isNotEmpty(collectionTag)) {
                startTag(collectionTag);
            }
            for (String value : values) {
                addTagWithValue(itemTag, value);
            }
            if (isNotEmpty(collectionTag)) {
                endTag();
            }
            return this;
        }

        public EndNoteCitationBuilder startTag(String tag) throws XMLStreamException {
            writer.writeStartElement(tag);
            return this;
        }

        public EndNoteCitationBuilder addAttribute(String name, String value) throws XMLStreamException {
            writer.writeAttribute(name, value);
            return this;
        }

        public EndNoteCitationBuilder addValue(String value) throws XMLStreamException {
            writer.writeCharacters(value);
            return this;
        }

        public EndNoteCitationBuilder endTag() throws XMLStreamException {
            writer.writeEndElement();
            return this;
        }
    }
}