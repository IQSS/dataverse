package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static edu.harvard.iq.dataverse.search.index.SearchPermissions.ALWAYS_PUBLIC;
import static edu.harvard.iq.dataverse.search.index.SearchPermissions.NEVER_PUBLIC;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Determine permissions for solr use
 */
@Stateless
public class SolrPermissionsFinder {

    private static final Logger logger = Logger.getLogger(SolrPermissionsFinder.class.getCanonicalName());

    private RoleAssigneeServiceBean roleAssigneeService;
    private DataverseRoleServiceBean rolesSvc;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SolrPermissionsFinder() {
        // JEE requirement
    }

    @Inject
    public SolrPermissionsFinder(RoleAssigneeServiceBean roleAssigneeService, DataverseRoleServiceBean rolesSvc) {
        this.roleAssigneeService = roleAssigneeService;
        this.rolesSvc = rolesSvc;
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns {@link SolrPermissions} on which Solr will JOIN
     * to enforce permissions.
     */
    public SolrPermissions findDataversePerms(Dataverse dataverse) {
        Map<SolrPermissionType, List<String>> perms = findDvObjectPerms(dataverse,
                SolrPermissionType.SEARCH, SolrPermissionType.ADD_DATASET);
        return new SolrPermissions(new SearchPermissions(perms.get(SolrPermissionType.SEARCH),
                    hasBeenPublished(dataverse) ? ALWAYS_PUBLIC : NEVER_PUBLIC),
                    new SolrPermission(Permission.AddDataset, perms.get(SolrPermissionType.ADD_DATASET)));
    }

    public SolrPermissions findDatasetVersionPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            return new SolrPermissions(new SearchPermissions(perms, ALWAYS_PUBLIC),
                    new SolrPermission(Permission.AddDataset, emptyList()));
        }

