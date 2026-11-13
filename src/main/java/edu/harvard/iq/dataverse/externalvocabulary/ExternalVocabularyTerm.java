package edu.harvard.iq.dataverse.externalvocabulary;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public class ExternalVocabularyTerm {

    private final String uri;
    private final String label;
    private final String vocabularyName;
    private final String vocabularyUri;
    private final String source;
    private final JsonObject mappedFields;

    public ExternalVocabularyTerm(String uri, String label, String vocabularyName, String vocabularyUri, String source) {
        this(uri, label, vocabularyName, vocabularyUri, source, null);
    }

    public ExternalVocabularyTerm(String uri, String label, String vocabularyName, String vocabularyUri, String source,
            JsonObject mappedFields) {
        this.uri = uri;
        this.label = label;
        this.vocabularyName = vocabularyName;
        this.vocabularyUri = vocabularyUri;
        this.source = source;
        this.mappedFields = mappedFields;
    }

    public String getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public String getVocabularyName() {
        return vocabularyName;
    }

    public String getVocabularyUri() {
        return vocabularyUri;
    }

    public String getSource() {
        return source;
    }

    public JsonObject getMappedFields() {
        return mappedFields;
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        return NullSafeJsonBuilder.jsonObjectBuilder()
                .add("uri", uri)
                .add("label", label)
                .add("vocabularyName", vocabularyName)
                .add("vocabularyUri", vocabularyUri)
                .add("source", source)
                .add("mappedFields", mappedFields);
    }
}
