package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static, testable methods with no runtime dependencies.
 */
public class PrivateUrlUtil {

    private static final Logger logger = Logger.getLogger(PrivateUrlUtil.class.getCanonicalName());

    /**
     * Use of this method should be limited to
     * RoleAssigneeServiceBean.getRoleAssignee. If you have the
     * {@link RoleAssignment} in your hand, just instantiate a
     * {@link PrivateUrlUser} using the definitionPoint.
     *
     * @param identifier For example, "#42". The identifier is expected to start
     * with "#" (the namespace for a PrivateUrlUser and its corresponding
     * RoleAssignment) and end with the dataset id.
     *
     * @return A valid PrivateUrlUser (which like any User or Group is a
     * RoleAssignee) if a valid identifier is provided or null.
     */
    public static RoleAssignee identifier2roleAssignee(String identifier) {
        String[] parts = identifier.split(PrivateUrlUser.PREFIX);
        long datasetId;
        try {
            datasetId = new Long(parts[1]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            logger.fine("Could not find dataset id in '" + identifier + "': " + ex);
            return null;
        }
        return new PrivateUrlUser(datasetId);
    }

    /**
     * @todo If there is a use case for this outside the context of Private URL,
     * move this method to somewhere more centralized.
     */
    static Dataset getDatasetFromRoleAssignment(RoleAssignment roleAssignment) {
        if (roleAssignment == null) {
            return null;
        }
        DvObject dvObject = roleAssignment.getDefinitionPoint();
        if (dvObject == null) {
            return null;
        }
        if (dvObject instanceof Dataset) {
            return (Dataset) roleAssignment.getDefinitionPoint();
        } else {
            return null;
        }
    }

    /**
     * @return DatasetVersion if a draft or null.
     *
     * @todo If there is a use case for this outside the context of Private URL,
     * move this method to somewhere more centralized.
     */
    static public DatasetVersion getDraftDatasetVersionFromRoleAssignment(RoleAssignment roleAssignment) {
        if (roleAssignment == null) {
            return null;
        }
        Dataset dataset = getDatasetFromRoleAssignment(roleAssignment);
        if (dataset != null) {
            DatasetVersion latestVersion = dataset.getLatestVersion();
            if (latestVersion.isDraft()) {
                return latestVersion;
            }
        }
        logger.fine("Couldn't find draft, returning null");
        return null;
    }

    static public PrivateUrlUser getPrivateUrlUserFromRoleAssignment(RoleAssignment roleAssignment) {
        if (roleAssignment == null) {
            return null;
        }
        Dataset dataset = getDatasetFromRoleAssignment(roleAssignment);
        if (dataset != null) {
            PrivateUrlUser privateUrlUser = new PrivateUrlUser(dataset.getId(), roleAssignment.isAnonymizedAccess());
            return privateUrlUser;
        }
        return null;
    }

    /**
     * @param roleAssignment
     * @return PrivateUrlRedirectData or null.
     *
     * @todo Show the Exception to the user?
     */
    public static PrivateUrlRedirectData getPrivateUrlRedirectData(RoleAssignment roleAssignment) {
        PrivateUrlUser privateUrlUser = PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(roleAssignment);
        String draftDatasetPageToBeRedirectedTo = PrivateUrlUtil.getDraftDatasetPageToBeRedirectedTo(roleAssignment);
        try {
            return new PrivateUrlRedirectData(privateUrlUser, draftDatasetPageToBeRedirectedTo);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Exception caught trying to instantiate PrivateUrlRedirectData: " + ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Returns a relative URL or "UNKNOWN."
     */
    static String getDraftDatasetPageToBeRedirectedTo(RoleAssignment roleAssignment) {
        DatasetVersion datasetVersion = getDraftDatasetVersionFromRoleAssignment(roleAssignment);
        return getDraftUrl(datasetVersion);
    }

    /**
     * Returns a relative URL or "UNKNOWN."
     */
    static String getDraftUrl(DatasetVersion draft) {
        if (draft != null) {
            Dataset dataset = draft.getDataset();
            if (dataset != null && dataset.getGlobalId()!=null) {
                if ( dataset.getGlobalId().isComplete() ) {
                    String relativeUrl = "/dataset.xhtml?persistentId=" + dataset.getGlobalId().toString() + "&version=DRAFT";
                    return relativeUrl;
                }
            }
        }
        return "UNKNOWN";
    }

    static PrivateUrl getPrivateUrlFromRoleAssignment(RoleAssignment roleAssignment, String dataverseSiteUrl) {
        if (dataverseSiteUrl == null) {
            logger.info("dataverseSiteUrl was null. Can not instantiate a PrivateUrl object.");
            return null;
        }
        Dataset dataset = PrivateUrlUtil.getDatasetFromRoleAssignment(roleAssignment);
        if (dataset != null) {
            PrivateUrl privateUrl = new PrivateUrl(roleAssignment, dataset, dataverseSiteUrl);
            return privateUrl;
        } else {
            return null;
        }
    }

    static PrivateUrlUser getPrivateUrlUserFromRoleAssignment(RoleAssignment roleAssignment, RoleAssignee roleAssignee) {
        if (roleAssignment != null) {
            if (roleAssignee instanceof PrivateUrlUser) {
                return (PrivateUrlUser) roleAssignee;
            }
        }
        return null;
    }

    /**
     * @return A list of the CamelCase "names" of required permissions, not the
     * human-readable equivalents.
     *
     * @todo Move this to somewhere more central.
     */
    public static List<String> getRequiredPermissions(CommandException ex) {
        List<String> stringsToReturn = new ArrayList<>();
        Map<String, Set<Permission>> map = ex.getFailedCommand().getRequiredPermissions();
        map.entrySet().stream().forEach((entry) -> {
            entry.getValue().stream().forEach((permission) -> {
                stringsToReturn.add(permission.name());
            });
        });
        return stringsToReturn;
    }

}
