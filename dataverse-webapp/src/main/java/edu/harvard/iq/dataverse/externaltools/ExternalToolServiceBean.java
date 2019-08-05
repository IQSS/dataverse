package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;

import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.*;

@Stateless
public class ExternalToolServiceBean {

    private static final Logger logger = Logger.getLogger(ExternalToolServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<ExternalTool> findAll() {
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o ORDER BY o.id", ExternalTool.class);
        return typedQuery.getResultList();
    }


    /**
     * @param type
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findByType(Type type) {
        return findByType(type, null);
    }

    /**
     * @param type
     * @param contentType - mimetype
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findByType(Type type, String contentType) {

        List<ExternalTool> externalTools = new ArrayList<>();

        //If contentType==null, get all tools of the given ExternalTool.Type 
        TypedQuery<ExternalTool> typedQuery = contentType != null ? em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.type = :type AND o.contentType = :contentType", ExternalTool.class) :
                em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.type = :type", ExternalTool.class);
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
     * This method takes a list of tools and a file and returns which tools that file supports
     * The list of tools is passed in so it doesn't hit the database each time
     */
    public static List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        List<ExternalTool> externalTools = new ArrayList<>();
        //Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before, but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType();
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
        String description = getRequiredTopLevelField(jsonObject, DESCRIPTION);
        String typeUserInput = getRequiredTopLevelField(jsonObject, TYPE);
        String contentType = getOptionalTopLevelField(jsonObject, CONTENT_TYPE);
        //Legacy support - assume tool manifests without any mimetype are for tabular data
        if (contentType == null) {
            contentType = TextMimeType.TSV_ALT.getMimeValue();
        }

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
        return new ExternalTool(displayName, description, type, toolUrl, toolParameters, contentType);
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


}
