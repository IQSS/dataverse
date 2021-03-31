package edu.harvard.iq.dataverse.citation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class Citation {
    private static final Logger logger = LoggerFactory.getLogger(Citation.class);

    private CitationData data;

    private CitationFormatsConverter converter;

    private Locale locale;

    // -------------------- CONSTRUCTORS --------------------

    public Citation(CitationData citationData, CitationFormatsConverter converter, Locale locale) {
        this.data = citationData;
        this.converter = converter;
        this.locale = locale;
    }

    // -------------------- GETTERS --------------------

    public CitationData getCitationData() {
        return data;
    }

    // -------------------- LOGIC --------------------

    public String toBibtexString() {
        return converter.toBibtexString(data, locale);
    }

    public String toRISString() {
        return converter.toRISString(data, locale);
    }

    public String toEndNoteString() {
        return converter.toEndNoteString(data, locale);
    }

    public String toString(boolean escapeHtml) {
        return converter.toString(data, locale, escapeHtml);
    }

    @Override
    public String toString() {
        return converter.toString(data, locale, false);
    }
}
