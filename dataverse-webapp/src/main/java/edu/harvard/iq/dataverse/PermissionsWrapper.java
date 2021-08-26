package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractCreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class PermissionsWrapper implements java.io.Serializable {

    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    private final Map<Long, Map<Class<? extends Command<?>>, Boolean>> commandMap = new HashMap<>();

    // Maps for caching permissions lookup results:
    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { DvObject.id : Boolean }
    private final Map<Permission, Boolean> datasetPermissionMap = new HashMap<>();

    /**
     * Check if the current Dataset can Issue Commands
     *
     * @param dvo     Target dataverse object.
     * @param command The command to execute
     * @return {@code true} if the user can issue the command on the object.
     */
    public boolean canIssueCommand(DvObject dvo, Class<? extends Command<?>> command) {
        if ((dvo == null) || (dvo.getId() == null)) {
            return false;
        }
        if (command == null) {
            return false;
        }

        if (commandMap.containsKey(dvo.getId())) {
            Map<Class<? extends Command<?>>, Boolean> dvoCommandMap = commandMap.get(dvo.getId());
            if (dvoCommandMap.containsKey(command)) {
                return dvoCommandMap.get(command);
            } else {
                return addCommandtoDvoCommandMap(dvo, command, dvoCommandMap);
            }

        } else {
            Map<Class<? extends Command<?>>, Boolean> newDvoCommandMap = new HashMap<>();
            commandMap.put(dvo.getId(), newDvoCommandMap);
            return addCommandtoDvoCommandMap(dvo, command, newDvoCommandMap);
        }
    }

    private boolean addCommandtoDvoCommandMap(DvObject dvo, Class<? extends Command<?>> command, Map<Class<? extends Command<?>>, Boolean> dvoCommandMap) {
        if (dvo == null || (dvo.getId() == null)) {
            return false;
        }
        if (command == null) {
            return false;
        }

        boolean canIssueCommand;
        canIssueCommand = permissionService.requestOn(dvRequestService.getDataverseRequest(), dvo).canIssue(command);
        dvoCommandMap.put(command, canIssueCommand);
        return canIssueCommand;
    }

    /* Dataverse Commands */

    public boolean canViewUnpublishedDataverse(Dataverse dataverse) {
        return permissionService
                .requestOn(dvRequestService.getDataverseRequest(), dataverse)
                .has(Permission.ViewUnpublishedDataverse);
    }

    public boolean canIssueUpdateDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, UpdateDataverseCommand.class);
    }

    public boolean canIssuePublishDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, PublishDataverseCommand.class);
    }

    public boolean canIssueDeleteDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, DeleteDataverseCommand.class);
    }

    public boolean canIssueCreateDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, CreateDataverseCommand.class);
    }

    public boolean canEditDataverseTextMessagesAndBanners(Long dataverseId) {
        return permissionService.isUserCanEditDataverseTextMessagesAndBanners(dvRequestService.getDataverseRequest().getUser(), dataverseId);
    }

    public boolean canManagePermissions(DvObject dvo) {
        if (dvo == null || (dvo.getId() == null)) {
            return false;
        }

        return dvo instanceof Dataverse
                ? canManageDataversePermissions((Dataverse) dvo)
                : canManageDatasetOrMinorDatasetPermissions((Dataset) dvo);
    }

    public boolean canManageDataversePermissions(Dataverse dv) {
        if (dv == null || (dv.getId() == null)) {
            return false;
        }
        return permissionService.requestOn(dvRequestService.getDataverseRequest(), dv).has(Permission.ManageDataversePermissions);
    }

    public boolean canManageDatasetOrMinorDatasetPermissions(Dataset ds) {
        if (ds == null || (ds.getId() == null)) {
            return false;
        }
        return permissionService.requestOn(dvRequestService.getDataverseRequest(), ds).has(Permission.ManageDatasetPermissions) ||
                permissionService.requestOn(dvRequestService.getDataverseRequest(), ds).has(Permission.ManageMinorDatasetPermissions);
    }

    public boolean canViewUnpublishedDataset(Dataset dataset) {
        return doesSessionUserHaveDataSetPermission(dvRequestService.getDataverseRequest(), dataset, Permission.ViewUnpublishedDataset);
    }

    public boolean canCurrentUserUpdateDataset(Dataset dataset) {
        DataverseRequest dataverseRequest = dvRequestService.getDataverseRequest();
        return doesSessionUserHaveDataSetPermission(dataverseRequest, dataset, Permission.EditDataset);
    }

    public boolean canUpdateAndPublishDataset(Dataset dataset) {
        return canCurrentUserUpdateDataset(dataset) && canIssuePublishDatasetCommand(dataset);
    }

    /**
     * (Using Raman's implementation in DatasetPage - moving it here, so that
     * other components could use this optimization -- L.A. 4.2.1)
     * <p>
     * Check Dataset related permissions
     *
     * @param req
     * @param dataset
     * @param permissionToCheck
     * @return
     */
    private boolean doesSessionUserHaveDataSetPermission(DataverseRequest req, Dataset dataset, Permission permissionToCheck) {
        if (permissionToCheck == null) {
            return false;
        }

        // Has this check already been done? 
        // 
        if (datasetPermissionMap.containsKey(permissionToCheck)) {
            // Yes, return previous answer
            return datasetPermissionMap.get(permissionToCheck);
        }

        // Check the permission
        boolean hasPermission = permissionService.requestOn(req, dataset).has(permissionToCheck);

        // Save the permission
        datasetPermissionMap.put(permissionToCheck, hasPermission);

        // return true/false
        return hasPermission;
    }

    /**
     * Does this dvoObject have "Permission.DownloadFile"?
     *
     * @param datafile
     * @return
     */
    public boolean hasDownloadFilePermission(DataFile datafile) {

        if ((datafile == null) || (datafile.getId() == null)) {
            return false;
        }

        // Has this check already been done? Check the hash
        //
        if (fileDownloadPermissionMap.containsKey(datafile.getId())) {
            // Yes, return previous answer
            return fileDownloadPermissionMap.get(datafile.getId());
        }

        // Check permissions
        //
        if (permissionService.requestOn(dvRequestService.getDataverseRequest(), datafile).has(Permission.DownloadFile)) {

            // Yes, has permission, store result
            fileDownloadPermissionMap.put(datafile.getId(), true);
            return true;

        } else {

            // No permission, store result
            fileDownloadPermissionMap.put(datafile.getId(), false);
            return false;
        }
    }
    

    
    /* -----------------------------------
        Dataset Commands 
     ----------------------------------- */

    // CREATE DATASET
    public boolean canIssueCreateDatasetCommand(DvObject dvo) {
        return canIssueCommand(dvo, AbstractCreateDatasetCommand.class);
    }

    // UPDATE DATASET
    public boolean canIssueUpdateDatasetCommand(DvObject dvo) {
        return canIssueCommand(dvo, UpdateDatasetVersionCommand.class);
    }

    // DELETE DATASET
    public boolean canIssueDeleteDatasetCommand(DvObject dvo) {
        return canIssueCommand(dvo, DeleteDatasetCommand.class);
    }

    // PLUBLISH DATASET
    public boolean canIssuePublishDatasetCommand(DvObject dvo) {
        return canIssueCommand(dvo, PublishDatasetCommand.class);
    }


    // todo: move any calls to this to call NavigationWrapper
    @Inject
    NavigationWrapper navigationWrapper;

    public String notAuthorized() {
        return navigationWrapper.notAuthorized();
    }

    public String notFound() {
        return navigationWrapper.notFound();
    }

}
