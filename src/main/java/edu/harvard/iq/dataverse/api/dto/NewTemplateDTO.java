package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewTemplateDTO {

    private String name;
    private List<DatasetField> datasetFields;
    private Map<String, String> instructionsMap;
    private boolean isDefault;

    public static NewTemplateDTO fromRequestBody(String requestBody, JsonParser jsonParser) throws JsonParseException {
        NewTemplateDTO newTemplateDTO = new NewTemplateDTO();

        JsonObject jsonObject = JsonUtil.getJsonObject(requestBody);

        newTemplateDTO.name = jsonObject.getString("name");
        newTemplateDTO.datasetFields = jsonParser.parseMultipleFields(jsonObject);
        newTemplateDTO.instructionsMap = parseInstructionsMap(jsonObject);
        newTemplateDTO.isDefault = jsonObject.getBoolean("isDefault");

        return newTemplateDTO;
    }

    private static Map<String, String> parseInstructionsMap(JsonObject jsonObject) {
        Map<String, String> instructionsMap = new HashMap<>();
        JsonArray instructionsJsonArray = jsonObject.getJsonArray("instructions");
        for (JsonObject instructionJsonObject : instructionsJsonArray.getValuesAs(JsonObject.class)) {
            instructionsMap.put(
                    instructionJsonObject.getString("instructionField"),
                    instructionJsonObject.getString("instructionText")
            );
        }
        return instructionsMap;
    }

    public String getName() {
        return name;
    }

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    public Map<String, String> getInstructionsMap() {
        return instructionsMap;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
