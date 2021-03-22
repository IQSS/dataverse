package edu.harvard.iq.dataverse.citation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Citation {
    private static final Logger logger = LoggerFactory.getLogger(Citation.class);

    private CitationData data;

    private CitationFormatsConverter converter;

    // -------------------- CONSTRUCTORS --------------------

    public Citation(CitationData citationData, CitationFormatsConverter converter) {
        this.data = citationData;
        this.converter = converter;
    }

    // -------------------- GETTERS --------------------

    public CitationData getCitationData() {
        return data;
    }

    // -------------------- LOGIC --------------------

    public String toBibtexString() {
        return converter.toBibtexString(data);
    }

    public String toRISString() {
        return converter.toRISString(data);
    }

    public String toEndNoteString() {
        return converter.toEndNoteString(data);
    }

    public String toString(boolean escapeHtml) {
        return converter.toString(data, escapeHtml);
    }

    @Override
    public String toString() {
        return converter.toString(data, false);
    }
}
