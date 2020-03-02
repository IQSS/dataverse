package edu.harvard.iq.dataverse.util.json;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.Optional;

public class ParserDataHolder {

    private JsonObjectBuilder parentDsf;
    private JsonArrayBuilder childValues;
    private List<String> primitiveValues;

    // -------------------- CONSTRUCTORS --------------------

    public ParserDataHolder(JsonObjectBuilder parentDsf, JsonArrayBuilder childValues) {
        this.parentDsf = parentDsf;
        this.childValues = childValues;
    }

    public ParserDataHolder(JsonObjectBuilder parentDsf, List<String> primitiveValues) {
        this.parentDsf = parentDsf;
        this.primitiveValues = primitiveValues;
    }

    // -------------------- GETTERS --------------------

    public JsonObjectBuilder getParentDsf() {
        return parentDsf;
    }

    public Optional<JsonArrayBuilder> getChildValues() {
        return Optional.ofNullable(childValues);
    }

    public Optional<List<String>> getPrimitiveValues() {
        return Optional.ofNullable(primitiveValues);
    }
}
