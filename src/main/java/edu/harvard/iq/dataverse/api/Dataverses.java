package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("dataverses")
public class Dataverses {

    private static final Logger logger = Logger.getLogger(Dataverses.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;

    @GET
    public String get() {
        List<Dataverse> dataverses = dataverseService.findAll();
        JsonArrayBuilder dataversesArrayBuilder = Json.createArrayBuilder();
        for (Dataverse dataverse : dataverses) {
            dataversesArrayBuilder.add(dataverse2json(dataverse));
        }
        JsonArray jsonArray = dataversesArrayBuilder.build();
        return Util.jsonArray2prettyString(jsonArray);
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") Long id) {
        Dataverse dataverse = dataverseService.find(id);
        if (dataverse != null) {
            return Util.jsonObject2prettyString(dataverse2json(dataverse));
        } else {
            /**
             * @todo inconsistent with /{id}/dump which simply returns nothing
             * and "204 No Content"
             */
            return Util.message2ApiError("Dataverse id " + id + " not found");
        }
    }

    // used to primarily to feed data into elasticsearch
    @GET
    @Path("{id}/{verb}")
    public Dataverse get(@PathParam("id") Long id, @PathParam("verb") String verb) {
        if (verb.equals("dump")) {
            Dataverse dataverse = dataverseService.find(id);
            if (dataverse != null) {
                return dataverse;
            }
        }
        /**
         * @todo return an error instead of "204 No Content"?
         *
         */
        logger.info("GET attempted with dataverse id " + id + " and verb " + verb);
        return null;
    }

    @POST
    public String add(Dataverse dataverse) {
        Dataverse savedDataverse = null;
        try {
            /**
             * @todo Is all this necessary? It's a way of knowing if the save
             * failed due to the database being down.
             */
            savedDataverse = dataverseService.save(dataverse);
        } catch (EJBException ex) {
            logger.info(ex.getClass().getName() + " caused by " + ex.getCausedByException().getLocalizedMessage());
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex);
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
                    }
                }
            }
            return Util.message2ApiError("POST failed: " + sb.toString());
        } catch (Exception ex) {
            logger.info(ex.getClass().getCanonicalName() + ": " + ex.getLocalizedMessage());
            return "problem saving (and probably indexing) dataverse " + dataverse.getAlias() + "\n";
        }
        if (savedDataverse != null) {
            return "dataverse " + dataverse.getAlias() + " created/updated (and probably indexed, check server.log)\n";
        } else {
            return "problem saving (and probably indexing) dataverse " + dataverse.getAlias() + "\n";
        }
    }

    public JsonObject dataverse2json(Dataverse dataverse) {
        JsonObjectBuilder dataverseInfoBuilder = Json.createObjectBuilder()
                /**
                 * @todo refactor to be same as index service bean?
                 */
                .add(SearchFields.ID, "dataverse_" + dataverse.getId())
                .add(SearchFields.ENTITY_ID, dataverse.getId())
                .add(SearchFields.TYPE, "dataverses")
                .add(SearchFields.NAME, dataverse.getName())
                .add(SearchFields.DESCRIPTION, dataverse.getDescription())
                .add(SearchFields.CATEGORY, dataverse.getAffiliation());
//                .add(SearchFields.AFFILIATION, dataverse.getAffiliation());
        return dataverseInfoBuilder.build();
    }
}
