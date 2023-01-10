package edu.harvard.iq.dataverse.validation.datasetfield;

import java.util.HashMap;
import java.util.Map;

public class ValidationDescriptor {

    // Common parameters
    /**
     * Prevents validation to bypass the field if it's empty or N/A.
     * Only presence of the parameter is significant, not the value.
     */
    public static final String RUN_ON_EMPTY_PARAM = "runOnEmpty";

    private String name;
    private Map<String, String> parameters = new HashMap<>();

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }
}
