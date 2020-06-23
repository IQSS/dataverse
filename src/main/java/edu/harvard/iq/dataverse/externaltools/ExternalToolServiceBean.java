package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.Type;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.Scope;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import static edu.harvard.iq.dataverse.externaltools.ExternalTool.*;

@Stateless
@Named
public class ExternalToolServiceBean {

    private static final Logger logger = Logger.getLogger(ExternalToolServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<ExternalTool> findAll() {
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o ORDER BY o.id", ExternalTool.class);
        return typedQuery.getResultList();
    }

    /**
     * @param type explore or configure
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findDatasetToolsByType(Type type) {
        String nullContentType = null;
        String nullPreviewAvailable = null;
        return findByScopeTypeAndContentType(ExternalTool.Scope.DATASET, type, nullContentType, nullPreviewAvailable);
    }

    /**
     * @param type explore or configure
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findFileToolsByType(Type type) {
        String nullContentType = null;
        String nullPreviewAvailable = null;
        return findByScopeTypeAndContentType(ExternalTool.Scope.FILE, type, nullContentType, nullPreviewAvailable);
    }

    /**
     * @param type explore or configure
     * @param contentType file content type (MIME type)
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findFileToolsByTypeAndContentType(Type type, String contentType) {
        String nullPreviewAvailable = null;
        return findByScopeTypeAndContentType(ExternalTool.Scope.FILE, type, contentType, nullPreviewAvailable);
    }
    
    /**
     * @param type explore or configure
     * @param contentType file content type (MIME type)
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findFileToolsByTypeContentTypeAndAvailablePreview(Type type, String contentType) {
        String previewAvailable = "true";
        return findByScopeTypeAndContentType(ExternalTool.Scope.FILE, type, contentType, previewAvailable);
    }

    /**
     * @param scope dataset or file
     * @param type explore or configure
     * @param contentType file content type (MIME type)
     * @return A list of tools or an empty list.
     */
    private List<ExternalTool> findByScopeTypeAndContentType(Scope scope, Type type, String contentType, String previewAvailable) {
        List<ExternalTool> externalTools = new ArrayList<>();
        String contentTypeClause = "";
        if (contentType != null) {
            contentTypeClause = "AND o.contentType = :contentType";
        }
        String previewAvailableClause = "";
        if (previewAvailable != null) {
            previewAvailableClause = " AND o.hasPreviewMode = 'true'";
        }
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.scope = :scope AND o.type = :type " + contentTypeClause + previewAvailableClause, ExternalTool.class);
        typedQuery.setParameter("scope", scope);
        typedQuery.setParameter("type", type);
        if (contentType != null) {
            typedQuery.setParameter("contentType", contentType);
        }
        List<ExternalTool> toolsFromQuery = typedQuery.getResultList();
        if (toolsFromQuery != null) {
            externalTools = toolsFromQuery;
        }
        return externalTools;
    }

    public ExternalTool findById(long id) {
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.id = :id", ExternalTool.class);
        typedQuery.setParameter("id", id);
        try {
            ExternalTool externalTool = typedQuery.getSingleResult();
            return externalTool;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public boolean delete(long doomedId) {
        ExternalTool doomed = findById(doomedId);
        try {
            em.remove(doomed);
            return true;
        } catch (Exception ex) {
            logger.info("Could not delete external tool with id of " + doomedId);
            return false;
        }
    }

    public ExternalTool save(ExternalTool externalTool) {
        em.persist(externalTool);
        return em.merge(externalTool);
    }

    /**
     * This method takes a list of tools and a file and returns which tools that
     * file supports The list of tools is passed in so it doesn't hit the
     * database each time
     */
    public static List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        List<ExternalTool> externalTools = new ArrayList<>();
        //Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before, but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? DataFileServiceBean.MIME_TYPE_TSV_ALT : file.getContentType();
        allExternalTools.forEach((externalTool) -> {
            //Match tool and file type 
            if (contentType.equals(externalTool.getContentType())) {
                externalTools.add(externalTool);
            }
        });

        return externalTools;
    }

    public static ExternalTool parseAddExternalToolManifest(String manifest) {

        if (manifest == null || manifest.isEmpty()) {
            throw new IllegalArgumentException("External tool manifest was null or empty!");
        }
        JsonReader jsonReader = Json.createReader(new StringReader(manifest));
        JsonObject jsonObject = jsonReader.readObject();
        //Note: ExternalToolServiceBeanTest tests are dependent on the order of these retrievals
        String displayName = getRequiredTopLevelField(jsonObject, DISPLAY_NAME);
        String toolName = getOptionalTopLevelField(jsonObject, TOOL_NAME);
        String description = getRequiredTopLevelField(jsonObject, DESCRIPTION);
        String typeUserInput = getRequiredTopLevelField(jsonObject, TYPE);
        String scopeUserInput = getRequiredTopLevelField(jsonObject, SCOPE);
        String contentType = getOptionalTopLevelField(jsonObject, CONTENT_TYPE);
        

        // Allow IllegalArgumentException to bubble up from ExternalTool.Type.fromString
        ExternalTool.Type type = ExternalTool.Type.fromString(typeUserInput);
        ExternalTool.Scope scope = ExternalTool.Scope.fromString(scopeUserInput);
        if (scope.equals(Scope.FILE) && (contentType == null || contentType.isEmpty())) {
            contentType = getRequiredTopLevelField(jsonObject, CONTENT_TYPE);
        }
        String toolUrl = getRequiredTopLevelField(jsonObject, TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray("queryParameters");
        boolean allRequiredReservedWordsFound = false;
        if (scope.equals(Scope.FILE)) {
            List<ReservedWord> requiredReservedWordCandidates = new ArrayList<>();
            requiredReservedWordCandidates.add(ReservedWord.FILE_ID);
            requiredReservedWordCandidates.add(ReservedWord.FILE_PID);
            for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
                Set<String> keyValuePair = queryParam.keySet();
                for (String key : keyValuePair) {
                    String value = queryParam.getString(key);
                    ReservedWord reservedWord = ReservedWord.fromString(value);
                    for (ReservedWord requiredReservedWordCandidate : requiredReservedWordCandidates) {
                        if (reservedWord.equals(requiredReservedWordCandidate)) {
                            allRequiredReservedWordsFound = true;
                        }
                    }
                }
            }
            if (!allRequiredReservedWordsFound) {
                List<String> requiredReservedWordCandidatesString = new ArrayList<>();
                for (ReservedWord requiredReservedWordCandidate : requiredReservedWordCandidates) {
                    requiredReservedWordCandidatesString.add(requiredReservedWordCandidate.toString());
                }
                String friendly = String.join(", ", requiredReservedWordCandidatesString);
                throw new IllegalArgumentException("One of the following reserved words is required: " + friendly + ".");
            }
        } else if (scope.equals(Scope.DATASET)) {
            List<ReservedWord> requiredReservedWordCandidates = new ArrayList<>();
            requiredReservedWordCandidates.add(ReservedWord.DATASET_ID);
            requiredReservedWordCandidates.add(ReservedWord.DATASET_PID);
            for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
                Set<String> keyValuePair = queryParam.keySet();
                for (String key : keyValuePair) {
                    String value = queryParam.getString(key);
                    ReservedWord reservedWord = ReservedWord.fromString(value);
                    for (ReservedWord requiredReservedWordCandidate : requiredReservedWordCandidates) {
                        if (reservedWord.equals(requiredReservedWordCandidate)) {
                            allRequiredReservedWordsFound = true;
                        }
                    }
                }
            }
            if (!allRequiredReservedWordsFound) {
                List<String> requiredReservedWordCandidatesString = new ArrayList<>();
                for (ReservedWord requiredReservedWordCandidate : requiredReservedWordCandidates) {
                    requiredReservedWordCandidatesString.add(requiredReservedWordCandidate.toString());
                }
                String friendly = String.join(", ", requiredReservedWordCandidatesString);
                throw new IllegalArgumentException("One of the following reserved words is required: " + friendly + ".");
            }

        }
        String toolParameters = toolParametersObj.toString();

        String hasPreviewMode = getOptionalTopLevelField(jsonObject, HAS_PREVIEW_MODE);

        boolean hasPreviewModeBoolean = false;
        if(hasPreviewMode != null && hasPreviewMode.equals("true")){
            hasPreviewModeBoolean = true;
        }

        return new ExternalTool(displayName, toolName, description, type, scope, toolUrl, toolParameters, contentType, hasPreviewModeBoolean);
    }

    private static String getRequiredTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

    private static String getOptionalTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public ApiToken getApiToken(String apiTokenString) {
        ApiToken apiToken = null;
        if (apiTokenString != null) {
            apiToken = new ApiToken();
            apiToken.setTokenString(apiTokenString);
        }
        return apiToken;
    }

    public JsonObjectBuilder getToolAsJsonWithQueryParameters(ExternalToolHandler externalToolHandler) {
        JsonObjectBuilder toolToJson = externalToolHandler.getExternalTool().toJson();
        String toolUrlWithQueryParams = externalToolHandler.getToolUrlWithQueryParams();
        toolToJson.add("toolUrlWithQueryParams", toolUrlWithQueryParams);
        return toolToJson;
    }

}
