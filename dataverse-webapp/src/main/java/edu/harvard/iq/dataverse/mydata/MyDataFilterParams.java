package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rmp553
 */
public class MyDataFilterParams {

    public static final List<String> defaultDvObjectTypes = Initializer.initializeDefaultDvObjectTypes();

    public static final List<String> defaultPublishedStates = Initializer.initializeDefaultPublishedStates();
    public static final List<String> allPublishedStates = defaultPublishedStates;

    public static final Map<String, SearchObjectType> sqlToSolrSearchMap = Initializer.initializeSqlToSolrSearchMap();

    // -----------------------------------
    // Filter parameters
    // -----------------------------------
    private DataverseRequest dataverseRequest;
    private AuthenticatedUser authenticatedUser;
    private String userIdentifier;
    private List<String> dvObjectTypes;
    private List<String> publicationStatuses;
    private List<Long> roleIds;

    public static final String defaultSearchTerm = "*:*";
    private String searchTerm;

    private boolean errorFound = false;
    private String errorMessage = null;

    // -------------------- CONSTRUCTORS --------------------

    public MyDataFilterParams(DataverseRequest dataverseRequest, List<String> dvObjectTypes, List<String> publicationStatuses,
                              List<Long> roleIds, String searchTerm) {
        if (dataverseRequest == null) {
            throw new NullPointerException("MyDataFilterParams constructor: dataverseRequest cannot be null ");
        }
        this.dataverseRequest = dataverseRequest;
        setAuthenticatedUserFromDataverseRequest(dataverseRequest);
        this.userIdentifier = authenticatedUser.getIdentifier();

        if (dvObjectTypes == null) {
            throw new NullPointerException("MyDataFilterParams constructor: dvObjectTypes cannot be null");
        }

        this.dvObjectTypes = dvObjectTypes;
        this.publicationStatuses = publicationStatuses == null ? MyDataFilterParams.defaultPublishedStates : publicationStatuses;

        this.roleIds = roleIds;
        this.searchTerm = StringUtils.isBlank(searchTerm) ? MyDataFilterParams.defaultSearchTerm : searchTerm;
        checkParams();
    }

    // -------------------- GETTERS --------------------

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public DataverseRequest getDataverseRequest() {
        return dataverseRequest;
    }

    public boolean hasError() {
        return errorFound;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public List<String> getPublicationStatuses() {
        return publicationStatuses;
    }

    // -------------------- LOGIC --------------------

    private void setAuthenticatedUserFromDataverseRequest(DataverseRequest dvRequest) {
        if (dvRequest == null) {
            throw new NullPointerException("MyDataFilterParams getAuthenticatedUserFromDataverseRequest: dvRequest cannot be null");
        }

        authenticatedUser = dvRequest.getAuthenticatedUser();

        if (authenticatedUser == null) {
            throw new NullPointerException("MyDataFilterParams getAuthenticatedUserFromDataverseRequest: Hold on! dvRequest must be associated with an AuthenticatedUser be null");
        }
    }

    private void checkParams() {

        if (StringUtils.isEmpty(userIdentifier)) {
            addError(BundleUtil.getStringFromBundle("mydataFragment.errorMessage.noUserSelected"));
            return;
        }

        if (roleIds == null || roleIds.isEmpty()) {
            addError(BundleUtil.getStringFromBundle("mydataFragment.errorMessage.noRoleSelected"));
            return;
        }

        if (dvObjectTypes == null || dvObjectTypes.isEmpty()) {
            addError(BundleUtil.getStringFromBundle("mydataFragment.errorMessage.noDvObjectsSelected"));
            return;
        }

        if (publicationStatuses == null || publicationStatuses.isEmpty()) {
            addError(String.format("%s %s.",
                BundleUtil.getStringFromBundle("mydataFragment.errorMessage.noPublicationStatusSelected"),
                StringUtils.join(MyDataFilterParams.defaultPublishedStates, ", ").replace("_", " ")));
            return;
        }

        for (String dtype : dvObjectTypes) {
            if (!DvObject.DTYPE_LIST.contains(dtype)) {
                addError(BundleUtil.getStringFromBundle("mydataFragment.errorMessage.unknownType.prefix")
                        + dtype + BundleUtil.getStringFromBundle("mydataFragment.errorMessage.unknownType.suffix"));
                return;
            }
        }
    }

    public void addError(String s) {
        errorFound = true;
        errorMessage = s;
    }

    public boolean areDataversesIncluded() {
        return dvObjectTypes.contains(DvObject.DATAVERSE_DTYPE_STRING);
    }

    public boolean areDatasetsIncluded() {
        return dvObjectTypes.contains(DvObject.DATASET_DTYPE_STRING);
    }

    public boolean areFilesIncluded() {
        return dvObjectTypes.contains(DvObject.DATAFILE_DTYPE_STRING);
    }

    public SearchForTypes getSolrFragmentForDvObjectType() {
        if (dvObjectTypes == null || dvObjectTypes.isEmpty()) {
            throw new IllegalStateException("Error encountered earlier.  Before calling this method, first check 'hasError()'");
        }

        return SearchForTypes.byTypes(dvObjectTypes.stream()
                .map(sqlToSolrSearchMap::get)
                .toArray(SearchObjectType[]::new));
    }

    public String getSolrFragmentForPublicationStatus() {
        if (publicationStatuses == null || publicationStatuses.isEmpty()) {
            throw new IllegalStateException("Error encountered earlier.  Before calling this method, first check 'hasError()'");
        }

        List<String> solrPublicationStatuses = publicationStatuses.stream()
                .map(s -> String.format("\"%s\"", s))
                .collect(Collectors.toList());

        String valStr = StringUtils.join(solrPublicationStatuses, " OR ");
        if (publicationStatuses.size() > 1) {
            valStr = "(" + valStr + ")";
        }

        return String.format("(%s:%s)", SearchFields.PUBLICATION_STATUS, valStr);
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        static List<String> initializeDefaultDvObjectTypes() {
            return Arrays.asList(DvObject.DATAVERSE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
        }

        static List<String> initializeDefaultPublishedStates() {
            return  Arrays.asList(SearchPublicationStatus.PUBLISHED.getSolrValue(),
                    SearchPublicationStatus.UNPUBLISHED.getSolrValue(),
                    SearchPublicationStatus.DRAFT.getSolrValue(),
                    SearchPublicationStatus.IN_REVIEW.getSolrValue(),
                    SearchPublicationStatus.DEACCESSIONED.getSolrValue());
        }

        static Map<String, SearchObjectType> initializeSqlToSolrSearchMap() {
            Map<String, SearchObjectType> sqlToSolrSearchMap = new HashMap<>();
            sqlToSolrSearchMap.put(DvObject.DATAVERSE_DTYPE_STRING, SearchObjectType.DATAVERSES);
            sqlToSolrSearchMap.put(DvObject.DATASET_DTYPE_STRING, SearchObjectType.DATASETS);
            sqlToSolrSearchMap.put(DvObject.DATAFILE_DTYPE_STRING, SearchObjectType.FILES);
            return sqlToSolrSearchMap;
        }
    }
}
