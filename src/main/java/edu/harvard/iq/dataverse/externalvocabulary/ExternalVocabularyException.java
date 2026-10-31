package edu.harvard.iq.dataverse.externalvocabulary;

public class ExternalVocabularyException extends Exception {

    public ExternalVocabularyException(String message) {
        super(message);
    }

    public ExternalVocabularyException(String message, Throwable cause) {
        super(message, cause);
    }
}
