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
import edu.harvard.iq.dataverse.engine.command.impl.*;
import java.util.HashMap;
import java.util.Map;
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
        return canIssueCommand(dvo, AbstractCreateDatasetCommand.class);
    }

    // UPDATE DATASET
    public boolean canIssueUpdateDatasetCommand(DvObject dvo){
        return canIssueCommand(dvo, UpdateDatasetVersionCommand.class);
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
