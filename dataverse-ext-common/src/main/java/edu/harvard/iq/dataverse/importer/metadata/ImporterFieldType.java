package edu.harvard.iq.dataverse.importer.metadata;

/**
 * Contains elements that could be used to build importer interface for user input.
 */
public enum ImporterFieldType {
    /**
     * Simple text input.
     */
    INPUT,

    /**
     * Longer text – could be used to present some longer text on the importer form.
     */
    DESCRIPTION,

    /**
     * [currently not implemented – should give the ability to choose already existing
     * file from dataset for processing by the importer]
     */
    DATASET_FILE,

    /**
     * Used for uploading temporary file that will be processed by the importer and then
     * deleted. Please note that deletion of file is handled by dataverse app.
     */
    UPLOAD_TEMP_FILE;
}
