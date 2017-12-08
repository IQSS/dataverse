/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class PermissionsWrapper implements java.io.Serializable {

    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseSession session;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;

    private final Map<Long, Map<Class<? extends Command<?>>, Boolean>> commandMap = new HashMap<>();

    // Maps for caching permissions lookup results:
    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { DvObject.id : Boolean }
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    /**
     * Check if the current Dataset can Issue Commands
     *
     * @param dvo Target dataverse object.
     * @param command The command to execute
     * @return {@code true} if the user can issue the command on the object.
     */
    public boolean canIssueCommand(DvObject dvo, Class<? extends Command<?>> command) {
        if ((dvo==null) || (dvo.getId()==null)){
            return false;
        }
        if (command==null){
            return false;
        }

        if (commandMap.containsKey(dvo.getId())) {
            Map<Class<? extends Command<?>>, Boolean> dvoCommandMap = this.commandMap.get(dvo.getId());
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
        if ( dvo==null || (dvo.getId()==null) ){
            return false;
        }
        if (command==null){
            return false;
        }
        
        boolean canIssueCommand;
        canIssueCommand = permissionService.requestOn(dvRequestService.getDataverseRequest(), dvo).canIssue(command);
        dvoCommandMap.put(command, canIssueCommand);
        return canIssueCommand;
    }

    /* Dataverse Commands */

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
    
    
    public boolean canManagePermissions(DvObject dvo) {
        if (dvo==null || (dvo.getId()==null) ){
            return false;
        }
        
        User u = session.getUser();
        return dvo instanceof Dataverse
                ? canManageDataversePermissions(u, (Dataverse) dvo)
                : canManageDatasetPermissions(u, (Dataset) dvo);
    }
    
    public boolean canManageDataversePermissions(User u, Dataverse dv) {
        if ( dv==null || (dv.getId()==null)){
            return false;
        }
        if (u==null){            
            return false;
        }
        return permissionService.requestOn(dvRequestService.getDataverseRequest(), dv).has(Permission.ManageDataversePermissions);
    }
    
    public boolean canManageDatasetPermissions(User u, Dataset ds) {
        if ( ds==null || (ds.getId()==null)){
            return false;
        }
        if (u==null){            
            return false;
        }
        return permissionService.requestOn(dvRequestService.getDataverseRequest(), ds).has(Permission.ManageDatasetPermissions);
    }

    public boolean canViewUnpublishedDataset(DataverseRequest dr, Dataset dataset) {
        return doesSessionUserHaveDataSetPermission(dr, dataset, Permission.ViewUnpublishedDataset);
    }
    
    public boolean canUpdateDataset(DataverseRequest dr, Dataset dataset) {
        return doesSessionUserHaveDataSetPermission(dr, dataset, Permission.EditDataset);
    }
    
    public void checkEditDatasetLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators. They can still make edits.
                
        //MAD: here I need to use the wrapper instead
        //MAD: getRequiredPermissions I think throws a IllegalArgumentException. not sure what to do if so... probably just let it trickle up if this is all based around exceptions???
            //command.getRequiredPermissions()

                //if (!isUserAllowedOn(dataverseRequest.getUser(), new PublishDatasetCommand(dataset, dataverseRequest, true), dataset)) {
                if (!doesSessionUserHaveDataSetPermissions(dvRequestService.getDataverseRequest(), dataset, new PublishDatasetCommand(dataset, dataverseRequest, true))) {
                    throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowedInReview"), command);
                }
            }
            if (dataset.isLockedFor(DatasetLock.Reason.Ingest)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
            // TODO: Do we need to check for "Workflow"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
            // TODO: Do we need to check for "DcmUpload"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
        }
    }
    
    //MAD: same idea as above
    //MAD: write tests for both?
    public void checkDownloadFileLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators or contributors. They can still download.                
                //if (!isUserAllowedOn(dataverseRequest.getUser(), new UpdateDatasetCommand(dataset, dataverseRequest), dataset)) {
                if (!doesSessionUserHaveDataSetPermissions(dvRequestService.getDataverseRequest(), dataset, new UpdateDatasetCommand(dataset, dataverseRequest))) {
                    throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowedInReview"), command);
                }
            }
            if (dataset.isLockedFor(DatasetLock.Reason.Ingest)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
            // TODO: Do we need to check for "Workflow"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
            // TODO: Do we need to check for "DcmUpload"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
        }
    }
            
    
    /**
     * (Using Raman's implementation in DatasetPage - moving it here, so that 
     * other components could use this optimization -- L.A. 4.2.1)
     * 
     * Check Dataset related permissions
     * 
     * @param req
     * @param dataset
     * @param permissionToCheck
     * @return 
     */
    public boolean doesSessionUserHaveDataSetPermission(DataverseRequest req, Dataset dataset, Permission permissionToCheck){
        if (permissionToCheck == null){
            return false;
        }
               
        String permName = permissionToCheck.getHumanName();
       
        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)){
            // Yes, return previous answer
            return this.datasetPermissionMap.get(permName);
        }
        
        // Check the permission
        boolean hasPermission = this.permissionService.requestOn(req, dataset).has(permissionToCheck);

        // Save the permission
        this.datasetPermissionMap.put(permName, hasPermission);
        
        // return true/false
        return hasPermission;
    }
    
    //Takes in a command, gets the permissions to check and calls the individual permissions check
    public boolean doesSessionUserHaveDataSetPermissions(DataverseRequest req, Dataset dataset, Command command){
        //MAD: In here I need to call doesSessionUserHaveDataSetPermission for each permission. I have no idea what that map is tho...
        //it seems the map is "dataverse name" "required permissions" but may be a singleton in most cases???
        
        //public static void printMap(Map mp)
        Map<String, Set<Permission>> permissionsToCheck = command.getRequiredPermissions();
        Iterator it = permissionsToCheck.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Set<Permission>> pair = (Map.Entry)it.next();

            for (Permission p : pair.getValue()) {
                if(!doesSessionUserHaveDataSetPermission(req, dataset, p)) {
                    return false;
                }
            }
           
            it.remove(); // avoids a ConcurrentModificationException
        }

        return true;
    }
    
    /**
     *  Does this dvoObject have "Permission.DownloadFile"?
     * @param dvo
     * @return 
     */
    public boolean hasDownloadFilePermission(DvObject dvo){
        
        if ((dvo==null)||(dvo.getId() == null)){
            return false;
        }
        
        // Has this check already been done? Check the hash
        //
        if (this.fileDownloadPermissionMap.containsKey(dvo.getId())){
            // Yes, return previous answer
            return this.fileDownloadPermissionMap.get(dvo.getId());
        }

        // Check permissions
        //
        if ( permissionService.on(dvo).has(Permission.DownloadFile) ){

            // Yes, has permission, store result
            fileDownloadPermissionMap.put(dvo.getId(), true);
            return true;
            
        } else {
        
            // No permission, store result
            fileDownloadPermissionMap.put(dvo.getId(), false);
            return false;
        }
    }
    

    
    /* -----------------------------------
        Dataset Commands 
     ----------------------------------- */
    
    // CREATE DATASET
    public boolean canIssueCreateDatasetCommand(DvObject dvo){
        return canIssueCommand(dvo, CreateDatasetCommand.class);
    }

    // UPDATE DATASET
    public boolean canIssueUpdateDatasetCommand(DvObject dvo){
        return canIssueCommand(dvo, UpdateDatasetCommand.class);
    }

    // DELETE DATASET
    public boolean canIssueDeleteDatasetCommand(DvObject dvo){
        return canIssueCommand(dvo, DeleteDatasetCommand.class);
    }
    
    // PLUBLISH DATASET
    public boolean canIssuePublishDatasetCommand(DvObject dvo){
        return canIssueCommand(dvo, PublishDatasetCommand.class);
    }
    
    
    
    
    
    // todo: move any calls to this to call NavigationWrapper   
    @Inject NavigationWrapper navigationWrapper;
    
    public String notAuthorized(){
        return navigationWrapper.notAuthorized();
    }
    
    public String notFound() {
        return navigationWrapper.notFound();
    }

}
