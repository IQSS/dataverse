package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.*;

import java.sql.Timestamp;
import java.util.*;

public class TemplateDTO {

    private String name;
    private List<DatasetField> datasetFields;
    private Map<String, String> instructionsMap;
    private boolean isDefault;

    public static TemplateDTO fromRequestBody(String requestBody, JsonParser jsonParser) throws JsonParseException {
        TemplateDTO templateDTO = new TemplateDTO();

        JsonObject jsonObject = JsonUtil.getJsonObject(requestBody);

        templateDTO.name = jsonObject.getString("name");
        templateDTO.datasetFields = jsonParser.parseMultipleFields(jsonObject);
        templateDTO.instructionsMap = jsonParser.parseRequestBodyInstructionsMap(jsonObject);
        templateDTO.isDefault = jsonObject.getBoolean("isDefault", false);
               
        return templateDTO;
    }

    public Template toTemplate() {
        Template template = new Template();

        template.setDatasetFields(getDatasetFields());
        template.setName(getName());
        template.setInstructionsMap(getInstructionsMap());
        template.updateInstructions();
        template.setCreateTime(new Timestamp(new Date().getTime()));
        template.setUsageCount(0L);
        template.setIsDefaultForDataverse(isDefault);

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

}
