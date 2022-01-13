package edu.harvard.iq.dataverse.search.response;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SolrSearchResult {

    private static final Logger logger = Logger.getLogger(SolrSearchResult.class.getCanonicalName());

    private String id;
    private Long entityId;
    private DvObject entity;
    private String identifier;
    private SearchObjectType type;
    private String htmlUrl;
    private String persistentUrl;
    private String downloadUrl;
    private String apiUrl;
    /**
     * This is called "imageUrl" because it used to really be a URL. While
     * performance improvements were being made in the 4.2 timeframe, we started
     * putting base64 representations of images in this String instead, which
     * broke the Search API and probably things built on top of it such as
     * MyData. See "`image_url` from Search API results no longer yields a
     * downloadable image" at https://github.com/IQSS/dataverse/issues/3616
     */
    private String imageUrl;
    private String name;
    private String nameSort;
    private Date releaseOrCreateDate;
    private List<SearchPublicationStatus> publicationStatuses = new ArrayList<>();

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
    private SearchParentInfo parent;
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
    private String fileAccess;
    /**
     * fileMD5 is here for legacy and backward-compatibility reasons. It might be deprecated some day in favor of "fileChecksumType" and "fileChecksumValue"
     */
    private String fileMd5;
    private DataFile.ChecksumType fileChecksumType;
    private String fileChecksumValue;
    private String dataverseAlias;
    private String dataverseParentAlias;
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
    private Long datasetVersionId;
    //Determine if the search result is owned by any of the dvs in the tree of the DV displayed
    private boolean isInTree;
    private float score;
    private boolean harvested = false;
    private List<String> fileCategories = null;
    private List<String> tabularDataTags = null;

    private String identifierOfDataverse = null;
    private String nameOfDataverse = null;

    private String filePersistentId = null;
    private List<String> matchedFields;

    // -------------------- CONSTRUCTORS --------------------

    public SolrSearchResult() { }

    // -------------------- GETTERS --------------------

    public boolean isIsInTree() {
        return isInTree;
    }

    public boolean isHarvested() {
        return harvested;
    }

    public boolean isPublishedState() {
        return publishedState;
    }

    public boolean isUnpublishedState() {
        return unpublishedState;
    }

    public List<SearchPublicationStatus> getPublicationStatuses() {
        return publicationStatuses;
    }

    public boolean isDraftState() {
        return draftState;
    }

    public boolean isInReviewState() {
        return inReviewState;
    }

    public boolean isDeaccessionedState() {
        return deaccessionedState;
    }

    public String getFilePersistentId() {
        return filePersistentId;
    }

    public String getIdentifierOfDataverse() {
        return identifierOfDataverse;
    }

    public String getNameOfDataverse() {
        return nameOfDataverse;
    }

    public Map<SolrField, Highlight> getHighlightsMap() {
        return highlightsMap;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public String getId() {
        return id;
    }

    public Long getEntityId() {
        return entityId;
    }

    public DvObject getEntity() {
        return entity;
    }

    public String getIdentifier() {
        return identifier;
    }

    public SearchObjectType getType() {
        return type;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getPersistentUrl() {
        return persistentUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescriptionNoSnippet() {
        return descriptionNoSnippet;
    }

    public List<String> getDatasetAuthors() {
        return datasetAuthors;
    }

    public String getDeaccessionReason() {
        return deaccessionReason;
    }

    public List<String> getFileCategories() {
        return fileCategories;
    }

    public List<String> getTabularDataTags() {
        return tabularDataTags;
    }

    public SearchParentInfo getParent() {
        return parent;
    }

    public String getDataverseAffiliation() {
        return dataverseAffiliation;
    }

    public String getCitation() {
        return citation;
    }

    public String getCitationHtml() {
        return citationHtml;
    }

    public String getFiletype() {
        return filetype;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public String getUnf() {
        return unf;
    }

    public Long getFileSizeInBytes() {
        return fileSizeInBytes;
    }

    public DataFile.ChecksumType getFileChecksumType() {
        return fileChecksumType;
    }

    public String getFileChecksumValue() {
        return fileChecksumValue;
    }

    public String getNameSort() {
        return nameSort;
    }

    public Date getReleaseOrCreateDate() {
        return releaseOrCreateDate;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public String getDataverseAlias() {
        return dataverseAlias;
    }

    public float getScore() {
        return score;
    }

    public String getDataverseParentAlias() {
        return dataverseParentAlias;
    }

    public String getFileAccess() {
        return fileAccess;
    }

    // -------------------- SETTERS --------------------

    public void setIsInTree(boolean isInTree) {
        this.isInTree = isInTree;
    }

    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }

    public void setPublishedState(boolean publishedState) {
        this.publishedState = publishedState;
    }

    public void setUnpublishedState(boolean unpublishedState) {
        this.unpublishedState = unpublishedState;
    }

    public void setDraftState(boolean draftState) {
        this.draftState = draftState;
    }

    public void setInReviewState(boolean inReviewState) {
        this.inReviewState = inReviewState;
    }

    public void setDeaccessionedState(boolean deaccessionedState) {
        this.deaccessionedState = deaccessionedState;
    }

    public void setHighlightsAsMap(Map<String, Highlight> highlightsAsMap) {
        this.highlightsAsMap = highlightsAsMap;
    }

    public void setHighlightsMap(Map<SolrField, Highlight> highlightsMap) {
        this.highlightsMap = highlightsMap;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setEntity(DvObject entity) {
        this.entity = entity;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setType(SearchObjectType type) {
        this.type = type;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setPersistentUrl(String persistentUrl) {
        this.persistentUrl = persistentUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescriptionNoSnippet(String descriptionNoSnippet) {
        this.descriptionNoSnippet = descriptionNoSnippet;
    }

    public void setDatasetAuthors(List<String> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }

    public void setDeaccessionReason(String deaccessionReason) {
        this.deaccessionReason = deaccessionReason;
    }

    public void setHighlightsAsList(List<Highlight> highlightsAsList) {
        this.highlightsAsList = highlightsAsList;
    }

    public void setFileCategories(List<String> fileCategories) {
        this.fileCategories = fileCategories;
    }

    public void setTabularDataTags(List<String> tabularDataTags) {
        this.tabularDataTags = tabularDataTags;
    }

    public void setParent(SearchParentInfo parent) {
        this.parent = parent;
    }

    public void setDataverseAffiliation(String dataverseAffiliation) {
        this.dataverseAffiliation = dataverseAffiliation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public void setCitationHtml(String citationHtml) {
        this.citationHtml = citationHtml;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public void setFileSizeInBytes(Long fileSizeInBytes) {
        this.fileSizeInBytes = fileSizeInBytes;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public void setFileChecksumType(DataFile.ChecksumType fileChecksumType) {
        this.fileChecksumType = fileChecksumType;
    }

    public void setFileChecksumValue(String fileChecksumValue) {
        this.fileChecksumValue = fileChecksumValue;
    }

    public void setNameSort(String nameSort) {
        this.nameSort = nameSort;
    }

    public void setReleaseOrCreateDate(Date releaseOrCreateDate) {
        this.releaseOrCreateDate = releaseOrCreateDate;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    public void setFilePersistentId(String pid) {
        filePersistentId = pid;
    }

    public void setDataverseAlias(String dataverseAlias) {
        this.dataverseAlias = dataverseAlias;
    }

    public void setDataverseParentAlias(String dataverseParentAlias) {
        this.dataverseParentAlias = dataverseParentAlias;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setIdentifierOfDataverse(String id) {
        this.identifierOfDataverse = id;
    }

    public void setNameOfDataverse(String id) {
        this.nameOfDataverse = id;
    }

    public void setFileAccess(String fileAccess) {
        this.fileAccess = fileAccess;
    }

    // -------------------- LOGIC --------------------

    public void setPublicationStatuses(List<SearchPublicationStatus> statuses) {
        if (statuses == null) {
            publicationStatuses = new ArrayList<>();
            return;
        }

        publicationStatuses = statuses;

        for (SearchPublicationStatus status : publicationStatuses) {
            if (status == SearchPublicationStatus.UNPUBLISHED) {
                setUnpublishedState(true);
            } else if (status == SearchPublicationStatus.PUBLISHED) {
                setPublishedState(true);
            } else if (status == SearchPublicationStatus.DRAFT) {
                setDraftState(true);
            } else if (status == SearchPublicationStatus.IN_REVIEW) {
                setInReviewState(true);
            } else if (status == SearchPublicationStatus.DEACCESSIONED) {
                setDeaccessionedState(true);
            }
        }
    }

    public String getNameHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.NAME);
        return highlight != null ? highlight.getSnippets().get(0) : null;
    }

    public String getDataverseAffiliationHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.AFFILIATION);
        return highlight != null ? highlight.getSnippets().get(0) : null;
    }

    public String getFileTypeHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.FILE_TYPE_FRIENDLY);
        return highlight != null ? highlight.getSnippets().get(0) : null;
    }

    public String getTitleHighlightSnippet() {
        /**
         * @todo: don't hard-code title, look it up properly... or start
         * indexing titles as names:
         * https://redmine.hmdc.harvard.edu/issues/3798#note-2
         */
        Highlight highlight = highlightsAsMap.get("title");
        return highlight != null ? highlight.getSnippets().get(0) : null;
    }

    public List<String> getDescriptionSnippets() {
        Highlight highlight = highlightsAsMap.get(SearchFields.DESCRIPTION);
        if (type == SearchObjectType.DATASETS) {
            highlight = highlightsAsMap.get(SearchFields.DATASET_DESCRIPTION);
        }
        return highlight != null ? highlight.getSnippets() : new ArrayList<>();
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

    public Long getParentIdAsLong() {
        if (getParent() == null || getParent().getId() == null) {
            return null;
        }

        String parentIdString = getParent().getId();

        try {
            return Long.parseLong(parentIdString);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String getFileMd5() {
        return DataFile.ChecksumType.MD5.equals(getFileChecksumType()) ? fileMd5 : null;
    }

    public String getDatasetUrl() {
        if (identifier == null) {
            String failSafeUrl = "/dataset.xhtml?id=" + entityId + "&versionId=" + datasetVersionId;
            logger.info("Dataset identifier/globalId was null. Returning failsafe URL: " + failSafeUrl);
            return failSafeUrl;
        }
        return isDraftState()
                ? "/dataset.xhtml?persistentId=" + identifier + "&version=DRAFT"
                : "/dataset.xhtml?persistentId=" + identifier;
    }
    public String getFileUrl() {
        if (identifier == null) {
            return "/file.xhtml?fileId=" + entityId + "&datasetVersionId=" + datasetVersionId;
        }
        return isDraftState()
                ? "/file.xhtml?persistentId=" + identifier + "&version=DRAFT"
                : "/file.xhtml?persistentId=" + identifier;
    }

    public String getFileDatasetUrl() {
        String parentDatasetGlobalId = parent.getParentIdentifier();

        return parentDatasetGlobalId != null
                ? "/dataset.xhtml?persistentId=" + parentDatasetGlobalId
                : "/dataset.xhtml?id=" + parent.getId() + "&versionId=" + datasetVersionId;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return String.format("%s:%s:%d", id, name != null ? name : title, entityId);
    }
}
