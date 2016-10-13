package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
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
import java.math.BigDecimal;
import javax.json.JsonValue;

public class SolrSearchResult {

    private static final Logger logger = Logger.getLogger(SolrSearchResult.class.getCanonicalName());

    private String id;
    private Long entityId;
    private DvObject entity;
    private String identifier;
    private String type;
    private String htmlUrl;
    private String persistentUrl;
    private String downloadUrl;
    private String apiUrl;
    private String imageUrl;
    private boolean displayImage;
    private String query;
    private String name;
    private String nameSort;
    private String status;
    private Date releaseOrCreateDate;
    private String dateToDisplayOnCard;
    private List<String> publicationStatuses = new ArrayList<>();

    /**
     * @todo: how important is it to differentiate between name and title?
     */
    private String title;
    private String descriptionNoSnippet;
    private List<String> datasetAuthors = new ArrayList<>();
    private String deaccessionReason;
    private List<Highlight> highlightsAsList = new ArrayList<>();
    private Map<SolrField, Highlight> highlightsMap;
    private Map<String, Highlight> highlightsAsMap;

    // parent can be dataverse or dataset, store the name and id
    /**
     * The "identifier" of a file's parent (a dataset) is a globalId (often a
     * doi).
     */
    public static String PARENT_IDENTIFIER = "identifier";
    private Map<String, String> parent;
    private String dataverseAffiliation;
    private String citation;
    private String citationHtml;
    /**
     * Files and datasets might have a UNF. Dataverses don't.
     */
    private String unf;
    private String filetype;
    private String fileContentType;
    private Long fileSizeInBytes;
    /**
     * fileMD5 is here for legacy and backward-compatibility reasons. It might be deprecated some day in favor of "fileChecksumType" and "fileChecksumValue"
     */
    private String fileMd5;
    private DataFile.ChecksumType fileChecksumType;
    private String fileChecksumValue;
    private String dataverseAlias;
    private String dataverseParentAlias;
//    private boolean statePublished;
    /**
     * @todo Investigate/remove this "unpublishedState" variable. For files that
     * have been published along with a dataset it says "true", which makes no
     * sense.
     */
    private boolean publishedState = false;
    private boolean unpublishedState = false;
    private boolean draftState = false;
    private boolean inReviewState = false;
    private boolean deaccessionedState = false;
    private long datasetVersionId;
    private String versionNumberFriendly;
    //Determine if the search result is owned by any of the dvs in the tree of the DV displayed
    private boolean isInTree;
    private float score;
    private List<String> userRole;
    private boolean harvested = false;
    private String dvTree;
    private String harvestingDescription = null;
    private List<String> fileCategories = null;
    private List<String> tabularDataTags = null;
    
    public String getDvTree() {
        return dvTree;
    }
    
    public void setDvTree(String dvTree) {
        this.dvTree = dvTree;
    } 
    
    public boolean isIsInTree() {
        return isInTree;
    }

    public void setIsInTree(boolean isInTree) {
        this.isInTree = isInTree;
    }
    
    public boolean isHarvested() {
        return harvested;
    }

    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }
    
    public String getHarvestingDescription() {
        //if (this.isHarvested()) {
            return harvestingDescription;
        //}
        //return null;
    }
    
    public void setHarvestingDescription(String harvestingDescription) {
        this.harvestingDescription = harvestingDescription;
    }
