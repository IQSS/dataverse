package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("admin/savedsearches")
public class SavedSearches extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(SavedSearches.class.getCanonicalName());

    @GET
    public Response meta() {
        JsonArrayBuilder endpoints = Json.createArrayBuilder();
        endpoints.add("GET");
        endpoints.add("GET /list");
        endpoints.add("GET /id");
        endpoints.add("POST");
        endpoints.add("DELETE /id");
        return ok(endpoints);
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
        return ok(response);
    }

    @GET
    @Path("{id}")
    public Response show(@PathParam("id") long id) {
        SavedSearch savedSearch = savedSearchSvc.find(id);
        if (savedSearch != null) {
            JsonObjectBuilder response = toJson(savedSearch);
            return ok(response);
        } else {
            return error(NOT_FOUND, "Could not find saved search id " + id);
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
        savedSearchJson.add("query", savedSearch.getQuery());
        savedSearchJson.add("filterQueries", fqBuilder);
        savedSearchJson.add("id", savedSearchId);
        savedSearchJson.add("definitionPointId", definitionPoint.getId());
        savedSearchJson.add("definitionPointAlias", definitionPoint.getAlias());
        savedSearchJson.add("creatorId", savedSearch.getCreator().getId());
        return savedSearchJson;
    }

    @POST
    public Response add(JsonObject body) {

        if (body == null) {
            return error(BAD_REQUEST, "JSON is expected.");
        }

        String keyForAuthenticatedUserId = "creatorId";
        long creatorIdToLookUp;
        try {
            creatorIdToLookUp = body.getInt(keyForAuthenticatedUserId);
        } catch (NullPointerException ex) {
            return error(BAD_REQUEST, "Required field missing: " + keyForAuthenticatedUserId);
        } catch (ClassCastException ex) {
            return error(BAD_REQUEST, "A number is required for " + keyForAuthenticatedUserId);
        } catch (Exception ex) {
            return error(BAD_REQUEST, "Problem with " + keyForAuthenticatedUserId + ": " + ex);
        }

        AuthenticatedUser creator = authSvc.findByID(creatorIdToLookUp);
        if (creator == null) {
            return error(Response.Status.NOT_FOUND, "Could not find user based on " + keyForAuthenticatedUserId + ": " + creatorIdToLookUp);
        }

        String keyForQuery = "query";
        String query;
        try {
            query = body.getString(keyForQuery);
        } catch (NullPointerException ex) {
            return error(BAD_REQUEST, "Required field missing: " + keyForQuery);
        }

        String keyForDefinitionPointId = "definitionPointId";
        long dataverseIdToLookup;
        try {
            dataverseIdToLookup = body.getInt(keyForDefinitionPointId);
        } catch (NullPointerException ex) {
            return error(BAD_REQUEST, "Required field missing: " + keyForDefinitionPointId);
        } catch (ClassCastException ex) {
            return error(BAD_REQUEST, "A number is required for " + keyForDefinitionPointId);
        } catch (Exception ex) {
            return error(BAD_REQUEST, "Problem with " + keyForDefinitionPointId + ": " + ex);
        }
        Dataverse definitionPoint = dataverseSvc.find(dataverseIdToLookup);
        if (definitionPoint == null) {
            return error(NOT_FOUND, "Could not find a dataverse based on id " + dataverseIdToLookup);
        }

        SavedSearch toPersist = new SavedSearch(query, definitionPoint, creator);

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
            return error(BAD_REQUEST, "Problem getting filter queries: " + ex);
        }

        if (!savedSearchFilterQuerys.isEmpty()) {
            toPersist.setSavedSearchFilterQueries(savedSearchFilterQuerys);
        }

        try {
            SavedSearch persistedSavedSearch = savedSearchSvc.add(toPersist);
            return ok("Added: " + persistedSavedSearch);
        } catch (EJBException ex) {
            StringBuilder errors = new StringBuilder();
            Throwable throwable = ex.getCause();
            while (throwable != null) {
                errors.append(throwable).append(" ");
                throwable = throwable.getCause();
            }
            return error(BAD_REQUEST, "Problem adding saved search: " + errors);
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long doomedId) {
        boolean disabled = true;
        if (disabled) {
            return error(BAD_REQUEST, "Saved Searches can not safely be deleted because links can not safely be deleted. See https://github.com/IQSS/dataverse/issues/1364 for details.");
        }
        SavedSearch doomed = savedSearchSvc.find(doomedId);
        if (doomed == null) {
            return error(NOT_FOUND, "Could not find saved search id " + doomedId);
        }
        boolean wasDeleted = savedSearchSvc.delete(doomedId);
        if (wasDeleted) {
            return ok(Json.createObjectBuilder().add("Deleted", doomedId));
        } else {
            return error(INTERNAL_SERVER_ERROR, "Problem deleting id " + doomedId);
        }
    }

    @PUT
    @Path("makelinks/all")
    public Response makeLinksForAllSavedSearches(@QueryParam("debug") boolean debug) {
        JsonObjectBuilder makeLinksResponse;
        try {
            makeLinksResponse = savedSearchSvc.makeLinksForAllSavedSearches(debug);
            return ok(makeLinksResponse);
        } catch (CommandException ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        } catch (SearchException ex) {
            return error(INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        }
    }

    @PUT
    @Path("makelinks/{id}")
    public Response makeLinksForSingleSavedSearch(@PathParam("id") long savedSearchIdToLookUp, @QueryParam("debug") boolean debug) {
        SavedSearch savedSearchToMakeLinksFor = savedSearchSvc.find(savedSearchIdToLookUp);
        if (savedSearchToMakeLinksFor == null) {
            return error(BAD_REQUEST, "Count not find saved search id " + savedSearchIdToLookUp);
        }
        try {
            DataverseRequest dataverseRequest = new DataverseRequest(savedSearchToMakeLinksFor.getCreator(), SavedSearchServiceBean.getHttpServletRequest());
            JsonObjectBuilder response = savedSearchSvc.makeLinksForSingleSavedSearch(dataverseRequest, savedSearchToMakeLinksFor, debug);
            return ok(response);
        } catch (CommandException ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        } catch (SearchException ex) {
            return error(INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        }
    }

}
