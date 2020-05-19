package edu.harvard.iq.dataverse.importers.ui.form;

public class FormConstants {
    public static final ProcessingType[] SINGLE_OPTIONS
            = new ProcessingType[] { ProcessingType.FILL_IF_EMPTY, ProcessingType.OVERWRITE };
    public static final ProcessingType[] MULTIPLE_OPTIONS
            = new ProcessingType[] { ProcessingType.MULTIPLE_CREATE_NEW, ProcessingType.MULTIPLE_OVERWRITE};

    public static final String EMPTY_FILE_MESSAGE_KEY = "metadata.import.form.empty.file";
    public static final String EMPTY_FIELD_MESSAGE_KEY = "metadata.import.form.empty.field";
}
