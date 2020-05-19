package edu.harvard.iq.dataverse.importer.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class should produce programmatic description of importer form that
 * will be used for obtain some needed initial data from the user.
 */
public class ImporterData {

    private List<ImporterField> importerFormSchema = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public List<ImporterField> getImporterFormSchema() {
        return importerFormSchema;
    }

    // -------------------- LOGIC --------------------

    public ImporterData addField(ImporterField field) {
        importerFormSchema.add(field);
        return this;
    }

    /**
     * A convenience method to add description (to the method we pass only bundle key of that
     * description), i.e. larger block of text that will be shown to the user on first step
     * of import dialog. It could be used to describe thoroughly how importer works or to give
     * some instruction for the users.
     */
    public ImporterData addDescription(String descriptionKey) {
        importerFormSchema.add(ImporterField.of(ImporterFieldKey.IRRELEVANT, ImporterFieldType.DESCRIPTION,
                false, StringUtils.EMPTY, descriptionKey));
        return this;
    }

    // -------------------- INNER CLASSES --------------------

    public static class ImporterField {
        public final ImporterFieldKey fieldKey;
        public final ImporterFieldType fieldType;
        public final boolean required;

        /**
         * Key for the label for the element on input form.
         */
        public final String labelKey;

        /**
         * Key for element description for the element on input form.
         */
        public final String descriptionKey;

        private ImporterField(ImporterFieldKey fieldKey, ImporterFieldType fieldType, boolean required,
                              String labelKey, String descriptionKey) {
            this.fieldKey = fieldKey;
            this.fieldType = fieldType;
            this.required = required;
            this.labelKey = labelKey;
            this.descriptionKey = descriptionKey;
        }

        public static ImporterField of(ImporterFieldKey fieldKey, ImporterFieldType fieldType, boolean required,
                                       String labelKey, String descriptionKey) {
            return new ImporterField(fieldKey, fieldType, required, labelKey, descriptionKey);
        }
    }
}