//    public boolean isStatePublished() {
//        return statePublished;
//    }
//    public void setStatePublished(boolean statePublished) {
//        this.statePublished = statePublished;
//    }

    public boolean isPublishedState() {
        return publishedState;
    }

    public void setPublishedState(boolean publishedState) {
        this.publishedState = publishedState;
    }

    public boolean isUnpublishedState() {
        return unpublishedState;
    }

    public void setUnpublishedState(boolean unpublishedState) {
        this.unpublishedState = unpublishedState;
    }

    public void setPublicationStatuses(List<String> statuses) {

        if (statuses == null) {
            this.publicationStatuses = new ArrayList<>();
            return;
        }
        this.publicationStatuses = statuses;

        // set booleans for individual statuses
        //
        for (String status : this.publicationStatuses) {

            if (status.equals(IndexServiceBean.getUNPUBLISHED_STRING())) {
                this.setUnpublishedState(true);

            } else if (status.equals(IndexServiceBean.getPUBLISHED_STRING())) {
                this.setPublishedState(true);

            } else if (status.equals(IndexServiceBean.getDRAFT_STRING())) {
                this.setDraftState(true);

            } else if (status.equals(IndexServiceBean.getIN_REVIEW_STRING())) {
                this.setInReviewState(true);

            } else if (status.equals(IndexServiceBean.getDEACCESSIONED_STRING())) {
                this.setDeaccessionedState(true);
            }
        }
    } // setPublicationStatuses

    /**
     * Never return null, return an empty list instead
     *
     * @return
     */
    public List<String> getPublicationStatuses() {

        if (this.publicationStatuses == null) {
            this.publicationStatuses = new ArrayList<>();
        }
        return this.publicationStatuses;
    }

    public JsonArrayBuilder getPublicationStatusesAsJSON() {

        JsonArrayBuilder statuses = Json.createArrayBuilder();
        for (String status : this.getPublicationStatuses()) {
            statuses.add(status);
        }
        return statuses;
    }

    public boolean isDraftState() {
        return draftState;
    }

    public void setDraftState(boolean draftState) {
        this.draftState = draftState;
    }

    public boolean isInReviewState() {
        return inReviewState;
    }

    public void setInReviewState(boolean inReviewState) {
        this.inReviewState = inReviewState;
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
        Highlight highlight = highlightsAsMap.get(SearchFields.FILE_TYPE_FRIENDLY);
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
            logger.fine("SolrSearchResult class: " + solrField.getNameSearchable() + ":" + highlight.getSnippets());
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

    public JsonObject toJsonObject(boolean showRelevance, boolean showEntityIds, boolean showApiUrls) {
        return json(showRelevance, showEntityIds, showApiUrls).build();
    }

    /**
     * Add additional fields for the MyData page
     *
     * @return
     */
    public JsonObjectBuilder getJsonForMyData() {

        JsonObjectBuilder myDataJson = json(true, true, true);//boolean showRelevance, boolean showEntityIds, boolean showApiUrls) 

        myDataJson.add("publication_statuses", this.getPublicationStatusesAsJSON())
                .add("is_draft_state", this.isDraftState())
                .add("is_in_review_state", this.isInReviewState())
                .add("is_unpublished_state", this.isUnpublishedState())
                .add("is_published", this.isPublishedState())
                .add("is_deaccesioned", this.isDeaccessionedState())
                .add("date_to_display_on_card", this.dateToDisplayOnCard);

        // Add is_deaccessioned attribute, even though MyData currently screens any deaccessioned info out
        //
        if ((this.isDeaccessionedState()) && (this.getPublicationStatuses().size() == 1)) {
            myDataJson.add("deaccesioned_is_only_pubstatus", true);
        }

        if ((this.getParent() != null) && (!this.getParent().isEmpty())) {
            //System.out.println("keys:" + parent.keySet().toString());
            if (this.entity.isInstanceofDataFile()) {
                myDataJson.add("parentIdentifier", this.getParent().get(SolrSearchResult.PARENT_IDENTIFIER))
                        .add("parentName", this.getParent().get("name"));

            } else {
                // for Dataverse and Dataset, get parent which is a Dataverse
                myDataJson.add("parentId", this.getParent().get("id"))
                        .add("parentName", this.getParent().get("name"));
            }
        }

        return myDataJson;
    } //getJsonForMydata

    public JsonObjectBuilder json(boolean showRelevance, boolean showEntityIds, boolean showApiUrls) {

        if (this.type == null) {
            return jsonObjectBuilder();
        }

        String displayName = null;

        String identifierLabel = null;
        String datasetCitation = null;
        String preferredUrl = null;
        String apiUrl = null;
        if (this.type.equals(SearchConstants.DATAVERSES)) {
            displayName = this.name;
            identifierLabel = "identifier";
            preferredUrl = getHtmlUrl();
        } else if (this.type.equals(SearchConstants.DATASETS)) {
            displayName = this.title;
            identifierLabel = "global_id";
            preferredUrl = getPersistentUrl();
            /**
             * @todo Should we show the name of the parent dataverse?
             */
        } else if (this.type.equals(SearchConstants.FILES)) {
            displayName = this.name;
            identifierLabel = "file_id";
            preferredUrl = getDownloadUrl();
            /**
             * @todo show more information for a file's parent, such as the
             * title of the dataset it belongs to.
             */
            datasetCitation = parent.get("citation");
        }

        //displayName = null; // testing NullSafeJsonBuilder
        // because we are using NullSafeJsonBuilder key/value pairs will be dropped if the value is null
        NullSafeJsonBuilder nullSafeJsonBuilder = jsonObjectBuilder()
                .add("name", displayName)
                .add("type", getDisplayType(getType()))
                .add("url", preferredUrl)
                .add("image_url", getImageUrl())
                //                .add("persistent_url", this.persistentUrl)
                //                .add("download_url", this.downloadUrl)
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
                 * @todo Expose MIME Type:
                 * https://github.com/IQSS/dataverse/issues/1595
                 */
                .add("file_type", this.filetype)
                .add("file_content_type", this.fileContentType)
                .add("size_in_bytes", getFileSizeInBytes())
                /**
                 * "md5" was the only possible value so it's hard-coded here but
                 * we might want to deprecate it someday since we now put the
                 * MD5 or SHA-1 in "checksum".
                 */
                .add("md5", getFileMd5())
                .add("checksum", JsonPrinter.getChecksumTypeAndValue(getFileChecksumType(), getFileChecksumValue()))
                .add("unf", getUnf())
                .add("dataset_citation", datasetCitation)
                .add("deaccession_reason", this.deaccessionReason)
                .add("citationHtml", this.citationHtml)
                .add("citation", this.citation);
        // Now that nullSafeJsonBuilder has been instatiated, check for null before adding to it!
        if (showRelevance) {
            nullSafeJsonBuilder.add("matches", getRelevance());
            nullSafeJsonBuilder.add("score", getScore());
        }
        if (showEntityIds) {
            if (this.entityId != null) {
                nullSafeJsonBuilder.add("entity_id", this.entityId);
            }
        }
        if (showApiUrls) {
            /**
             * @todo We should probably have a metadata_url or api_url concept
             * enabled by default, not hidden behind an undocumented boolean.
             * For datasets, this would be http://example.com/api/datasets/10 or
             * whatever (to get more detailed JSON), but right now this requires
             * an API token. Discuss at
             * https://docs.google.com/document/d/1d8sT2GLSavgiAuMTVX8KzTCX0lROEET1edhvHHRDZOs/edit?usp=sharing";
             */
            if (getApiUrl() != null) {
                nullSafeJsonBuilder.add("api_url", getApiUrl());
            }
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
            datePublished = releaseOrCreateDate == null ? null : Util.getDateTimeFormat().format(releaseOrCreateDate);
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

    public DvObject getEntity() {
        return entity;
    }

    public void setEntity(DvObject entity) {
        this.entity = entity;
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

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getPersistentUrl() {
        return persistentUrl;
    }

    public void setPersistentUrl(String persistentUrl) {
        this.persistentUrl = persistentUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isDisplayImage() {
        return displayImage;
    }

    public void setDisplayImage(boolean displayImage) {
        this.displayImage = displayImage;
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

    public String getDeaccessionReason() {
        return deaccessionReason;
    }

    public void setDeaccessionReason(String deaccessionReason) {
        this.deaccessionReason = deaccessionReason;
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

    public List<String> getFileCategories() {
        return fileCategories;
    }
    
    public void setFileCategories(List<String> fileCategories) {
        this.fileCategories = fileCategories;
    }
    
    public List<String> getTabularDataTags() {
        return tabularDataTags;
    }
    
    public void setTabularDataTags(List<String> tabularDataTags) {
        this.tabularDataTags = tabularDataTags;
    }
    
    public Map<String, String> getParent() {
        return parent;
    }

    public Long getParentIdAsLong() {

        if (this.getParent() == null) {
            return null;
        }
        if (!this.getParent().containsKey("id")) {
            return null;
        }

        String parentIdString = getParent().get("id");
        if (parentIdString == null) {
            return null;
        }

        try {
            return Long.parseLong(parentIdString);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void setParent(Map<String, String> parent) {
        this.parent = parent;
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

    public String getCitationHtml() {
        return citationHtml;
    }

    public void setCitationHtml(String citationHtml) {
        this.citationHtml = citationHtml;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public String getUnf() {
        return unf;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public Long getFileSizeInBytes() {
        return fileSizeInBytes;
    }

    public void setFileSizeInBytes(Long fileSizeInBytes) {
        this.fileSizeInBytes = fileSizeInBytes;
    }

    public String getFileMd5() {
        if (DataFile.ChecksumType.MD5.equals(getFileChecksumType())) {
            return fileMd5;
        } else {
            return null;
        }
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public DataFile.ChecksumType getFileChecksumType() {
        return fileChecksumType;
    }

    public void setFileChecksumType(DataFile.ChecksumType fileChecksumType) {
        this.fileChecksumType = fileChecksumType;
    }

    public String getFileChecksumValue() {
        return fileChecksumValue;
    }

    public void setFileChecksumValue(String fileChecksumValue) {
        this.fileChecksumValue = fileChecksumValue;
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

    public String getVersionNumberFriendly() {
        return versionNumberFriendly;
    }

    public void setVersionNumberFriendly(String versionNumberFriendly) {
        this.versionNumberFriendly = versionNumberFriendly;
    }

    public String getDatasetUrl() {
        String failSafeUrl = "/dataset.xhtml?id=" + entityId + "&versionId=" + datasetVersionId;
        if (identifier != null) {
            /**
             * Unfortunately, colons in the globalId (doi:10...) are converted
             * to %3A (doi%3A10...). To prevent this we switched many JSF tags
             * to a plain "a" tag with an href as suggested at
             * http://stackoverflow.com/questions/24733959/houtputlink-value-escaped
             */
            String badString = "null";
            if (!identifier.contains(badString)) {
                if (entity != null && entity instanceof Dataset) {
                    if (this.isHarvested()) {
                        String remoteArchiveUrl = ((Dataset) entity).getRemoteArchiveURL();
                        if (remoteArchiveUrl != null) {
                            return remoteArchiveUrl;
                        }
                        return null;
                    }
                }
                if (isDraftState()) {
                    return "/dataset.xhtml?persistentId=" + identifier + "&version=DRAFT";
                }
                return "/dataset.xhtml?persistentId=" + identifier;
            } else {
                logger.info("Dataset identifier/globalId contains \"" + badString + "\" perhaps due to https://github.com/IQSS/dataverse/issues/1147 . Fix data in database and reindex. Returning failsafe URL: " + failSafeUrl);
                return failSafeUrl;
            }
        } else {
            logger.info("Dataset identifier/globalId was null. Returning failsafe URL: " + failSafeUrl);
            return failSafeUrl;
        }
    }

    public String getFileParentIdentifier() {
        if (entity == null) {
            return null;
        }
        if (entity instanceof DataFile) {
            return parent.get(PARENT_IDENTIFIER);   // Dataset globalID
        }

        return null;
        //if (entity)
    }

    public String getFileUrl() {
        // Nothing special needs to be done for harvested file URLs: 
        // simply directing these to the local dataset.xhtml for this dataset
        // will take care of it - because DatasetPage will issue a redirect 
        // to the remote archive URL. 
        // This is true AS OF 4.2.4, FEB. 2016! - We'll probably want to make
        // .getRemoteArchiveURL() methods, both in DataFile and Dataset objects, 
        // work again at some point in the future. 
        /*
        if (entity != null && entity instanceof DataFile && this.isHarvested()) {
            String remoteArchiveUrl = ((DataFile) entity).getRemoteArchiveURL();
            if (remoteArchiveUrl != null) {
                return remoteArchiveUrl;
            }
            return null;
        }*/
        
        return "/file.xhtml?fileId=" + entity.getId() + "&datasetVersionId=" + datasetVersionId;
        
        /*
        if (parentDatasetGlobalId != null) {
            return "/dataset.xhtml?persistentId=" + parentDatasetGlobalId;
        } else {
            return "/dataset.xhtml?id=" + parent.get(SearchFields.ID) + "&versionId=" + datasetVersionId;
        }*/
    }
    
    public String getFileDatasetUrl() {
        // See the comment in the getFileUrl() method above. -- L.A. 4.2.4
        /*
        if (entity != null && entity instanceof DataFile && this.isHarvested()) {
            String remoteArchiveUrl = ((DataFile) entity).getRemoteArchiveURL();
            if (remoteArchiveUrl != null) {
                return remoteArchiveUrl;
            }
            return null;
        }*/
               
        String parentDatasetGlobalId = parent.get(PARENT_IDENTIFIER);        

        if (parentDatasetGlobalId != null) {
            return "/dataset.xhtml?persistentId=" + parentDatasetGlobalId;
        } else {
            return "/dataset.xhtml?id=" + parent.get(SearchFields.ID) + "&versionId=" + datasetVersionId;
        }
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

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
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

    /*
    public JsonArrayBuilder getUserRolesAsJson() {

        JsonArrayBuilder jsonRoleStrings = Json.createArrayBuilder();
        for (String role : this.getUserRole()) {
            jsonRoleStrings.add(role);
        }
        return jsonRoleStrings;
    }*/
    public List<String> getUserRole() {
        return userRole;
    }

    public void setUserRole(List<String> userRole) {
        this.userRole = userRole;
    }


}
