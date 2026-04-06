package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;


import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Given a draft datasetVersion (which has no published predecessors), and PID provider that is different to the current
 * configured one this command updates the PID to the new provider (Removed the existing PID, registers a new one and notifies the user about the change).
 *
 * @author jdarms
 */
// No required permissions because we check for superuser status.
@RequiredPermissions({})
public class ReconcileDatasetPidCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(ReconcileDatasetPidCommand.class.getName());
    PidProvider newPidProvider;

    public ReconcileDatasetPidCommand(DataverseRequest aRequest, Dataset theDataset, PidProvider newPidProvider) {
        super(aRequest, theDataset);
        this.newPidProvider = newPidProvider;
    }


    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        // ensure that only superuser can execute the command.
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException(BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"), this,
                    Collections.singleton(Permission.EditDataset), getDataset());
        }
        // Datast must be unreleased! This means there is only one version!
        if (getDataset().isReleased()) {
            throw new IllegalCommandException("Dataset already published, cannot alter PID Provider", this);
        }
        // Dataset must not be harvested!
        if (getDataset().isHarvested()) {
            throw new IllegalCommandException("Dataset is harvested, cannot alter PID Provider", this);
        }
        GlobalId oldId = getDataset().getGlobalId();

        if (oldId == null) {
            throw new IllegalStateException("Dataset without a global identifier, cannot be altered!");
        }
        PidProvider currentPidProvider = PidUtil.getPidProvider(oldId.getProviderId());
        // new PID Provider must be different to requested one!
        if (this.newPidProvider.equals(currentPidProvider)) {
            throw new IllegalCommandException("PID Provider " + currentPidProvider.getId() + " is same as configured. This Operation has no effect!", this);
        }

        logger.fine("Reconciling dataset( id =`" + getDataset().getId() + ")` - removing globalId `" + getDataset().getGlobalId() + '`');
        // remove dataset PID
        try {
            if (currentPidProvider.alreadyRegistered(getDataset())) { //if not registered with PIDProvider than there is no need to delete it...
                currentPidProvider.deleteIdentifier(getDataset()); // delete it externally
            }
            getDataset().setGlobalId(null); //  remove it internally
            getDataset().setGlobalIdCreateTime(null);
            getDataset().setIdentifierRegistered(false);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier deletion was not successful:", e.getMessage());
        }

        if (ctxt.systemConfig().isFilePIDsEnabledForCollection(getDataset().getOwner())) {
            reconcileFilePids(ctxt);
        }

        newPidProvider.generatePid(getDataset()); // this updates Protocol, Authority, and Identifier and thus a new GlobalID
        logger.fine("Reconciling dataset( id =`" + getDataset().getId() + ")` - creating new globalId `" + getDataset().getGlobalId() + '`');
        if (!newPidProvider.registerWhenPublished()) {
            registerExternalIdentifier(getDataset(), ctxt, true); // this updates GlobalIdCreateTime and IdentifierRegistered
        }

        AlternativePersistentIdentifier api;
        api = new AlternativePersistentIdentifier();
        api.setProtocol(oldId.getProtocol());
        api.setAuthority(oldId.getAuthority());
        api.setIdentifier(oldId.getIdentifier());
        api.setDvObject(getDataset());
        api.setStorageLocationDesignator(true);// cf. Dataset#getIdentifierForFileStorage()
        if (getDataset().getAlternativePersistentIndentifiers() != null) {
            // check iF an alternative ID is already used as storage location designator
            Optional<AlternativePersistentIdentifier> storageId = getDataset().getAlternativePersistentIndentifiers().stream().filter(s -> s.isStorageLocationDesignator()).findAny();
            if (storageId.isPresent()) {
                api.setStorageLocationDesignator(false);// If there is already a storage location designator we do not alter it.
            }
            getDataset().getAlternativePersistentIndentifiers().add(api);
        } else {
            getDataset().setAlternativePersistentIndentifiers(Set.of(api));
        }

        // We keep the old persistent identifier as AlternativePersistentIdentifier with storageLocationDesignator true.
        // This keep the link the object store intact, without altering the files. A superuser can update the storage
        // manually from old file path to the new one, and remove the AlternativeIdentifer form the database.
        // This removed the old identifier totally from the system and avoids all side effects...
        ctxt.em().merge(getDataset());
        ctxt.em().flush();

        logger.fine("Reconciling dataset( id =`" + getDataset().getId() + ")` - Replaced old globalId `" + oldId + " with new globalId `" + getDataset().getGlobalId() + '`');
        // notify all users with direct role assignments about the changed persistent identifier
        List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(getDataset());
        for (RoleAssignment ra : ras) {
            for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.PIDRECONCILED, getDataset().getLatestVersion().getId(), "Persistent identifier changed!");
            }
        }

        return getDataset();
    }

    private void reconcileFilePids(CommandContext ctxt) {
        // remove datafile PIDs
        try {
            for (DataFile df : getDataset().getFiles()) {
                GlobalId oldPid = df.getGlobalId();
                if(df.getGlobalId()==null){
                    // if there is no global ID of a datafile, there is no need to reconcile anything.
                    continue;
                }
                PidProvider currentPidProvider = PidUtil.getPidProvider(oldPid.getProviderId());
                if (currentPidProvider.alreadyRegistered(df)) {
                    currentPidProvider.deleteIdentifier(df);// delete it external
                }

                df.setGlobalId(null); // and remove it internally from data structure
                df.setGlobalIdCreateTime(null);
                df.setIdentifierRegistered(false);

                AlternativePersistentIdentifier api;
                api = new AlternativePersistentIdentifier();
                api.setProtocol(oldPid.getProtocol());
                api.setAuthority(oldPid.getAuthority());
                api.setIdentifier(oldPid.getIdentifier());
                api.setDvObject(df);
                api.setStorageLocationDesignator(true); // cf. Dataset#getIdentifierForFileStorage()
                if (df.getAlternativePersistentIndentifiers() != null) {
                    // check iF an alternative ID is already used as storage location designator
                    Optional<AlternativePersistentIdentifier> storageId = df.getAlternativePersistentIndentifiers().stream().filter(s -> s.isStorageLocationDesignator()).findAny();
                    if (storageId.isPresent()) {
                        api.setStorageLocationDesignator(false);// If there is already a storage location designator we do not alter it.
                    }
                    df.getAlternativePersistentIndentifiers().add(api);
                } else {
                    df.setAlternativePersistentIndentifiers(Set.of(api));
                }
                newPidProvider.generatePid(df); // this updates Protocol, Authority, and Identifier and thus a new GlobalID
                logger.fine("Reconciling datafile( id =`" + df.getId() + ")` - creating new globalId `" + df.getGlobalId() + '`');
                if (!newPidProvider.registerWhenPublished()) {
                    registerExternalIdentifier(df, ctxt, true); // this updates GlobalIdCreateTime and IdentifierRegistered
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Reconcilation of the datafile persistent identifier was not successful:", e.getMessage());
        }
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        //update search index with the state
        ctxt.index().asyncIndexDataset(getDataset(), true);
        return true;
    }
}
