package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All the permissions in the system are implemented as enum values in this
 * class. For performance, the permissions are stored internally in a bit field
 * (in effect, a {@code long}). This brings database fetches to a single action
 * rather than a join, and in-memory permission set unions to a bitwise or
 * rather than a tree merge. But some caution must be practiced when making
 * changes to this class.
 * <p>
 * =========================================================
 * IMPORTANT NOTES, READ BEFORE MAKING CHANGES TO THIS FILE
 * =========================================================
 * <p>
 * 1. Number of permissions must be kept under 64. If more
 * than 64 permissions are needed, storage must be updated
 * to include two {@code long}s, rather then the current one.
 * 2. Do not change the order of the enum values, and add new values only
 * after the last enum value. If you wish to change the order or add a
 * permission in between existing ones (or at the beginning), ALSO PROVIDE
 * A MIGRATION SCRIPT FOR THE DATABASE. Otherwise, permissions in the
 * database will be mis-assigned. This may be a major security issue.
 *
 * @author michael
 */
public enum Permission implements java.io.Serializable {

    // Create
    //1
    AddDataverse(BundleUtil.getStringFromBundle("permission.addDataverseDataverse"), true, Dataverse.class),
    //2
    AddDataset(BundleUtil.getStringFromBundle("permission.addDatasetDataverse"), true, Dataverse.class),
    // Read
    //4
    ViewUnpublishedDataverse(BundleUtil.getStringFromBundle("permission.viewUnpublishedDataverse"), false, Dataverse.class),
    //8
    ViewUnpublishedDataset(BundleUtil.getStringFromBundle("permission.viewUnpublishedDataset"), false, Dataset.class),
    //16
    DownloadFile(BundleUtil.getStringFromBundle("permission.downloadFile"), false, DataFile.class),
    // Update
    //32
    EditDataverse(BundleUtil.getStringFromBundle("permission.editDataverse"), true, Dataverse.class),
    //64
    EditDataset(BundleUtil.getStringFromBundle("permission.editDataset"), true, Dataset.class),
    //128
    ManageDataversePermissions(BundleUtil.getStringFromBundle("permission.managePermissionsDataverse"), true, Dataverse.class),
    //256
    ManageDatasetPermissions(BundleUtil.getStringFromBundle("permission.managePermissionsDataset"), true, Dataset.class),
    //512
    PublishDataverse(BundleUtil.getStringFromBundle("permission.publishDataverse"), true, Dataverse.class),
    //1024
    PublishDataset(BundleUtil.getStringFromBundle("permission.publishDataset"), true, Dataset.class, Dataverse.class),
    // Delete
    //2048
    DeleteDataverse(BundleUtil.getStringFromBundle("permission.deleteDataverse"), true, Dataverse.class),
    //4096
    DeleteDatasetDraft(BundleUtil.getStringFromBundle("permission.deleteDataset"), true, Dataset.class),
    //8192
    ManageMinorDatasetPermissions(BundleUtil.getStringFromBundle("permission.manageMinorDatasetPermissions"), true, Dataset.class);

    // FUTURE:
    //RestrictMetadata("Mark metadata as restricted", DvObject.class),
    //AccessRestrictedMetadata("Access metadata marked as\"restricted\"", DvObject.class),

    /**
     * A human readable name for the permission.
     */
    private final String humanName;

    /**
     * Which types of {@link DvObject}s this permission applies to.
     */
    private final Set<Class<? extends DvObject>> appliesTo;

    /**
     * Can this permission be applied only to {@link AuthenticatedUser}s, or to any user?
     */
    private final boolean requiresAuthenticatedUser;

    @SafeVarargs
    Permission(String aHumanName, boolean authenticatedUserRequired, Class<? extends DvObject>... appliesToList) {
        humanName = aHumanName;
        appliesTo = new HashSet<>(Arrays.asList(appliesToList));
        requiresAuthenticatedUser = authenticatedUserRequired;
    }

    public String getHumanName() {
        return BundleUtil.getStringFromBundle("permission." + name() + ".desc");
    }

    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("permission." + name() + ".label");
    }

    public boolean appliesTo(Class<? extends DvObject> aClass) {
        for (Class<? extends DvObject> c : appliesTo) {
            if (c.isAssignableFrom(aClass)) {
                return true;
            }
        }
        return false;
    }

    public boolean requiresAuthenticatedUser() {
        return requiresAuthenticatedUser;
    }
}
