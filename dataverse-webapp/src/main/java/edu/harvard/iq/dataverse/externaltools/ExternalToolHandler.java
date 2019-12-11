package edu.harvard.iq.dataverse.externaltools;

import com.google.common.base.Preconditions;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
@Stateless
public class ExternalToolHandler {

    private static final Logger logger = Logger.getLogger(ExternalToolHandler.class.getCanonicalName());

    @Inject
    private SystemConfig systemConfig;
    
    
    
    // -------------------- LOGIC --------------------
    
    public String buildToolUrlWithQueryParams(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken) {
        Preconditions.checkNotNull(externalTool);
        Preconditions.checkNotNull(dataFile);
        
        return externalTool.getToolUrl() + getQueryParametersForUrl(externalTool, dataFile, apiToken, systemConfig.getDataverseSiteUrl());
    }

    // -------------------- PRIVATE --------------------
    
    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    private String getQueryParametersForUrl(ExternalTool externalTool, DataFile datafile, ApiToken apiToken, String dataverseUrl) {
        Dataset dataset = datafile.getFileMetadata().getDatasetVersion().getDataset();
        
        String queryString = parseToolParameters(externalTool).entrySet().stream()
                .map(keyValue -> new Tuple2<>(keyValue.getKey(), resolvePlaceholder(keyValue.getValue(),
                        datafile, dataset, apiToken, dataverseUrl)))
                .filter(keyValue -> StringUtils.isNotEmpty(keyValue._2()))
                .map(keyValue -> keyValue._1() + "=" + keyValue._2())
                .collect(Collectors.joining("&"));
        
        return "?" + queryString;
    }
    
    private Map<String, String> parseToolParameters(ExternalTool externalTool) {
        Map<String, String> toolParams = new HashMap<>();
        
        String toolParameters = externalTool.getToolParameters();
        JsonReader jsonReader = Json.createReader(new StringReader(toolParameters));
        JsonObject obj = jsonReader.readObject();
        JsonArray queryParams = obj.getJsonArray("queryParameters");
        if (queryParams == null || queryParams.isEmpty()) {
            return toolParams;
        }
        
        queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
            queryParam.keySet().forEach((key) -> {
                
                toolParams.put(key, queryParam.getString(key));
            });
        });
        
        return toolParams;
    }
    
    private String resolvePlaceholder(String value, DataFile datafile, Dataset dataset, ApiToken apiToken, String dataverseUrl) {
        ReservedWord reservedWord = ReservedWord.fromString(value);
        switch (reservedWord) {
            case FILE_ID:
                return datafile.getId().toString();
            case SITE_URL:
                return systemConfig.getDataverseSiteUrl();
            case API_TOKEN:
                String apiTokenString = null;
                if (apiToken != null) {
                    apiTokenString = apiToken.getTokenString();
                    return apiTokenString;
                }
                break;
            case DATASET_ID:
                return dataset.getId().toString();
            case DATASET_VERSION:
                String version = null;
                if (apiToken != null) {
                    version = dataset.getLatestVersion().getFriendlyVersionNumber();
                } else {
                    version = dataset.getLatestVersionForCopy().getFriendlyVersionNumber();
                }
                if (("DRAFT").equals(version)) {
                    version = ":draft"; // send the token needed in api calls that can be substituted for a numeric
                    // version.
                }
                return version;
            default:
                break;
        }
        return null;
    }

}
