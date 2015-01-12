package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.Highlight;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class SolrSearchResult {

    private static final Logger logger = Logger.getLogger(SolrSearchResult.class.getCanonicalName());

    private String id;
    private Long entityId;
    private String identifier;
    private String type;
    private String url;
    private String persistentUrl;
    private String query;
    private String name;
    private String nameSort;
    private String status;
    private Date releaseOrCreateDate;
    private String dateToDisplayOnCard;

    /**
     * @todo: how important is it to differentiate between name and title?
     */
    private String title;
    private String descriptionNoSnippet;
    private List<String> datasetAuthors = new ArrayList<>();
    private List<Highlight> highlightsAsList = new ArrayList<>();
    private Map<SolrField, Highlight> highlightsMap;
    private Map<String, Highlight> highlightsAsMap;

    // parent can be dataverse or dataset, store the name and id
    private Map<String, String> parent;
    // used on the SearchPage but not the search API
    private List<Dataset> datasets;
    private String dataverseAffiliation;
    private String citation;
    private String filetype;
    private String dataverseAlias;
    private String dataverseParentAlias;
//    private boolean statePublished;
    /**
     * @todo Investigate/remove this "unpublishedState" variable. For files that
     * have been published along with a dataset it says "true", which makes no
     * sense.
     */
    private boolean unpublishedState;
    private boolean draftState;
    private boolean deaccessionedState;
    private long datasetVersionId;

//    public boolean isStatePublished() {
//        return statePublished;
//    }
//    public void setStatePublished(boolean statePublished) {
//        this.statePublished = statePublished;
//    }
    public boolean isUnpublishedState() {
        return unpublishedState;
    }

    public void setUnpublishedState(boolean unpublishedState) {
        this.unpublishedState = unpublishedState;
    }

    public boolean isDraftState() {
        return draftState;
    }

    public void setDraftState(boolean draftState) {
        this.draftState = draftState;
    }

    public boolean isDeaccessionedState() {
        return deaccessionedState;
    }

    public void setDeaccessionedState(boolean deaccessionedState) {
        this.deaccessionedState = deaccessionedState;
    }
    /**
     * @todo: used? remove
     */
    private List<String> matchedFields;

    /**
     * @todo: remove name?
     */
    SolrSearchResult(String queryFromUser, String name) {
        this.query = queryFromUser;
//        this.name = name;
    }

    public Map<String, Highlight> getHighlightsAsMap() {
        return highlightsAsMap;
    }

    public void setHighlightsAsMap(Map<String, Highlight> highlightsAsMap) {
        this.highlightsAsMap = highlightsAsMap;
    }

    public String getNameHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.NAME);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getDataverseAffiliationHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.AFFILIATION);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getFileTypeHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.FILE_TYPE_MIME);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getTitleHighlightSnippet() {
        /**
         * @todo: don't hard-code title, look it up properly... or start
         * indexing titles as names:
         * https://redmine.hmdc.harvard.edu/issues/3798#note-2
         */
        Highlight highlight = highlightsAsMap.get("title");
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public List<String> getDescriptionSnippets() {
        for (Map.Entry<SolrField, Highlight> entry : highlightsMap.entrySet()) {
            SolrField solrField = entry.getKey();
            Highlight highlight = entry.getValue();
            logger.info("SolrSearchResult class: " + solrField.getNameSearchable() + ":" + highlight.getSnippets());
        }

        Highlight highlight = highlightsAsMap.get(SearchFields.DESCRIPTION);
        if (type.equals("datasets")) {
            highlight = highlightsAsMap.get(SearchFields.DATASET_DESCRIPTION);
        }
        if (highlight != null) {
            return highlight.getSnippets();
        } else {
            return new ArrayList<>();
        }
    }

    public Map<SolrField, Highlight> getHighlightsMap() {
        return highlightsMap;
    }

    public void setHighlightsMap(Map<SolrField, Highlight> highlightsMap) {
        this.highlightsMap = highlightsMap;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    @Override
    public String toString() {
        if (this.name != null) {
            return this.id + ":" + this.name + ":" + this.entityId;
        } else {
            return this.id + ":" + this.title + ":" + this.entityId;
        }
    }

    public JsonArrayBuilder getRelevance() {
        JsonArrayBuilder matchedFieldsArray = Json.createArrayBuilder();
        JsonObjectBuilder matchedFieldObject = Json.createObjectBuilder();
        for (Map.Entry<SolrField, Highlight> entry : highlightsMap.entrySet()) {
            SolrField solrField = entry.getKey();
            Highlight snippets = entry.getValue();
            JsonArrayBuilder snippetArrayBuilder = Json.createArrayBuilder();
            JsonObjectBuilder matchedFieldDetails = Json.createObjectBuilder();
            for (String highlight : snippets.getSnippets()) {
                snippetArrayBuilder.add(highlight);
            }
            /**
             * @todo for the Search API, it might be nice to return offset
             * numbers rather than html snippets surrounded by span tags or
             * whatever.
             *
             * That's what the GitHub Search API does: "Requests can opt to
             * receive those text fragments in the response, and every fragment
             * is accompanied by numeric offsets identifying the exact location
             * of each matching search term."
             * https://developer.github.com/v3/search/#text-match-metadata
             *
             * It's not clear if getting the offset values is possible with
             * Solr, however:
             * stackoverflow.com/questions/13863118/can-solr-highlighting-also-indicate-the-position-or-offset-of-the-returned-fragments-within-the-original-field
             */
            matchedFieldDetails.add("snippets", snippetArrayBuilder);
            /**
             * @todo In addition to the name of the field used by Solr , it
             * would be nice to show the "friendly" name of the field we show in
             * the GUI.
             */
//            matchedFieldDetails.add("friendly", "FIXME");
            matchedFieldObject.add(solrField.getNameSearchable(), matchedFieldDetails);
            matchedFieldsArray.add(matchedFieldObject);
        }
        return matchedFieldsArray;
    }

    public JsonObject toJsonObject(boolean showRelevance, boolean showEntityIds) {
        return json(showRelevance, showEntityIds).build();
    }

    public JsonObjectBuilder json(boolean showRelevance, boolean showEntityIds) {

        if (this.type == null) {
            return jsonObjectBuilder();
        }

        String displayName = null;

        String identifierLabel = null;
        if (this.type.equals(SearchConstants.DATAVERSES)) {
            displayName = this.name;
            identifierLabel = "identifier";
        } else if (this.type.equals(SearchConstants.DATASETS)) {
            displayName = this.title;
            identifierLabel = "global_id";
            /**
             * @todo Should we show the name of the parent dataverse?
             */
        } else if (this.type.equals(SearchConstants.FILES)) {
            displayName = this.name;
            identifierLabel = "file_id";
            /**
             * @todo show more information for a file's parent, such as the
             * title of the dataset it belongs to.
             */
        }

        //displayName = null; // testing NullSafeJsonBuilder
        // because we are using NullSafeJsonBuilder key/value pairs will be dropped if the value is null
        NullSafeJsonBuilder nullSafeJsonBuilder = jsonObjectBuilder()
                .add("name", displayName)
                .add("entity_id", this.entityId)
                .add("type", getDisplayType(getType()))
                /**
                 * @todo We should probably have a metadata_url or api_url
                 * concept. For datasets, this would be
                 * http://example.com/api/datasets/10 or whatever (to get more
                 * detailed JSON), but right now this requires an API token.
                 * Discuss at
                 * https://docs.google.com/document/d/1d8sT2GLSavgiAuMTVX8KzTCX0lROEET1edhvHHRDZOs/edit?usp=sharing";
                 */
                .add("html_url", getUrl())
                .add("persistent_url", this.persistentUrl)
                /**
                 * @todo How much value is there in exposing the identifier for
                 * dataverses? For
                 */
                .add(identifierLabel, this.identifier)
                /**
                 * @todo Get dataset description from dsDescriptionValue. Also,
                 * is descriptionNoSnippet the right field to use generally?
                 *
                 * @todo What about the fact that datasets can now have multiple
                 * descriptions? Should we create an array called
                 * "additional_descriptions" that gets populated if there is
                 * more than one dataset description?
                 *
                 * @todo Why aren't file descriptions ever null? They always
                 * have an empty string at least.
                 */
                .add("description", this.descriptionNoSnippet)
                /**
                 * @todo In the future we'd like to support non-public datasets
                 * per https://github.com/IQSS/dataverse/issues/1299 but for now
                 * we are only supporting non-public searches.
                 */
                .add("published_at", getDateTimePublished())
                /**
                 * @todo Maybe we should expose MIME Type also.
                 */
                .add("file_type", this.filetype)
                .add("citation", this.citation);
        if (showRelevance) {
            nullSafeJsonBuilder.add("matches", getRelevance());
        }
        // NullSafeJsonBuilder is awesome but can't build null safe arrays. :(
        if (!datasetAuthors.isEmpty()) {
            JsonArrayBuilder authors = Json.createArrayBuilder();
            for (String datasetAuthor : datasetAuthors) {
                authors.add(datasetAuthor);
            }
            nullSafeJsonBuilder.add("authors", authors);
        }
        return nullSafeJsonBuilder;
    }

    private String getDateTimePublished() {
        String datePublished = null;
        if (draftState == false) {
            datePublished = Util.getDateTimeFormatToReturnIn(releaseOrCreateDate);
        }
        return datePublished;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPersistentUrl() {
        return persistentUrl;
    }

    public void setPersistentUrl(String persistentUrl) {
        this.persistentUrl = persistentUrl;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionNoSnippet() {
        return descriptionNoSnippet;
    }

    public void setDescriptionNoSnippet(String descriptionNoSnippet) {
        this.descriptionNoSnippet = descriptionNoSnippet;
    }

    public List<String> getDatasetAuthors() {
        return datasetAuthors;
    }

    public void setDatasetAuthors(List<String> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }

    public List<Highlight> getHighlightsAsListOrig() {
        return highlightsAsList;
    }

    public List<Highlight> getHighlightsAsList() {
        List<Highlight> filtered = new ArrayList<>();
        for (Highlight highlight : highlightsAsList) {
            String field = highlight.getSolrField().getNameSearchable();
            /**
             * @todo don't hard code "title" here. And should we collapse name
             * and title together anyway?
             */
            if (!field.equals(SearchFields.NAME)
                    && !field.equals(SearchFields.DESCRIPTION)
                    && !field.equals(SearchFields.DATASET_DESCRIPTION)
                    && !field.equals(SearchFields.AFFILIATION)
                    && !field.equals("title")) {
                filtered.add(highlight);
            }
        }
        return filtered;
    }

    public void setHighlightsAsList(List<Highlight> highlightsAsList) {
        this.highlightsAsList = highlightsAsList;
    }

    public Map<String, String> getParent() {
        return parent;
    }

    public void setParent(Map<String, String> parent) {
        this.parent = parent;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public String getDataverseAffiliation() {
        return dataverseAffiliation;
    }

    public void setDataverseAffiliation(String dataverseAffiliation) {
        this.dataverseAffiliation = dataverseAffiliation;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public String getNameSort() {
        return nameSort;
    }

    public void setNameSort(String nameSort) {
        this.nameSort = nameSort;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    public Date getReleaseOrCreateDate() {
        return releaseOrCreateDate;
    }

    public void setReleaseOrCreateDate(Date releaseOrCreateDate) {
        this.releaseOrCreateDate = releaseOrCreateDate;
    }

    public String getDateToDisplayOnCard() {
        return dateToDisplayOnCard;
    }

    public void setDateToDisplayOnCard(String dateToDisplayOnCard) {
        this.dateToDisplayOnCard = dateToDisplayOnCard;
    }

    public long getDatasetVersionId() {
        return datasetVersionId;
    }

    public void setDatasetVersionId(long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    public String getDatasetUrl() {
        return "/dataset.xhtml?id=" + entityId + "&versionId=" + datasetVersionId;
    }

    public String getFileUrl() {
        return "/dataset.xhtml?id=" + parent.get(SearchFields.ID) + "&versionId=" + datasetVersionId;
    }

    /**
     * @return the dataverseAlias
     */
    public String getDataverseAlias() {
        return dataverseAlias;
    }

    /**
     * @param dataverseAlias the dataverseAlias to set
     */
    public void setDataverseAlias(String dataverseAlias) {
        this.dataverseAlias = dataverseAlias;
    }

    /**
     * @return the dataverseParentAlias
     */
    public String getDataverseParentAlias() {
        return dataverseParentAlias;
    }

    /**
     * @param dataverseParentAlias the dataverseParentAlias to set
     */
    public void setDataverseParentAlias(String dataverseParentAlias) {
        this.dataverseParentAlias = dataverseParentAlias;
    }

    private String getDisplayType(String type) {
        if (type.equals(SearchConstants.DATAVERSES)) {
            return SearchConstants.DATAVERSE;
        } else if (type.equals(SearchConstants.DATASETS)) {
            return SearchConstants.DATASET;
        } else if (type.equals(SearchConstants.FILES)) {
            return SearchConstants.FILE;
        } else {
            return null;
        }
    }

}
