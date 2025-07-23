package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.*;

import java.sql.Timestamp;
import java.util.*;

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
        newTemplateDTO.instructionsMap = parseRequestBodyInstructionsMap(jsonObject);
        newTemplateDTO.isDefault = jsonObject.getBoolean("isDefault", false);

        return newTemplateDTO;
    }

    public Template toTemplate() {
        Template template = new Template();

        template.setDatasetFields(getDatasetFields());
        template.setName(getName());
        template.setInstructionsMap(getInstructionsMap());
        template.updateInstructions();
        template.setCreateTime(new Timestamp(new Date().getTime()));
        template.setUsageCount(0L);

        return template;
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

    private static Map<String, String> parseRequestBodyInstructionsMap(JsonObject jsonObject) {
        Map<String, String> instructionsMap = new HashMap<>();
        JsonArray instructionsJsonArray = jsonObject.getJsonArray("instructions");
        if (instructionsJsonArray == null) {
            return null;
        }
        for (JsonObject instructionJsonObject : instructionsJsonArray.getValuesAs(JsonObject.class)) {
            instructionsMap.put(
                    instructionJsonObject.getString("instructionField"),
                    instructionJsonObject.getString("instructionText")
            );
        }
        return instructionsMap;
    }
}
