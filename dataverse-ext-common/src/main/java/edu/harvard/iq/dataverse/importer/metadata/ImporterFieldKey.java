package edu.harvard.iq.dataverse.importer.metadata;

/**
 * This interface should be implemented by enums which will be used as a set of keys
 * for elements of interface of importer form.
 */
public interface ImporterFieldKey {
    /**
     * This value is (and has to be) used by those elements of form that should
     * not be processed.
     */
    ImporterFieldKey IRRELEVANT = new ImporterFieldKey() {
        @Override
        public String getName() {
            return "IRRELEVANT";
        }

        @Override
        public boolean isRelevant() {
            return false;
        }
    };

    /**
     * The name used for id generation on view.
     */
    String getName();

    default boolean isRelevant() {
        return true;
    }
}
