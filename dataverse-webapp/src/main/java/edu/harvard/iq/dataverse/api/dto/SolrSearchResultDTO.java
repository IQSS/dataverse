package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import edu.harvard.iq.dataverse.search.response.Highlight;
import edu.harvard.iq.dataverse.search.response.SearchParentInfo;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SolrSearchResultDTO {
    private String name;
    private String type;
    private String url;

    @JsonProperty("image_url")
    private String imageUrl;

    private String identifier;

    @JsonProperty("global_id")
    private String globalId;

    @JsonProperty("file_id")
    private String fileId;

    private String description;

    @JsonProperty("published_at")
    private String publishedAt;

    @JsonProperty("file_type")
    private String fileType;

    @JsonProperty("file_content_type")
    private String fileContentType;

    @JsonProperty("size_in_bytes")
    private Long sizeInBytes;

    private String md5;
    private ChecksumDTO checksum;
    private String unf;

    @JsonProperty("file_persistent_id")
    private String filePersistentId;

    @JsonProperty("dataset_name")
    private String datasetName;

    @JsonProperty("dataset_id")
    private String datasetId;

    @JsonProperty("dataset_persistent_id")
    private String datasetPersistentId;

    @JsonProperty("dataset_citation")
    private String datasetCitation;

    @JsonProperty("deaccession_reason")
    private String deaccessionReason;

    @JsonProperty("citation_html")
    private String citationHtml;

    @JsonProperty("identifier_of_dataverse")
    private String identifierOfDataverse;

    @JsonProperty("name_of_dataverse")
    private String nameOfDataverse;

    private String citation;
    private List<Map<String, Object>> matches;
    private Float score;

    @JsonProperty("entity_id")
    private Long entityId;

    @JsonProperty("api_url")
    private String apiUrl;

    private List<String> authors;

    @JsonProperty("publication_statuses")
    private List<String> publicationStatuses;

    @JsonProperty("is_draft_state")
    private Boolean isDraftState;

    @JsonProperty("is_in_review_state")
    private Boolean isInReviewState;

    @JsonProperty("is_unpublished_state")
    private Boolean isUnpublishedState;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("is_deaccessioned")
    private Boolean isDeaccessioned;

    @JsonProperty("date_to_display_on_card")
    private String dateToDisplayOnCard;

    @JsonProperty("deaccesioned_is_only_pubstatus")
    private Boolean deaccesionedIsOnlyPubstatus;

    private String parentIdentifier;
    private String parentId;
    private String parentName;

    @JsonProperty("parent_alias")
    private String parentAlias;

    @JsonProperty("user_roles")
    private List<String> userRoles;

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getGlobalId() {
        return globalId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getDescription() {
        return description;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getMd5() {
        return md5;
    }

    public ChecksumDTO getChecksum() {
        return checksum;
    }

    public String getUnf() {
        return unf;
    }

    public String getFilePersistentId() {
        return filePersistentId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public String getDatasetPersistentId() {
        return datasetPersistentId;
    }

    public String getDatasetCitation() {
        return datasetCitation;
    }

    public String getDeaccessionReason() {
        return deaccessionReason;
    }

    public String getCitationHtml() {
        return citationHtml;
    }

    public String getIdentifierOfDataverse() {
        return identifierOfDataverse;
    }

    public String getNameOfDataverse() {
        return nameOfDataverse;
    }

    public String getCitation() {
        return citation;
    }

    public List<Map<String, Object>> getMatches() {
        return matches;
    }

    public Float getScore() {
        return score;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getPublicationStatuses() {
        return publicationStatuses;
    }

    public Boolean getDraftState() {
        return isDraftState;
    }

    public Boolean getInReviewState() {
        return isInReviewState;
    }

    public Boolean getUnpublishedState() {
        return isUnpublishedState;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public Boolean getDeaccessioned() {
        return isDeaccessioned;
    }

    public String getDateToDisplayOnCard() {
        return dateToDisplayOnCard;
    }

    public Boolean getDeaccesionedIsOnlyPubstatus() {
        return deaccesionedIsOnlyPubstatus;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public String getParentId() {
        return parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getParentAlias() {
        return parentAlias;
    }

    public List<String> getUserRoles() {
        return userRoles;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public void setSizeInBytes(Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setChecksum(ChecksumDTO checksum) {
        this.checksum = checksum;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public void setFilePersistentId(String filePersistentId) {
        this.filePersistentId = filePersistentId;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public void setDatasetPersistentId(String datasetPersistentId) {
        this.datasetPersistentId = datasetPersistentId;
    }

    public void setDatasetCitation(String datasetCitation) {
        this.datasetCitation = datasetCitation;
    }

    public void setDeaccessionReason(String deaccessionReason) {
        this.deaccessionReason = deaccessionReason;
    }

    public void setCitationHtml(String citationHtml) {
        this.citationHtml = citationHtml;
    }

    public void setIdentifierOfDataverse(String identifierOfDataverse) {
        this.identifierOfDataverse = identifierOfDataverse;
    }

    public void setNameOfDataverse(String nameOfDataverse) {
        this.nameOfDataverse = nameOfDataverse;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public void setMatches(List<Map<String, Object>> matches) {
        this.matches = matches;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setPublicationStatuses(List<String> publicationStatuses) {
        this.publicationStatuses = publicationStatuses;
    }

    public void setDraftState(Boolean draftState) {
        isDraftState = draftState;
    }

    public void setInReviewState(Boolean inReviewState) {
        isInReviewState = inReviewState;
    }

    public void setUnpublishedState(Boolean unpublishedState) {
        isUnpublishedState = unpublishedState;
    }

    public void setPublished(Boolean published) {
        isPublished = published;
    }

    public void setDeaccessioned(Boolean deaccessioned) {
        isDeaccessioned = deaccessioned;
    }

    public void setDateToDisplayOnCard(String dateToDisplayOnCard) {
        this.dateToDisplayOnCard = dateToDisplayOnCard;
    }

    public void setDeaccesionedIsOnlyPubstatus(Boolean deaccesionedIsOnlyPubstatus) {
        this.deaccesionedIsOnlyPubstatus = deaccesionedIsOnlyPubstatus;
    }

    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setParentAlias(String parentAlias) {
        this.parentAlias = parentAlias;
    }

    public void setUserRoles(List<String> userRoles) {
        this.userRoles = userRoles;
    }

    // -------------------- INNER CLASSES --------------------

    public static class ChecksumDTO {
        private String type;
        private String value;

        // -------------------- GETTERS --------------------

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        // -------------------- SETTERS --------------------

        public void setType(String type) {
            this.type = type;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Creator {

        private DataverseDao dataverseDao;
        private RoleTagRetriever roleTagRetriever;

        // -------------------- CONSTRUCTORS --------------------

        public Creator(DataverseDao dataverseDao, RoleTagRetriever roleTagRetriever) {
            this.dataverseDao = dataverseDao;
            this.roleTagRetriever = roleTagRetriever;
        }

        // -------------------- LOGIC --------------------

        public List<SolrSearchResultDTO> createResults(SolrQueryResponse response) {
            List<SolrSearchResultDTO> results = response.getSolrSearchResults().stream()
                    .map(this::createResult)
                    .collect(Collectors.toList());
            List<Long> nonFileIds = results.stream()
                    .filter(r -> !SearchObjectType.FILES.getSolrValue().equals(r.getType()))
                    .map(SolrSearchResultDTO::getEntityId)
                    .collect(Collectors.toList());
            Map<Long, String> nonFileIdToParentAlias = dataverseDao.getParentAliasesForIds(nonFileIds).stream()
                    .collect(Collectors.toMap(i -> (Long) i[0], i -> (String) i[1], (next, prev) -> next));
            List<Long> allIds = results.stream()
                    .map(SolrSearchResultDTO::getEntityId)
                    .collect(Collectors.toList());
            Map<Long, List<String>> idToRoles = roleTagRetriever.getRolesForCard(allIds);
            for (SolrSearchResultDTO result : results) {
                Long id = result.getEntityId();
                result.setParentAlias(nonFileIdToParentAlias.get(id));
                result.setUserRoles(idToRoles.get(id));
            }
            return results;
        }

        // -------------------- PRIVATE --------------------

        private SolrSearchResultDTO createResult(SolrSearchResult result) {
            SolrSearchResultDTO created = new SolrSearchResultDTO();
            SearchParentInfo parent = result.getParent();
            switch (result.getType()) {
                case DATAVERSES:
                    created.setName(result.getName());
                    created.setUrl(result.getHtmlUrl());
                    created.setIdentifier(result.getIdentifier());
                    break;
                case DATASETS:
                    created.setName(result.getTitle());
                    created.setUrl(result.getPersistentUrl());
                    created.setGlobalId(result.getIdentifier());
                    break;
                case FILES:
                    created.setName(result.getName());
                    created.setUrl(result.getDownloadUrl());
                    created.setFileId(result.getIdentifier());
                    created.setDatasetName(parent.getName());
                    created.setDatasetId(parent.getId());
                    created.setDatasetPersistentId(parent.getParentIdentifier());
                    created.setDatasetCitation(parent.getCitation());
                    break;
            }
            created.setType(result.getDisplayType());
            created.setImageUrl(result.getImageUrl());
            created.setDescription(result.getDescriptionNoSnippet());
            created.setPublishedAt(result.getDateTimePublished());
            created.setFileType(result.getFiletype());
            created.setFileContentType(result.getFileContentType());
            created.setSizeInBytes(result.getFileSizeInBytes());
            created.setMd5(result.getFileMd5());
            created.setChecksum(createChecksum(result));
            created.setUnf(result.getUnf());
            created.setFilePersistentId(result.getFilePersistentId());
            created.setDeaccessionReason(result.getDeaccessionReason());
            created.setCitationHtml(result.getCitationHtml());
            created.setIdentifierOfDataverse(result.getIdentifierOfDataverse());
            created.setNameOfDataverse(result.getNameOfDataverse());
            created.setCitation(result.getCitation());
            created.setMatches(createMatches(result));
            created.setScore(result.getScore());
            created.setEntityId(result.getEntityId());
            created.setApiUrl(result.getApiUrl());
            created.setAuthors(result.getDatasetAuthors());

            created.setPublicationStatuses(result.getPublicationStatuses().stream()
                    .map(SearchPublicationStatus::getSolrValue)
                    .collect(Collectors.toList()));
            created.setDraftState(result.isDraftState());
            created.setInReviewState(result.isInReviewState());
            created.setUnpublishedState(result.isUnpublishedState());
            created.setPublished(result.isPublishedState());
            created.setDeaccessioned(result.isDeaccessionedState());
            created.setDateToDisplayOnCard(new SimpleDateFormat("MMM d, yyyy", Locale.US)
                    .format(result.getReleaseOrCreateDate()));
            if (result.isDeaccessionedState() && result.getPublicationStatuses().size() == 1) {
                created.setDeaccesionedIsOnlyPubstatus(true);
            }
            if (parent != null && !parent.isInfoMissing()) {
                if (result.getType() == SearchObjectType.FILES) {
                    created.setParentIdentifier(parent.getParentIdentifier());
                } else {
                    created.setParentId(parent.getId());
                }
                created.setParentName(parent.getName());
            }
            return created;
        }

        private List<Map<String, Object>> createMatches(SolrSearchResult result) {
            List<Map<String, Object>> matches = new ArrayList<>();
            Map<SolrField, Highlight> highlights = result.getHighlightsMap();
            for (Map.Entry<SolrField, Highlight> entry : highlights.entrySet()) {
                Map<String, Object> matchedField = new HashMap<>();
                String fieldName = entry.getKey().getNameSearchable();
                List<String> fieldDetails = new ArrayList<>(entry.getValue().getSnippets());
                Map<String, List<String>> snippets = new HashMap<>();
                snippets.put("snippets", fieldDetails);
                matchedField.put(fieldName, snippets);
                matches.add(matchedField);
            }
            return matches;
        }

        private ChecksumDTO createChecksum(SolrSearchResult result) {
            if (result.getFileChecksumType() == null) {
                return null;
            }
            ChecksumDTO checksum = new ChecksumDTO();
            checksum.setType(result.getFileChecksumType().toString());
            checksum.setValue(result.getFileChecksumValue());
            return checksum;
        }
    }
}
