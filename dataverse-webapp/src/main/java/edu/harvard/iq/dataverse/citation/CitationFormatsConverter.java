package edu.harvard.iq.dataverse.citation;

import java.util.Locale;

public interface CitationFormatsConverter {

    String toString(CitationData data, Locale locale, boolean escapeHtml);

    String toBibtexString(CitationData data, Locale locale);

    String toRISString(CitationData data, Locale locale);

    String toEndNoteString(CitationData data, Locale locale);
}
