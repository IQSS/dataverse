package edu.harvard.iq.dataverse.externalvocabulary;

public class ExternalVocabularyContext {

    private final String vocabulary;
    private final String language;

    public ExternalVocabularyContext(String vocabulary, String language) {
        this.vocabulary = vocabulary;
        this.language = language;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public String getLanguage() {
        return language;
    }
}
