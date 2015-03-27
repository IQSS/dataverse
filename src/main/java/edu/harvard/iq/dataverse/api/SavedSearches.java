package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("admin/savedsearches")
public class SavedSearches extends AbstractApiBean {

    @GET
    @Path("list")
    public Response list() {
        JsonArrayBuilder savedSearchesBuilder = Json.createArrayBuilder();
        List<SavedSearch> savedSearches = savedSearchSvc.findAll();
        for (SavedSearch savedSearch : savedSearches) {
            JsonObjectBuilder thisSavedSearch = Json.createObjectBuilder();
            long savedSearchId = savedSearch.getId();
            JsonArrayBuilder fqBuilder = Json.createArrayBuilder();
            for (SavedSearchFilterQuery fq : savedSearch.getSavedSearchFilterQueries()) {
                fqBuilder.add(fq.getFilterQuery());
            }
            thisSavedSearch.add("fq", fqBuilder);
            thisSavedSearch.add("id", savedSearchId);
            savedSearchesBuilder.add(thisSavedSearch);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("saved searches", savedSearchesBuilder);
        return okResponse(response);
    }

}
