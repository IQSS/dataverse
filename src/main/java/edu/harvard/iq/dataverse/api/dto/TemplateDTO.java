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
    private Map<String, String> termsOfUseAndAccess;

    public static TemplateDTO fromRequestBody(String requestBody, JsonParser jsonParser) throws JsonParseException {
        TemplateDTO templateDTO = new TemplateDTO();

        JsonObject jsonObject = JsonUtil.getJsonObject(requestBody);

        templateDTO.name = jsonObject.getString("name");
        templateDTO.datasetFields = jsonParser.parseMultipleFields(jsonObject);
        templateDTO.instructionsMap = parseRequestBodyInstructionsMap(jsonObject);
        templateDTO.isDefault = jsonObject.getBoolean("isDefault", false);
        templateDTO.termsOfUseAndAccess = parseRequestBodyTerms(jsonObject);
               
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
    
    private static Map<String, String> parseRequestBodyTerms(JsonObject jsonObject){
        Map<String, String> termsMap = new HashMap<>();
        JsonArray termsJsonArray = jsonObject.getJsonArray("termsOfUseAndAccess");
        if (termsJsonArray == null) {
            return null;
        }
        /*
        
        {
  "name": "CC BY 4.0"
}
                License license = termsOfUseAndAccess.getLicense();
        return jsonObjectBuilder()
                .add("id", termsOfUseAndAccess.getId())
                .add("license", license != null ? json(license) : null)
                .add("termsOfUse", termsOfUseAndAccess.getTermsOfUse())
                .add("termsOfAccess", termsOfUseAndAccess.getTermsOfAccess())
                .add("confidentialityDeclaration", termsOfUseAndAccess.getConfidentialityDeclaration())
                .add("specialPermissions", termsOfUseAndAccess.getSpecialPermissions())
                .add("restrictions", termsOfUseAndAccess.getRestrictions())
                .add("citationRequirements", termsOfUseAndAccess.getCitationRequirements())
                .add("depositorRequirements", termsOfUseAndAccess.getDepositorRequirements())
                .add("conditions", termsOfUseAndAccess.getConditions())
                .add("disclaimer", termsOfUseAndAccess.getDisclaimer())
                .add("dataAccessPlace", termsOfUseAndAccess.getDataAccessPlace())
                .add("originalArchive", termsOfUseAndAccess.getOriginalArchive())
                .add("availabilityStatus", termsOfUseAndAccess.getAvailabilityStatus())
                .add("sizeOfCollection", termsOfUseAndAccess.getSizeOfCollection())
                .add("studyCompletion", termsOfUseAndAccess.getStudyCompletion())
                .add("contactForAccess", termsOfUseAndAccess.getContactForAccess())
                .add("fileAccessRequest", termsOfUseAndAccess.isFileAccessRequest());
        */
        
        for (JsonObject instructionJsonObject : termsJsonArray.getValuesAs(JsonObject.class)) {
            termsMap.put(
                    instructionJsonObject.getString("instructionField"),
                    instructionJsonObject.getString("instructionText")
            );
        }
        return termsMap;
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
