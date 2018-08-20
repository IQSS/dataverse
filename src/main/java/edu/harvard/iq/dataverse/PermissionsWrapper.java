package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandHelper;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * <b>PLASE READ BEFOR USING THIS CLASS:</b>
 * 
 * This class gives us a great performance boost on the UI/JSF side. This is done,
 * in some rare edge cases, at the expense of correctness. Specifically, this 
 * class makes the following false assumptions, that are very very likely 
 * to be correct:
 * 
 * <ol>
 *  <li>Commands require permissions only over a single object</li>
 *  <li>The required permissions of said object live under the empty key</li>
 *  <li>The required permissions can be decided on statically (by using class
 *        annotations only, without calling {@link Command#getRequiredPermissions()}).</li>
 * </ol>
 * 
 * If any of these assumptions do not hold for your use case, <b>do not use this 
 * class!</b>
 * 
 * We use this class in the UI, and the risk we are running is merely
 * presentational: Some buttons might not be enabled/disabled as needed. Even
 * if a button is enabled when it shouldn't be, the command engine will reject 
 * the command, so no harm do the data could be made. And we get the performance
 * gains, which are critical to a human interface.
 *
 * Try to not use this class in APIs, though.
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
    
    private final Map<Long, Map<Class<? extends Command<?>>, Boolean>> isPermittedCache = new HashMap<>();

    // Maps for caching permissions lookup results:
    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { DvObject.id : Boolean }
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    /**
     * Check if a command can be issued in the current request.
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

        if (isPermittedCache.containsKey(dvo.getId())) {
            Map<Class<? extends Command<?>>, Boolean> dvoCommandMap = this.isPermittedCache.get(dvo.getId());
            if (dvoCommandMap.containsKey(command)) {
                return dvoCommandMap.get(command);
            } else {
                return addCommandtoDvoCommandMap(dvo, command, dvoCommandMap);
            }

        } else {
            Map<Class<? extends Command<?>>, Boolean> newDvoCommandMap = new HashMap<>();
            isPermittedCache.put(dvo.getId(), newDvoCommandMap);
            return addCommandtoDvoCommandMap(dvo, command, newDvoCommandMap);
        }
    }

    private boolean addCommandtoDvoCommandMap(DvObject dvo, Class<? extends Command<?>> commandClass, Map<Class<? extends Command<?>>, Boolean> dvoCommandMap) {
        if ( dvo==null || (dvo.getId()==null) ){
            return false;
        }
        if (commandClass==null){
            return false;
        }
        
        boolean canIssueCommand;
        Set<Permission> requiredPermissions = CommandHelper.CH.permissionsRequired(commandClass).get("");
        canIssueCommand = permissionService.hasPermissionsFor(dvRequestService.getDataverseRequest(), dvo, requiredPermissions);
        dvoCommandMap.put(commandClass, canIssueCommand);
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
        return permissionService.request(dvRequestService.getDataverseRequest())
                                .on(dv)
                                .has(Permission.ManageDataversePermissions);
    }
    
    public boolean canManageDatasetPermissions(User u, Dataset ds) {
        if ( ds==null || (ds.getId()==null)){
            return false;
        }
        if (u==null){            
            return false;
        }
        return permissionService.request(dvRequestService.getDataverseRequest())
                                .on(ds)
                                .has(Permission.ManageDatasetPermissions);
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
        boolean hasPermission = this.permissionService.request(req).on(dataset).has(permissionToCheck);

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
