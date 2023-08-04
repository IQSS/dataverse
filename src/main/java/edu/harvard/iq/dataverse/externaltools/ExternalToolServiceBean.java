package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.AuxiliaryFile;
import edu.harvard.iq.dataverse.AuxiliaryFileServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.Type;
import edu.harvard.iq.dataverse.util.URLTokenUtil;
import edu.harvard.iq.dataverse.util.URLTokenUtil.ReservedWord;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ejb.EJB;
import javax.json.JsonValue;

@Stateless
@Named
public class ExternalToolServiceBean {

    private static final Logger logger = Logger.getLogger(ExternalToolServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    AuxiliaryFileServiceBean auxiliaryFileService;

    public List<ExternalTool> findAll() {
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o ORDER BY o.id", ExternalTool.class);
        return typedQuery.getResultList();
    }

    /**
     * @param type explore, configure or preview
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findDatasetToolsByType(Type type) {
        String nullContentType = null;
        return findByScopeTypeAndContentType(ExternalTool.Scope.DATASET, type, nullContentType);
    }

    /**
     * @param type explore, configure or preview, query
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findFileToolsByType(Type type) {
        String nullContentType = null;
        return findByScopeTypeAndContentType(ExternalTool.Scope.FILE, type, nullContentType);
    }

    /**
     * @param type explore, configure or preview, query
     * @param contentType file content type (MIME type)
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findFileToolsByTypeAndContentType(Type type, String contentType) {
        return findByScopeTypeAndContentType(ExternalTool.Scope.FILE, type, contentType);
    }

    /**
     * @param scope dataset or file
     * @param type explore, configure, or preview
     * @param contentType file content type (MIME type)
     * @return A list of tools or an empty list.
     */
    private List<ExternalTool> findByScopeTypeAndContentType(Scope scope, Type type, String contentType) {
        List<ExternalTool> externalTools = new ArrayList<>();
        String contentTypeClause = "";
        if (contentType != null) {
            contentTypeClause = "AND o.contentType = :contentType";
        }
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o JOIN o.externalToolTypes t WHERE o.scope = :scope AND t.type = :type " + contentTypeClause, ExternalTool.class);
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
        for (ExternalToolType externalToolType : externalTool.getExternalToolTypes()) {
            // Avoid ERROR: null value in column "externaltool_id" violates not-null constraint
            externalToolType.setExternalTool(externalTool);
        }
        em.persist(externalTool);
        return em.merge(externalTool);
    }

    /**
     * This method takes a list of tools and a file and returns which tools that
     * file supports The list of tools is passed in so it doesn't hit the
     * database each time
     */
    public List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        List<ExternalTool> externalTools = new ArrayList<>();
        //Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before, but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? DataFileServiceBean.MIME_TYPE_TSV_ALT : file.getContentType();
        allExternalTools.forEach((externalTool) -> {
            //Match tool and file type, then check requirements
            if (contentType.equals(externalTool.getContentType()) && meetsRequirements(externalTool, file)) {
                externalTools.add(externalTool);
            }
        });

        return externalTools;
    }

    public boolean meetsRequirements(ExternalTool externalTool, DataFile dataFile) {
        String requirements = externalTool.getRequirements();
        if (requirements == null) {
            logger.fine("Data file id" + dataFile.getId() + ": no requirements for tool id " + externalTool.getId());
            return true;
        }
        boolean meetsRequirements = true;
        JsonObject requirementsObj = JsonUtil.getJsonObject(requirements);
        JsonArray auxFilesExist = requirementsObj.getJsonArray("auxFilesExist");
        for (JsonValue jsonValue : auxFilesExist) {
            String formatTag = jsonValue.asJsonObject().getString("formatTag");
            String formatVersion = jsonValue.asJsonObject().getString("formatVersion");
            AuxiliaryFile auxFile = auxiliaryFileService.lookupAuxiliaryFile(dataFile, formatTag, formatVersion);
            if (auxFile == null) {
                logger.fine("Data file id" + dataFile.getId() + ": cannot find required aux file. formatTag=" + formatTag + ". formatVersion=" + formatVersion);
                meetsRequirements = false;
                break;
            } else {
                logger.fine("Data file id" + dataFile.getId() + ": found required aux file. formatTag=" + formatTag + ". formatVersion=" + formatVersion);
                meetsRequirements = true;
            }
        }
        return meetsRequirements;
    }

