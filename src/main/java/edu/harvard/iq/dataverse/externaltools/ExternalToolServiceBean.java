package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DESCRIPTION;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DISPLAY_NAME;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_PARAMETERS;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_URL;
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
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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

    public List<ExternalToolHandler> findExternalToolHandlersByFile(DataFile file, ApiToken apiToken) {
        return findExternalToolHandlersByFile(findAll(), file, apiToken);
    }

    public ExternalTool save(ExternalTool externalTool) {
        em.persist(externalTool);
        return em.merge(externalTool);
    }

    public enum ReservedWord {

        // TODO: Research if a format like "{reservedWord}" is easily parse-able or if another format would be
        // better. The choice of curly braces is somewhat arbitrary, but has been observed in documenation for
        // various REST APIs. For example, "Variable substitutions will be made when a variable is named in {brackets}."
        // from https://swagger.io/specification/#fixed-fields-29 but that's for URLs.
        FILE_ID("{fileId}"),
        API_TOKEN("{apiToken}");

        private final String text;

        private ReservedWord(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * This method takes a list of tools from the database and returns
     * "handlers" that have inserted parameters in the right places.
     */
    public static List<ExternalToolHandler> findExternalToolHandlersByFile(List<ExternalTool> externalTools, DataFile file, ApiToken apiToken) {
        List<ExternalToolHandler> externalToolHandlers = new ArrayList<>();
        externalTools.forEach((externalTool) -> {
            ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, file, apiToken);
            externalToolHandlers.add(externalToolHandler);
        });
        return externalToolHandlers;
    }

    public static ExternalTool parseAddExternalToolManifest(String manifest) {
        if (manifest == null || manifest.isEmpty()) {
            throw new IllegalArgumentException("External tool manifest was null or empty!");
        }
        JsonReader jsonReader = Json.createReader(new StringReader(manifest));
        JsonObject jsonObject = jsonReader.readObject();
        String displayName = getRequiredTopLevelField(jsonObject, DISPLAY_NAME);
        String description = getRequiredTopLevelField(jsonObject, DESCRIPTION);
        String toolUrl = getRequiredTopLevelField(jsonObject, TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray("queryParameters");
        boolean allRequiredReservedWordsFound = false;
        for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
            Set<String> keyValuePair = queryParam.keySet();
            for (String key : keyValuePair) {
                String value = queryParam.getString(key);
                if (value.equals(ReservedWord.FILE_ID.text)) {
                    // Good, not only is {fileId} is a valid reserved word, it's required.
                    allRequiredReservedWordsFound = true;
                } else if (value.equals(ReservedWord.API_TOKEN.text)) {
                    // Good, {apiToken} is a valid reserved word.
                } else {
                    // Only {fileId} and {apiToken} are valid. Fail fast.
                    // Note that the same IllegalArgumentException is thrown in ExternalToolHandler.getQueryParam
                    throw new IllegalArgumentException("Unknown reserved word: " + value);
                }
            }
        }
        if (!allRequiredReservedWordsFound) {
            // Some day there might be more reserved words than just {fileId}.
            throw new IllegalArgumentException("Required reserved word not found: " + ReservedWord.FILE_ID.text);
        }
        String toolParameters = toolParametersObj.toString();
        return new ExternalTool(displayName, description, toolUrl, toolParameters);
    }

    private static String getRequiredTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

}
