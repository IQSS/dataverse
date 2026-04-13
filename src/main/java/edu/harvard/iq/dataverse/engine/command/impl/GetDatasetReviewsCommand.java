package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SortBy;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

// No annotations here since permissions are dynamically decided
public class GetDatasetReviewsCommand extends AbstractCommand<JsonObjectBuilder> {

    private final Dataset dataset;

    public GetDatasetReviewsCommand(DataverseRequest request, Dataset target) {
        super(request, target);
        dataset = target;
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        JsonObjectBuilder reviews = Json.createObjectBuilder();
        List<Dataverse> dataverses = new ArrayList<>();
        // Putting PID as URL in quotes to avoid hits we don't want
        String query = "itemReviewedUrl:\"" + dataset.getGlobalId().asURL() + "\"";
        List<String> filterQueries = new ArrayList<>();
        // Limit to datasets (review datasets)
        filterQueries.add(SearchFields.TYPE + ":" + SearchConstants.DATASETS);
        String sortField = SearchFields.ID;
        String sortOrder = SortBy.ASCENDING;
        int paginationStart = 0;
        boolean dataRelatedToMe = false;
        // We only expect a handful of reviews. This should be plenty.
        int numResultsPerPage = 100;
        try {
            SolrQueryResponse solrQueryResponse = ctxt.search().getDefaultSearchService().search(getRequest(),
                    dataverses, query, filterQueries, sortField, sortOrder, paginationStart, dataRelatedToMe,
                    numResultsPerPage);
            JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
            List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                // Construct a JSON object intentionally rather than simply returning the
                // solrSearchResult. This "get reviews" command may be powered by a
                // database query in the future and we'll want to preserve the contract
                // we're establishing here if we make the switch from Solr.
                JsonObjectBuilder searchResultBuilder = solrSearchResult.json(false, true, false);
                JsonObject searchResultObject = searchResultBuilder.build();
                String title = searchResultObject.getString("name");
                String citation = searchResultObject.getString("citation");
                String citationHtml = searchResultObject.getString("citationHtml");
                String pid = searchResultObject.getString("global_id");
                String pidUrl = searchResultObject.getString("url");
                long id = searchResultObject.getJsonNumber("entity_id").longValue();
                JsonObjectBuilder review = Json.createObjectBuilder()
                        .add("title", title)
                        .add("persistentId", pid)
                        .add("persistentIdUrl", pidUrl)
                        .add("id", id)
                        .add("citation", citation)
                        .add("citationHtml", citationHtml);
                itemsArrayBuilder.add(review);
            }
            reviews.add("reviews", itemsArrayBuilder);
        } catch (SearchException ex) {
            throw new CommandException(ex.getMessage(), this);
        }
        return reviews;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataset.isReleased() ? Collections.<Permission>emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataset));
    }

}
