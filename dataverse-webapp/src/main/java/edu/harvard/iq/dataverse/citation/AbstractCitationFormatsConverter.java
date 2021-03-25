package edu.harvard.iq.dataverse.citation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public abstract class AbstractCitationFormatsConverter implements CitationFormatsConverter {

    // -------------------- INNER CLASSES --------------------

    protected static class CitationBuilder {
        private final boolean escapeHtml;
        private final StringBuilder citation = new StringBuilder();
        private final List<Token> partElements = new ArrayList<>();

        // -------------------- CONSTRUCTORS --------------------

        public CitationBuilder(boolean escapeHtml) {
            this.escapeHtml = escapeHtml;
        }

        // -------------------- LOGIC --------------------

        public CitationBuilder add(String value) {
            partElements.add(new Token(value, Token.Type.STATIC));
            return this;
        }

        public CitationBuilder value(String value) {
            if (isNotBlank(value)) {
                partElements.add(new Token(escapeHtml ? StringEscapeUtils.escapeHtml4(value) : value, Token.Type.DATA));
            } else {
                partElements.add(new Token(StringUtils.EMPTY, Token.Type.DATA));
            }
            return this;
        }

        public CitationBuilder rawValue(String value) {
            partElements.add(new Token(value, Token.Type.DATA));
            return this;
        }

        public CitationBuilder urlValue(String text, String url) {
            if (text == null) {
                partElements.add(new Token(StringUtils.EMPTY, Token.Type.DATA));
            } else {
                String value = escapeHtml && url != null
                        ? "<a href=\"" + url + "\" target=\"_blank\">" + StringEscapeUtils.escapeHtml4(text) + "</a>"
                        : text;
                partElements.add(new Token(value, Token.Type.DATA));
            }
            return this;
        }

        public CitationBuilder endPart() {
            return endPart(", ");
        }

        public CitationBuilder endPart(String delimiter) {
            if (partElements.stream()
                    .filter(t -> Token.Type.DATA == t.type)
                    .map(t -> t.value)
                    .allMatch(StringUtils::isNotBlank)) {
                citation.append(partElements.stream()
                        .map(t -> t.value)
                        .collect(Collectors.joining()))
                        .append(delimiter);
            }
            partElements.clear();
            return this;
        }

        public String toString() {
            return citation.toString();
        }

        private static class Token {
            public final String value;
            public final Type type;

            public Token(String value, Type type) {
                this.value = value;
                this.type = type;
            }

            enum Type {
                STATIC, DATA
            }
        }
    }


    protected static class BibTeXCitationBuilder {
        private StringBuilder sb = new StringBuilder();

        // -------------------- CONSTRUCTORS --------------------

        public BibTeXCitationBuilder() { }

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

    protected static class RISCitationBuilder {
        private StringBuilder sb = new StringBuilder();

        // -------------------- CONSTRUCTORS --------------------

        public RISCitationBuilder() { }

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

    protected static class EndNoteCitationBuilder {
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