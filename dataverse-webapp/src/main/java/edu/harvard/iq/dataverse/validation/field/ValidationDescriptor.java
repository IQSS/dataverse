package edu.harvard.iq.dataverse.validation.field;

import java.util.HashMap;
import java.util.Map;

public class ValidationDescriptor {

    // Common parameters
    /**
     * Prevents validation to bypass the field if it's empty or N/A.
     * Only presence of the parameter is significant, not the value.
     */
    public static final String RUN_ON_EMPTY_PARAM = "runOnEmpty";
    public static final String CONTEXT_PARAM = "context";
    public static final String DATASET_CONTEXT = "DATASET";
    public static final String SEARCH_CONTEXT = "SEARCH";


    private String name;
    private Map<String, Object> parameters = new HashMap<>();

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }
}
