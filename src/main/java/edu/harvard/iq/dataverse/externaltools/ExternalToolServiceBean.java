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
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
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

    public List<ExternalToolHandler> findExternalToolHandlersByFile(DataFile file, ApiToken apiToken) {
        return findExternalToolHandlersByFile(findAll(), file, apiToken);
    }

    public ExternalTool save(ExternalTool externalTool) {
        em.persist(externalTool);
        return em.merge(externalTool);
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

    public static ExternalTool parseAddExternalToolInput(String userInput) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(userInput));
            JsonObject jsonObject = jsonReader.readObject();
            String displayName = jsonObject.getString(DISPLAY_NAME);
            String description = jsonObject.getString(DESCRIPTION);
            String toolUrl = jsonObject.getString(TOOL_URL);
            JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
            String toolParameters = toolParametersObj.toString();
            return new ExternalTool(displayName, description, toolUrl, toolParameters);
        } catch (Exception ex) {
            logger.info("Exception caught in parseAddExternalToolInput: " + ex);
            return null;
        }
    }

}
