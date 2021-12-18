package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.PublicationStatusCounts;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class MyDataDTO {
    private PagerDTO pagination;

    private List<SolrSearchResultDTO> items;

    @JsonProperty("total_count")
    private Long totalCount;

    private Long start;

    @JsonProperty("search_term")
    private String searchTerm;

    @JsonProperty("dvobject_counts")
    private DvObjectCountsDTO dvObjectCounts;

    @JsonProperty("pubstatus_counts")
    private PubStatusCountsDTO pubStatusCounts;

    @JsonProperty("selected_filters")
    private SelectedFiltersDTO selectedFilters;

    @JsonProperty("other_user")
    private String otherUser;

    // -------------------- GETTERS --------------------

    public PagerDTO getPagination() {
        return pagination;
    }

    public List<SolrSearchResultDTO> getItems() {
        return items;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Long getStart() {
        return start;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public DvObjectCountsDTO getDvObjectCounts() {
        return dvObjectCounts;
    }

    public PubStatusCountsDTO getPubStatusCounts() {
        return pubStatusCounts;
    }

    public SelectedFiltersDTO getSelectedFilters() {
        return selectedFilters;
    }

    public String getOtherUser() {
        return otherUser;
    }

    // -------------------- SETTERS --------------------

    public void setPagination(PagerDTO pagination) {
        this.pagination = pagination;
    }

    public void setItems(List<SolrSearchResultDTO> items) {
        this.items = items;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public void setDvObjectCounts(DvObjectCountsDTO dvObjectCounts) {
        this.dvObjectCounts = dvObjectCounts;
    }

    public void setPubStatusCounts(PubStatusCountsDTO pubStatusCounts) {
        this.pubStatusCounts = pubStatusCounts;
    }

    public void setSelectedFilters(SelectedFiltersDTO selectedFilters) {
        this.selectedFilters = selectedFilters;
    }

    public void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
    }

    // -------------------- INNER CLASSES --------------------

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class DvObjectCountsDTO {
        @JsonProperty("dataverses_count")
        private Long dataversesCount;

        @JsonProperty("datasets_count")
        private Long datasetsCount;

        @JsonProperty("files_count")
        private Long filesCount;

        // -------------------- GETTERS --------------------

        public Long getDataversesCount() {
            return dataversesCount;
        }

        public Long getDatasetsCount() {
            return datasetsCount;
        }

        public Long getFilesCount() {
            return filesCount;
        }

        // -------------------- SETTERS --------------------

        public void setDataversesCount(Long dataversesCount) {
            this.dataversesCount = dataversesCount;
        }

        public void setDatasetsCount(Long datasetsCount) {
            this.datasetsCount = datasetsCount;
        }

        public void setFilesCount(Long filesCount) {
            this.filesCount = filesCount;
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class PubStatusCountsDTO {
        @JsonProperty("in_review_count")
        private Long inReviewCount;

        @JsonProperty("unpublished_count")
        private Long unpublishedCount;

        @JsonProperty("published_count")
        private Long publishedCount;

        @JsonProperty("draft_count")
        private Long draftCount;

        @JsonProperty("deaccessioned_count")
        private Long deaccessionedCount;

        // -------------------- GETTERS --------------------

        public Long getInReviewCount() {
            return inReviewCount;
        }

        public Long getUnpublishedCount() {
            return unpublishedCount;
        }

        public Long getPublishedCount() {
            return publishedCount;
        }

        public Long getDraftCount() {
            return draftCount;
        }

        public Long getDeaccessionedCount() {
            return deaccessionedCount;
        }

        // -------------------- SETTERS --------------------

        public void setInReviewCount(Long inReviewCount) {
            this.inReviewCount = inReviewCount;
        }

        public void setUnpublishedCount(Long unpublishedCount) {
            this.unpublishedCount = unpublishedCount;
        }

        public void setPublishedCount(Long publishedCount) {
            this.publishedCount = publishedCount;
        }

        public void setDraftCount(Long draftCount) {
            this.draftCount = draftCount;
        }

        public void setDeaccessionedCount(Long deaccessionedCount) {
            this.deaccessionedCount = deaccessionedCount;
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class SelectedFiltersDTO {
        @JsonProperty("publication_statuses")
        private List<String> publicationStatuses;

        @JsonProperty("role_names")
        private List<String> roleNames;

        // -------------------- GETTERS --------------------

        public List<String> getPublicationStatuses() {
            return publicationStatuses;
        }

        public List<String> getRoleNames() {
            return roleNames;
        }

        // -------------------- SETTERS --------------------

        public void setPublicationStatuses(List<String> publicationStatuses) {
            this.publicationStatuses = publicationStatuses;
        }

        public void setRoleNames(List<String> roleNames) {
            this.roleNames = roleNames;
        }
    }

    public static class Creator {

        private DataverseDao dataverseDao;
        private RoleTagRetriever roleTagRetriever;
        private DataverseRolePermissionHelper permissionHelper;

        // -------------------- CONSTRUCTORS --------------------

        public Creator(DataverseDao dataverseDao, RoleTagRetriever roleTagRetriever,
                       DataverseRolePermissionHelper permissionHelper) {
            this.dataverseDao = dataverseDao;
            this.roleTagRetriever = roleTagRetriever;
            this.permissionHelper = permissionHelper;
        }

        // -------------------- LOGIC --------------------

        public MyDataDTO create(SolrQueryResponse response, Pager pager, MyDataFilterParams filterParams) {
            MyDataDTO created = new MyDataDTO();
            created.setPagination(new PagerDTO.Converter().convert(pager));
            created.setItems(new SolrSearchResultDTO.Creator(dataverseDao, roleTagRetriever).createResults(response));
            created.setTotalCount(response.getNumResultsFound());
            created.setStart(response.getResultsStart());
            created.setSearchTerm(filterParams.getSearchTerm());
            created.setDvObjectCounts(createDvObjectCounts(response));
            created.setPubStatusCounts(createPubStatusCounts(response));
            created.setSelectedFilters(createSelectedFilters(filterParams));
            return created;
        }

        private SelectedFiltersDTO createSelectedFilters(MyDataFilterParams filterParams) {
            SelectedFiltersDTO selectedFilters = new SelectedFiltersDTO();
            selectedFilters.setPublicationStatuses(filterParams.getPublicationStatuses());
            selectedFilters.setRoleNames(filterParams.getRoleIds().stream()
                    .map(permissionHelper::getRoleName)
                    .collect(Collectors.toList()));
            return selectedFilters;
        }

        // -------------------- PRIVATE --------------------

        private DvObjectCountsDTO createDvObjectCounts(SolrQueryResponse response) {
            DvObjectCountsDTO dvObjectCounts = new DvObjectCountsDTO();
            DvObjectCounts counts = response.getDvObjectCounts();
            dvObjectCounts.setDataversesCount(counts.getDataversesCount());
            dvObjectCounts.setDatasetsCount(counts.getDatasetsCount());
            dvObjectCounts.setFilesCount(counts.getDatafilesCount());
            return dvObjectCounts;
        }

        private PubStatusCountsDTO createPubStatusCounts(SolrQueryResponse response) {
            PubStatusCountsDTO pubStatusCounts = new PubStatusCountsDTO();
            PublicationStatusCounts counts = response.getPublicationStatusCounts();
            pubStatusCounts.setDeaccessionedCount(counts.getDeaccessionedCount());
            pubStatusCounts.setDraftCount(counts.getDraftCount());
            pubStatusCounts.setInReviewCount(counts.getInReviewCount());
            pubStatusCounts.setPublishedCount(counts.getPublishedCount());
            pubStatusCounts.setUnpublishedCount(counts.getUnpublishedCount());
            return pubStatusCounts;
        }
    }
}
