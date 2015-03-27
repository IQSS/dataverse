/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
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

    private Map<Long, Map<Class<? extends Command>, Boolean>> commandMap = new HashMap<>();

    /**
     * Check if the current Dataset can Issue Commands
     *
     * @param commandName
     */
    private boolean canIssueCommand(DvObject dvo, Class<? extends Command> command) {
        if ((dvo == null) || (command == null)) {
            return false;
        }

        if (commandMap.containsKey(dvo.getId())) {
            Map<Class<? extends Command>, Boolean> dvoCommandMap = this.commandMap.get(dvo.getId());
            if (dvoCommandMap.containsKey(command)) {
                return dvoCommandMap.get(command);
            } else {
                return addCommandtoDvoCommandMap(dvo, command, dvoCommandMap);
            }

        } else {
            Map newDvoCommandMap = new HashMap();
            commandMap.put(dvo.getId(), newDvoCommandMap);
            return addCommandtoDvoCommandMap(dvo, command, newDvoCommandMap);
        }
    }

    private boolean addCommandtoDvoCommandMap(DvObject dvo, Class<? extends Command> command, Map<Class<? extends Command>, Boolean> dvoCommandMap) {
        boolean canIssueCommand;
        canIssueCommand = permissionService.on(dvo).canIssue(command);
        dvoCommandMap.put(command, canIssueCommand);
        return canIssueCommand;
    }

    public boolean canManageDataversePermissions(User u, Dataverse dv) {
        return permissionService.userOn(u, dv).has(Permission.ManageDataversePermissions);
    }

    public boolean CanIssueUpdateDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, UpdateDataverseCommand.class);
    }

    public boolean CanIssuePublishDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, PublishDataverseCommand.class);
    }

    public boolean CanIssueDeleteDataverseCommand(DvObject dvo) {
        return canIssueCommand(dvo, DeleteDataverseCommand.class);
    }

    public boolean canManagePermissions(DvObject dvo) {
        User u = session.getUser();
        return dvo instanceof Dataverse
                ? canManageDataversePermissions(u, (Dataverse) dvo)
                : canManageDatasetPermissions(u, (Dataset) dvo);
    }

    public boolean canManageDatasetPermissions(User u, Dataset ds) {
        return permissionService.userOn(u, ds).has(Permission.ManageDatasetPermissions);
    }

}
