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
import io.vavr.control.Option;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Determine whether items should be searchable.
 */
@Stateless
public class SearchPermissionsFinder {

    private static final Logger logger = Logger.getLogger(SearchPermissionsFinder.class.getCanonicalName());

    private RoleAssigneeServiceBean roleAssigneeService;
    private DataverseRoleServiceBean rolesSvc;
    
    // -------------------- CONSTRUCTORS --------------------
    
    @Deprecated
    public SearchPermissionsFinder() {
        // JEE requirement
    }
    
    @Inject
    public SearchPermissionsFinder(RoleAssigneeServiceBean roleAssigneeService, DataverseRoleServiceBean rolesSvc) {
        this.roleAssigneeService = roleAssigneeService;
        this.rolesSvc = rolesSvc;
    }

    // -------------------- LOGIC --------------------
    
    /**
     * Returns {@link SearchPermissions} on which Solr will JOIN
     * to enforce permissions.
     */
    public SearchPermissions findDataversePerms(Dataverse dataverse) {
        List<String> permStrings = new ArrayList<>();
        if (hasBeenPublished(dataverse)) {
            return new SearchPermissions(permStrings, SearchPermissions.ALWAYS_PUBLIC);
        }
        
        permStrings.addAll(findDvObjectPerms(dataverse));
        return new SearchPermissions(permStrings, SearchPermissions.NEVER_PUBLIC);
    }

    public SearchPermissions findDatasetVersionPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            return new SearchPermissions(perms, SearchPermissions.ALWAYS_PUBLIC);
        }
        
        perms.addAll(findDvObjectPerms(version.getDataset()));
        return new SearchPermissions(perms, SearchPermissions.NEVER_PUBLIC);
    }
    
    /**
     * Returns {@link SearchPermissions} for fileMetadata inside
     * the given dataset version.
     * <p>
     * Note that {@link SearchPermissions} are specific to parent
     * dataset version only.
     * <p>
     * This fact can be used to retrieve {@link SearchPermissions} for
     * every file metadata inside single dataset version only once.
     */
    public SearchPermissions findFileMetadataPermsFromDatasetVersion(DatasetVersion version) {
        Dataset dataset = version.getDataset();
        List<String> perms = new ArrayList<>();
        
        Instant publicFrom = SearchPermissions.NEVER_PUBLIC;
        
        if (version.isReleased() && !dataset.getEmbargoDate().isDefined()) {
            publicFrom = SearchPermissions.ALWAYS_PUBLIC;
        } else if (version.isReleased()) {
            publicFrom = dataset.getEmbargoDate()
                    .map(Date::toInstant)
                    .getOrElse(SearchPermissions.NEVER_PUBLIC);
        }
        
        
        if (publicFrom != SearchPermissions.ALWAYS_PUBLIC) {
            perms.addAll(findDvObjectPerms(dataset));
        }
        
        return new SearchPermissions(perms, publicFrom);
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
    
    private List<String> findDvObjectPerms(DvObject dvObject) {
        List<String> permStrings = new ArrayList<>();
        Map<String, RoleAssignee> roleAssigneeCache = new HashMap<>(100);
        
        Set<RoleAssignment> roleAssignments = rolesSvc.rolesAssignments(dvObject);
        for (RoleAssignment roleAssignment : roleAssignments) {
            logger.fine("role assignment on dvObject " + dvObject.getId() + ": " + roleAssignment.getAssigneeIdentifier());
            if (roleAssignment.getRole().permissions().contains(getRequiredSearchPermission(dvObject))) {
                
                roleAssigneeCache.computeIfAbsent(roleAssignment.getAssigneeIdentifier(), identifier -> roleAssigneeService.getRoleAssignee(identifier));
                RoleAssignee userOrGroup = roleAssigneeCache.get(roleAssignment.getAssigneeIdentifier());
                
                Option.of(getIndexableStringForUserOrGroup(userOrGroup))
                    .peek(permStrings::add);
            }
        }
        return permStrings;
    }
    
    private boolean hasBeenPublished(Dataverse dataverse) {
        return dataverse.isReleased();
    }

    private Permission getRequiredSearchPermission(DvObject dvObject) {
        if (dvObject.isInstanceofDataverse()) {
            return Permission.ViewUnpublishedDataverse;
        } else {
            return Permission.ViewUnpublishedDataset;
        }

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
            // Strong prefence to index based on system generated value (e.g. primary key) whenever possible: https://github.com/IQSS/dataverse/issues/1151
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
                return null;
            }
        } else {
            return null;
        }
    }

}
