/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class PermissionsWrapper implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(PermissionsWrapper.class.getName());

    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseSession session;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    @Inject
    SettingsWrapper settingsWrapper;

    // This map stores looked up permission results (boolean), for multiple DvObjects
    // (referenced by the Long ids), for multiple Commands. The current session 
    // user is assumed when lookups are performed. 
    private final Map<Long, Map<Class<? extends Command<?>>, Boolean>> commandMap = new HashMap<>();
    
    // In some instances our pages need to know whether Authenticated Users as a whole - 
    // not a specific user! - can perform a specific action on a dataverse
    // - such as create datasets or dataverses, in the current dv, or root etc. 
    // These values can be used in rendered= logic, so we want to cache them too. 
    private final Map<Long, Map<Class<? extends Command<?>>, Boolean>> authUsersCommandMap = new HashMap<>();

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
        if (dvo==null || dvo.getId()==null || command==null){
            return false;
        }

        if (checkDvoCacheForCommandAuthorization(dvo.getId(), command, commandMap) == null) {
            boolean canIssueCommand = permissionService.requestOn(dvRequestService.getDataverseRequest(), dvo).canIssue(command);
            logger.fine("retrieved authorization for " + command.toString() + " on dvo " + dvo.getId());

            addCommandAuthorizationToDvoCache(dvo.getId(), command, commandMap, canIssueCommand);
        } else {
            logger.fine("using cached authorization for " + command.toString() + " on dvo " + dvo.getId());
        }
        return checkDvoCacheForCommandAuthorization(dvo.getId(), command, commandMap);
    }

    private Boolean checkDvoCacheForCommandAuthorization(Long id, Class<? extends Command<?>> command, Map<Long, Map<Class<? extends Command<?>>, Boolean>> dvoCommandMap) {
        if (!dvoCommandMap.containsKey(id)) {
            return null;
        }
        if (!dvoCommandMap.get(id).containsKey(command)) {
            return null; 
        }
        return dvoCommandMap.get(id).get(command);
    }
    
    
    private void addCommandAuthorizationToDvoCache(Long id, Class<? extends Command<?>> command, Map<Long, Map<Class<? extends Command<?>>, Boolean>> dvoCommandMap, boolean canIssueCommand) {
        if (!dvoCommandMap.containsKey(id)) {
            dvoCommandMap.put(id, new HashMap<>());
        }
        dvoCommandMap.get(id).put(command, canIssueCommand);
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
    
    // TODO: 
    // Wondering if there's a reason why we don't attempt to cache the 
    // looked up Permission.* results as well as Command authorizations
    // (below). For starters, need to investigate just how much these 
    // cost us under different scenarios. May be a moot point. -- L.A. 5.7
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
    
    public boolean canManageFilesOnDataset(Dataset dataset) {
        return doesSessionUserHaveDataSetPermission(dvRequestService.getDataverseRequest(), dataset, Permission.ManageFilePermissions);
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
    
    // For the dataverse_header fragment (and therefore, most of the pages),
    // we need to know if authenticated users can add dataverses and datasets to the
    // root collection. For the "Add Data" menu further in the search include fragment
    // if the user is not logged in, the page will check if authenticated users can 
    // add dataverses and datasets in the *current* dataverse. 
    // These are not very expensive operations - but each take about 10 database queries, 
    // and it'll add up quickly, if the page keeps evaluating the expressions repeatedly. 
    // So these values absolutely need to be cached too. 
    
    public boolean authenticatedUsersCanIssueCommand(DvObject dvo, Class<? extends Command<?>> command) {
        if (dvo == null || dvo.getId() == null || command == null) {
            return false;
        }
        if (checkDvoCacheForCommandAuthorization(dvo.getId(), command, authUsersCommandMap) == null) {
            boolean canIssueCommand = permissionService.isUserAllowedOn(AuthenticatedUsers.get(), command, dvo);
            logger.fine("retrieved the authorization for authenticated users to issue "+command.toString());
            addCommandAuthorizationToDvoCache(dvo.getId(), command, authUsersCommandMap, canIssueCommand);
        } else {
            logger.fine("using cached authorization for authenticated users to issue "+command.toString());
        }
        return checkDvoCacheForCommandAuthorization(dvo.getId(), command, authUsersCommandMap);
    }
    
    public boolean authUsersCanCreateDatasetsInDataverse(Dataverse dataverse) {
        return authenticatedUsersCanIssueCommand(dataverse, CreateNewDatasetCommand.class);
    }
    
    public boolean authUsersCanCreateDataversesInDataverse(Dataverse dataverse) {
        return authenticatedUsersCanIssueCommand(dataverse, CreateDataverseCommand.class);
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
