package edu.harvard.iq.dataverse.citation;

public interface CitationFormatsConverter {

    String toString(CitationData data, boolean escapeHtml);

    String toBibtexString(CitationData data);

    String toRISString(CitationData data);

    String toEndNoteString(CitationData data);
}
