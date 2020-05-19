package edu.harvard.iq.dataverse.importers.ui.form;

import org.apache.commons.lang3.StringUtils;

public enum ProcessingType {
    FILL_IF_EMPTY("metadata.import.processing.type.fill.if.empty"),
    OVERWRITE("metadata.import.processing.type.overwrite"),
    MULTIPLE_OVERWRITE("metadata.import.processing.type.multiple.overwrite"),
    MULTIPLE_CREATE_NEW("metadata.import.processing.type.create.new"),
    VOCABULARY_VALUE(StringUtils.EMPTY),
    UNPROCESSABLE(StringUtils.EMPTY)
    ;

    private String key;

    // -------------------- CONSTRUCTORS --------------------

    ProcessingType(String key) {
        this.key = key;
    }

    // -------------------- GETTERS --------------------

    public String getKey() {
        return key;
    }
}
