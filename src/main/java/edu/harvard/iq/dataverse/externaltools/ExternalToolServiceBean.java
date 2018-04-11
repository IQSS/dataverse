package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.ReservedWord;
import static edu.harvard.iq.dataverse.externaltools.ExternalTool.DESCRIPTION;
import static edu.harvard.iq.dataverse.externaltools.ExternalTool.DISPLAY_NAME;
import static edu.harvard.iq.dataverse.externaltools.ExternalTool.TOOL_PARAMETERS;
import static edu.harvard.iq.dataverse.externaltools.ExternalTool.TOOL_URL;
import static edu.harvard.iq.dataverse.externaltools.ExternalTool.TYPE;
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


    /**
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findByType(ExternalTool.Type type) {
        List<ExternalTool> externalTools = new ArrayList<>();
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.type = :type", ExternalTool.class);
        typedQuery.setParameter("type", type);
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
     * This method takes a list of tools and a file and returns which tools that file supports
     * The list of tools is passed in so it doesn't hit the database each time
     */
    public static List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        List<ExternalTool> externalTools = new ArrayList<>();
        allExternalTools.forEach((externalTool) -> {
            if (file.isTabularData()) {
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
        String displayName = getRequiredTopLevelField(jsonObject, DISPLAY_NAME);
        String description = getRequiredTopLevelField(jsonObject, DESCRIPTION);
        String typeUserInput = getRequiredTopLevelField(jsonObject, TYPE);
        // Allow IllegalArgumentException to bubble up from ExternalTool.Type.fromString
        ExternalTool.Type type = ExternalTool.Type.fromString(typeUserInput);
        String toolUrl = getRequiredTopLevelField(jsonObject, TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray("queryParameters");
        boolean allRequiredReservedWordsFound = false;
        for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
            Set<String> keyValuePair = queryParam.keySet();
            for (String key : keyValuePair) {
                String value = queryParam.getString(key);
                ReservedWord reservedWord = ReservedWord.fromString(value);
                if (reservedWord.equals(ReservedWord.FILE_ID)) {
                    allRequiredReservedWordsFound = true;
                }
            }
        }
        if (!allRequiredReservedWordsFound) {
            // Some day there might be more reserved words than just {fileId}.
            throw new IllegalArgumentException("Required reserved word not found: " + ReservedWord.FILE_ID.toString());
        }
        String toolParameters = toolParametersObj.toString();
        return new ExternalTool(displayName, description, type, toolUrl, toolParameters);
    }

    private static String getRequiredTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

}
