package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("admin/savedsearches")
public class SavedSearches extends AbstractApiBean {

    @GET
    public Response meta() {
        JsonArrayBuilder endpoints = Json.createArrayBuilder();
        endpoints.add("GET");
        endpoints.add("GET /list");
        endpoints.add("GET /id");
        endpoints.add("POST");
        endpoints.add("DELETE /id");
        return okResponse(endpoints);
    }

    @GET
    @Path("list")
    public Response list() {
        JsonArrayBuilder savedSearchesBuilder = Json.createArrayBuilder();
        List<SavedSearch> savedSearches = savedSearchSvc.findAll();
        for (SavedSearch savedSearch : savedSearches) {
            JsonObjectBuilder thisSavedSearch = toJson(savedSearch);
            savedSearchesBuilder.add(thisSavedSearch);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("saved searches", savedSearchesBuilder);
        return okResponse(response);
    }

    @GET
    @Path("{id}")
    public Response show(@PathParam("id") long id) {
        SavedSearch savedSearch = savedSearchSvc.find(id);
        if (savedSearch != null) {
            JsonObjectBuilder response = toJson(savedSearch);
            return okResponse(response);
        } else {
            return errorResponse(NOT_FOUND, "Could not find saved search id " + id);
        }
    }

    private JsonObjectBuilder toJson(SavedSearch savedSearch) {
        JsonObjectBuilder savedSearchJson = Json.createObjectBuilder();
        long savedSearchId = savedSearch.getId();
        Dataverse definitionPoint = savedSearch.getDefinitionPoint();
        JsonArrayBuilder fqBuilder = Json.createArrayBuilder();
        for (SavedSearchFilterQuery fq : savedSearch.getSavedSearchFilterQueries()) {
            fqBuilder.add(fq.getFilterQuery());
        }
        savedSearchJson.add("filterQueries", fqBuilder);
        savedSearchJson.add("id", savedSearchId);
        savedSearchJson.add("definitionPointId", definitionPoint.getId());
        return savedSearchJson;
    }

    @POST
    public Response add(JsonObject body) {

        if (body == null) {
            return errorResponse(BAD_REQUEST, "JSON is expected.");
        }
        String keyForQuery = "query";
        String query;
        try {
            query = body.getString(keyForQuery);
        } catch (NullPointerException ex) {
            return errorResponse(BAD_REQUEST, "Required field missing: " + keyForQuery);
        }

        String keyForDefinitionPointId = "definitionPointId";
        long dataverseIdToLookup;
        try {
            dataverseIdToLookup = body.getInt(keyForDefinitionPointId);
        } catch (NullPointerException ex) {
            return errorResponse(BAD_REQUEST, "Required field missing: " + keyForDefinitionPointId + ". " + ex);
        } catch (ClassCastException ex) {
            return errorResponse(BAD_REQUEST, "A number is required for " + keyForDefinitionPointId);
        } catch (Exception ex) {
            return errorResponse(BAD_REQUEST, "Problem with " + keyForDefinitionPointId + ": " + ex);
        }
        Dataverse definitionPoint = dataverseSvc.find(dataverseIdToLookup);
        if (definitionPoint == null) {
            return errorResponse(NOT_FOUND, "Could not find a dataverse based on id " + dataverseIdToLookup);
        }

        SavedSearch toPersist = new SavedSearch(query, definitionPoint);

        String keyForFilterQueries = "filterQueries";
        List<SavedSearchFilterQuery> savedSearchFilterQuerys = new ArrayList<>();
        try {
            JsonArray filterQueries = body.getJsonArray(keyForFilterQueries);
            for (int i = 0; i < filterQueries.size(); i++) {
                String fq = filterQueries.getString(i);
                SavedSearchFilterQuery filterQuery = new SavedSearchFilterQuery(fq, toPersist);
                savedSearchFilterQuerys.add(filterQuery);
            }
        } catch (NullPointerException ex) {
            // filter queries are not required, keep going
        } catch (Exception ex) {
            return errorResponse(BAD_REQUEST, "Problem getting filter queries: " + ex);
        }

        if (!savedSearchFilterQuerys.isEmpty()) {
            toPersist.setSavedSearchFilterQueries(savedSearchFilterQuerys);
        }

        try {
            SavedSearch persistedSavedSearch = savedSearchSvc.add(toPersist);
            return okResponse("Added: " + persistedSavedSearch);
        } catch (EJBException ex) {
            StringBuilder errors = new StringBuilder();
            Throwable throwable = ex.getCause();
            while (throwable != null) {
                errors.append(throwable).append(" ");
                throwable = throwable.getCause();
            }
            return errorResponse(BAD_REQUEST, "Problem adding saved search: " + errors);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long doomedId) {
        SavedSearch doomed = savedSearchSvc.find(doomedId);
        if (doomed == null) {
            return errorResponse(NOT_FOUND, "Could not find saved search id " + doomedId);
        }
        boolean wasDeleted = savedSearchSvc.delete(doomedId);
        if (wasDeleted) {
            return okResponse("deleted id " + doomedId);
        } else {
            return errorResponse(INTERNAL_SERVER_ERROR, "Problem deleting id " + doomedId);
        }
    }
}