    public static ExternalTool parseAddExternalToolManifest(String manifest) {

        if (manifest == null || manifest.isEmpty()) {
            throw new IllegalArgumentException("External tool manifest was null or empty!");
        }
        JsonObject jsonObject = JsonUtil.getJsonObject(manifest);
        //Note: ExternalToolServiceBeanTest tests are dependent on the order of these retrievals
        String displayName = getRequiredTopLevelField(jsonObject, DISPLAY_NAME);
        String toolName = getOptionalTopLevelField(jsonObject, TOOL_NAME);
        String description = getRequiredTopLevelField(jsonObject, DESCRIPTION);
        // Types are complicated enough to warrant their own method.
        List<ExternalToolType> externalToolTypes = getAndValidateTypes(jsonObject);
        String scopeUserInput = getRequiredTopLevelField(jsonObject, SCOPE);
        String contentType = getOptionalTopLevelField(jsonObject, CONTENT_TYPE);

        ExternalTool.Scope scope = ExternalTool.Scope.fromString(scopeUserInput);
        if (scope.equals(Scope.FILE) && (contentType == null || contentType.isEmpty())) {
            contentType = getRequiredTopLevelField(jsonObject, CONTENT_TYPE);
        }
        String toolUrl = getRequiredTopLevelField(jsonObject, TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray("queryParameters");
        JsonArray allowedApiCallsArray = jsonObject.getJsonArray(ALLOWED_API_CALLS);
        JsonObject requirementsObj = jsonObject.getJsonObject(REQUIREMENTS);
 
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
        String allowedApiCalls = null;
        if(allowedApiCallsArray !=null) {
            allowedApiCalls = allowedApiCallsArray.toString();
        }
        String requirements = null;
        if (requirementsObj != null) {
            requirements = requirementsObj.toString();
        }

        return new ExternalTool(displayName, toolName, description, externalToolTypes, scope, toolUrl, toolParameters, contentType, allowedApiCalls, requirements);
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

    private static List<String> getRequiredTopLevelFieldArray(JsonObject jsonObject, String key) {
        try {
            List<String> returnList = new ArrayList<>();
            JsonArray jsonArray = jsonObject.getJsonArray(key);
            for (int i = 0; i < jsonArray.size(); i++) {
                String listItem = jsonArray.getString(i);
                returnList.add(listItem);
            }
            return returnList;
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

    /**
     * Throws exceptions to enforce that a type is present in the manifest and
     * that each type is one of the supported types.
     *
     * While the newer "types: [string]" form is greatly preferred, the older
     * "type: string" form is supported for backward compatibility.
     */
    private static List<ExternalToolType> getAndValidateTypes(JsonObject jsonObject) {
        List<ExternalToolType> externalToolTypes = new ArrayList<>();
        try {
            // Try to get the preferred "types" form, which supports multiple.
            List<String> typesUserInput = getRequiredTopLevelFieldArray(jsonObject, TYPES);
            for (String typeUserInput : typesUserInput) {
                ExternalToolType externalToolType = new ExternalToolType();
                try {
                    externalToolType.setType(ExternalTool.Type.fromString(typeUserInput));
                } catch (IllegalArgumentException ex) {
                    // The error we return here might be something like
                    // "Type must be one of these values:...".
                    // To let it bubble up we throw something other than
                    // IllegalArgumentException so it isn't caught below.
                    throw new RuntimeException(ex.getLocalizedMessage());
                }
                externalToolTypes.add(externalToolType);
            }
        } catch (IllegalArgumentException ex) {
            // Fallback to the legacy "type" form, a single type.
            // Known issue: If you pass an array in, you get a weird error.
            String typeUserInput = getRequiredTopLevelField(jsonObject, LEGACY_SINGLE_TYPE);
            ExternalToolType externalToolType = new ExternalToolType();
            // Allow IllegalArgumentException to bubble up from ExternalTool.Type.fromString
            externalToolType.setType(ExternalTool.Type.fromString(typeUserInput));
            externalToolTypes.add(externalToolType);
        }
        return externalToolTypes;
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