        perms.addAll(findDvObjectPerms(version.getDataset(), SolrPermissionType.SEARCH)
                .get(SolrPermissionType.SEARCH));
        return new SolrPermissions(new SearchPermissions(perms, NEVER_PUBLIC),
                new SolrPermission(Permission.AddDataset, emptyList()));
    }

    /**
     * Returns {@link SolrPermissions} (currently only {@link SearchPermissions})
     * for fileMetadata inside the given dataset version.
     * <p>
     * Note that {@link SearchPermissions} are specific to parent
     * dataset version only.
     * <p>
     * This fact can be used to retrieve {@link SearchPermissions} for
     * every file metadata inside single dataset version only once.
     */
    public SolrPermissions findFileMetadataPermsFromDatasetVersion(final DatasetVersion version) {
        
        final Dataset dataset = version.getDataset();
        
        Instant publicFrom = version.publicFrom();

        final List<String> perms = new ArrayList<>();
        if (publicFrom != ALWAYS_PUBLIC) {
            perms.addAll(findDvObjectPerms(dataset, SolrPermissionType.SEARCH)
                    .get(SolrPermissionType.SEARCH));
        }

        return new SolrPermissions(new SearchPermissions(perms, publicFrom),
                new SolrPermission(Permission.AddDataset, emptyList()));
    }

    /**
     * Returns {@link DatasetVersion}s from the given {@link Dataset}
     * that needs permission objects indexing.
     * <p>
     * Note that returned versions should reflect what is in
     * {@link IndexServiceBean#indexDataset(Dataset, boolean)}
     */
    public Set<DatasetVersion> extractVersionsForPermissionIndexing(Dataset dataset) {
        Set<DatasetVersion> versionsToIndex = new HashSet<>();

        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        DatasetVersion latestVersion = dataset.getLatestVersion();


        if (releasedVersion != null) {
            versionsToIndex.add(releasedVersion);
        }

        if (latestVersion.getVersionState() == VersionState.DRAFT) {
            versionsToIndex.add(latestVersion);
        }
        if (latestVersion.getVersionState() == VersionState.DEACCESSIONED && releasedVersion == null) {
            versionsToIndex.add(latestVersion);
        }

        return versionsToIndex;
    }


    // -------------------- PRIVATE --------------------

    private Map<SolrPermissionType, List<String>> findDvObjectPerms(DvObject dvObject, SolrPermissionType... permissionTypes) {
        if (permissionTypes.length < 1) {
            return emptyMap();
        }
        Map<String, RoleAssignee> roleAssigneeCache = new HashMap<>(100);
        Set<RoleAssignment> roleAssignments = rolesSvc.rolesAssignments(dvObject);
        Map<SolrPermissionType, List<String>> result = new EnumMap<>(SolrPermissionType.class);
        for (SolrPermissionType permissionType : permissionTypes) {
            result.put(permissionType, new ArrayList<>());
        }
        for (RoleAssignment roleAssignment : roleAssignments) {
            logger.fine("role assignment on dvObject " + dvObject.getId() + ": " + roleAssignment.getAssigneeIdentifier());
            Set<Permission> currentPermissions = roleAssignment.getRole().permissions();
            for (SolrPermissionType permissionType : permissionTypes) {
                if (permissionType.test(dvObject, currentPermissions)) {
                    RoleAssignee userOrGroup = roleAssigneeCache.computeIfAbsent(roleAssignment.getAssigneeIdentifier(),
                            id -> roleAssigneeService.getRoleAssignee(id));
                    String indexableString = getIndexableStringForUserOrGroup(userOrGroup);
                    if (isNotBlank(indexableString)) {
                        result.get(permissionType).add(indexableString);
                    }
                }
            }
        }
        return result;
    }

    private boolean hasBeenPublished(Dataverse dataverse) {
        return dataverse.isReleased();
    }

    /**
     * From a Solr perspective we can't just index any string when we go to do
     * the JOIN to enforce security. (Maybe putting quotes around the string at
     * search time would allow this.) For users, we index the primary key from
     * the AuthenticatedUsers table. For groups we index the "alias" which
     * should be globally unique because non-builtin groups have a sort of a
     * name space with "shib/2" and "ip/ipGroup3", for example.
     */
    private String getIndexableStringForUserOrGroup(RoleAssignee userOrGroup) {
        
        if (userOrGroup instanceof AuthenticatedUser) {
            logger.fine(userOrGroup.getIdentifier() + " must be a user: " + userOrGroup.getClass().getName());
            AuthenticatedUser au = (AuthenticatedUser) userOrGroup;
            // Strong preference to index based on system generated value (e.g. primary key) whenever possible:
            // https://github.com/IQSS/dataverse/issues/1151
            Long primaryKey = au.getId();
            return IndexServiceBean.getGroupPerUserPrefix() + primaryKey;
        } else if (userOrGroup instanceof Group) {
            logger.fine(userOrGroup.getIdentifier() + " must be a group: " + userOrGroup.getClass().getName());
            Group group = (Group) userOrGroup;
            logger.fine("group: " + group.getAlias());
            String groupAlias = group.getAlias();
            if (groupAlias != null) {
                return IndexServiceBean.getGroupPrefix() + groupAlias;
            } else {
                logger.fine("Could not find group alias for " + group.getIdentifier());
            }
        }
        return null;
    }

    private enum SolrPermissionType {

        SEARCH {
            boolean test(final DvObject dvo, final Collection<Permission> p) {
                
                final Permission perm = dvo.isInstanceofDataverse() ? 
                          Permission.ViewUnpublishedDataverse
                        : Permission.ViewUnpublishedDataset;
                
                return p.contains(perm);
            }
        },
        ADD_DATASET {
            boolean test(final DvObject dvo, final Collection<Permission> p) {
                
                return dvo.isInstanceofDataverse() && p.contains(Permission.AddDataset);
            }
        };

        abstract boolean test(DvObject dvo, Collection<Permission> p);
    }
}
