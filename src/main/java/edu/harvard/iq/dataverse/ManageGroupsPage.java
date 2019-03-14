package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang.StringUtils;

/**
 * @author michaelsuo
 */
@ViewScoped
@Named
public class ManageGroupsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManageGroupsPage.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    ExplicitGroupServiceBean explicitGroupService;
    @EJB
    EjbDataverseEngine engineService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    GroupServiceBean groupService;
    @Inject
    DataverseRequestServiceBean dvRequestService;

    @Inject
    PermissionsWrapper permissionsWrapper;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataversePage dvpage;

    @Inject
    GuestbookPage guestbookPage;

    @Inject
    DataverseSession session;

    private List<ExplicitGroup> explicitGroups;
    private Dataverse dataverse;
    private Long dataverseId;
    private ExplicitGroup selectedGroup = null;

    public String init() {
        setDataverse(dataverseService.find(getDataverseId()));
        Dataverse editDv = getDataverse();
        dvpage.setDataverse(editDv);

        if (editDv == null) {
            return permissionsWrapper.notFound();
        }

        Boolean hasPermissions = permissionsWrapper.canIssueCommand(editDv, CreateExplicitGroupCommand.class);
        hasPermissions |= permissionsWrapper.canIssueCommand(editDv, DeleteExplicitGroupCommand.class);
        hasPermissions |= permissionsWrapper.canIssueCommand(editDv, UpdateExplicitGroupCommand.class);
        if (!hasPermissions) {
            return permissionsWrapper.notAuthorized();
        }
        explicitGroups = new LinkedList<>(explicitGroupService.findByOwner(getDataverseId()));

        return null;
    }


    public void setSelectedGroup(ExplicitGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    public List<ExplicitGroup> getExplicitGroups() {
        return explicitGroups;
    }

    public void setExplicitGroups(List<ExplicitGroup> explicitGroups) {
        this.explicitGroups = explicitGroups;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Group getSelectedGroup() {
        return selectedGroup;
    }

    public void deleteGroup() {
        if (selectedGroup != null) {
            explicitGroups.remove(selectedGroup);
            try {
                engineService.submit(new DeleteExplicitGroupCommand(dvRequestService.getDataverseRequest(), selectedGroup));
                JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("dataverse.manageGroups.delete"));
            } catch (CommandException ex) {
                String failMessage = BundleUtil.getStringFromBundle("dataverse.manageGroups.nodelete");
                JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            }
        } else {
            logger.info("Selected group is null");
        }
    }

    private List<RoleAssignee> selectedGroupRoleAssignees = new ArrayList<>();

    public void setSelectedGroupRoleAssignees(List<RoleAssignee> newSelectedGroupRoleAssignees) {
        this.selectedGroupRoleAssignees = newSelectedGroupRoleAssignees;
    }

    public List<RoleAssignee> getSelectedGroupRoleAssignees() {
        return this.selectedGroupRoleAssignees;
    }

    private List<RoleAssignee> selectedGroupAddRoleAssignees;

    public void setSelectedGroupAddRoleAssignees(List<RoleAssignee> ras) {
        this.selectedGroupAddRoleAssignees = ras;
    }

    public List<RoleAssignee> getSelectedGroupAddRoleAssignees() {
        return this.selectedGroupAddRoleAssignees;
    }

    public void viewSelectedGroup(ExplicitGroup selectedGroup) {
        this.selectedGroup = selectedGroup;

        // initialize member list for autocomplete interface
        setSelectedGroupAddRoleAssignees(new LinkedList<>());
        setSelectedGroupRoleAssignees(getExplicitGroupMembers(selectedGroup));
    }

    /**
     * Return the set of all role assignees for an explicit group.
     * Does not traverse subgroups.
     * @param eg The explicit group to check.
     * @return The set of role assignees belonging to explicit group.
     */
    public List<RoleAssignee> getExplicitGroupMembers(ExplicitGroup eg) {
        return (eg != null) ?
                new ArrayList<>(eg.getDirectMembers()) : null;
    }

    /**
     * Return a string describing the type of a role assignee
     * TODO reference the bundle for localization
     * @param ra The role assignee
     * @return A {@code String} representing the role assignee's type.
     */
    public String getRoleAssigneeTypeString(RoleAssignee ra) {
        if (ra instanceof User) {
            return BundleUtil.getStringFromBundle("dataverse.manageGroups.User");
        } else if (ra instanceof Group) {
            return BundleUtil.getStringFromBundle("dataverse.manageGroups.Group");
        } else {
            return BundleUtil.getStringFromBundle("dataverse.manageGroups.unknown");
        }
    }

    public String getMembershipString(ExplicitGroup eg) {
        long userCount = 0;
        long groupCount = 0;
        for ( RoleAssignee ra : eg.getDirectMembers() ) {
            if ( ra instanceof User ) {
                userCount++;
            } else {
                groupCount++;
            }
        }

        if (userCount == 0 && groupCount == 0) {
            return BundleUtil.getStringFromBundle("dataverse.manageGroups.nomembers");
        }

        String memberString = "";
        if (userCount == 1) {
            memberString = "1 "+BundleUtil.getStringFromBundle("dataverse.manageGroups.user");
        } else if (userCount != 1) {
            memberString = Long.toString(userCount) + " "+BundleUtil.getStringFromBundle("dataverse.manageGroups.users");
        }

        if (groupCount == 1) {
            memberString = memberString + ", 1 " + BundleUtil.getStringFromBundle("dataverse.manageGroups.group");
        } else if (groupCount != 1) {
            memberString = memberString + ", " + Long.toString(groupCount) + " " + BundleUtil.getStringFromBundle("dataverse.manageGroups.groups");
        }

        return memberString;
    }

    public void removeMemberFromSelectedGroup(RoleAssignee ra) {
        selectedGroup.remove(ra);
    }

    public List<RoleAssignee> completeRoleAssignee( String query ) {

        List<RoleAssignee> alreadyAssignedRoleAssignees = new ArrayList<>();

        if (this.getNewExplicitGroupRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getNewExplicitGroupRoleAssignees());
        }
        if (this.getSelectedGroupRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getSelectedGroupRoleAssignees());
        }
        if (this.getSelectedGroupAddRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getSelectedGroupAddRoleAssignees());
        }

        return roleAssigneeService.filterRoleAssignees(query, dataverse, alreadyAssignedRoleAssignees);

    }

    /*
    ============================================================================
    Explicit Group dialogs
    ============================================================================
    */

    String explicitGroupIdentifier = "";
    String explicitGroupName = "";
    String newExplicitGroupDescription = "";
    UIInput explicitGroupIdentifierField;

    List<RoleAssignee> newExplicitGroupRoleAssignees = new LinkedList<>();

    public void initExplicitGroupDialog(ActionEvent ae) {
        showNoMessages();
        setExplicitGroupName("");
        setExplicitGroupIdentifier("");
        setNewExplicitGroupDescription("");
        setNewExplicitGroupRoleAssignees(new LinkedList<>());
        FacesContext context = FacesContext.getCurrentInstance();
        setSelectedGroupRoleAssignees(null);
    }

    public void createExplicitGroup(ActionEvent ae) {

        ExplicitGroup eg = explicitGroupService.getProvider().makeGroup();
        eg.setDisplayName( getExplicitGroupName() );
        eg.setGroupAliasInOwner( getExplicitGroupIdentifier() );
        eg.setDescription( getNewExplicitGroupDescription() );

        if ( getNewExplicitGroupRoleAssignees()!= null ) {
            try {
                for ( RoleAssignee ra : getNewExplicitGroupRoleAssignees() ) {
                    eg.add( ra );
                }
            } catch ( GroupException ge ) {
                JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,
                        BundleUtil.getStringFromBundle("dataverse.manageGroups.create.fail"),
                        ge.getMessage());
                return;
            }
        }
        try {
            eg = engineService.submit( new CreateExplicitGroupCommand(dvRequestService.getDataverseRequest(), this.dataverse, eg));
            explicitGroups.add(eg);
            List<String> args = Arrays.asList(eg.getDisplayName());
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.manageGroups.create.success", args));

        } catch ( CreateExplicitGroupCommand.GroupAliasExistsException gaee ) {
            explicitGroupIdentifierField.setValid( false );
            FacesContext.getCurrentInstance().addMessage(explicitGroupIdentifierField.getClientId(),
                    new FacesMessage( FacesMessage.SEVERITY_ERROR, gaee.getMessage(), null));

        } catch (CommandException ex) {
            logger.log(Level.WARNING, "Group creation failed", ex);
            JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("dataverse.manageGroups.create.fail"),
                    ex.getMessage());
        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotSaved"));
            logger.log(Level.SEVERE, "Error saving role: " + ex.getMessage(), ex);
        }
        showAssignmentMessages();
    }

    public void saveExplicitGroup(ActionEvent ae) {
        ExplicitGroup eg = selectedGroup;

        if ( getSelectedGroupAddRoleAssignees() != null ) {
            try {
                for ( RoleAssignee ra : getSelectedGroupAddRoleAssignees() ) {
                    eg.add( ra );
                }
            } catch ( GroupException ge ) {
                JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,
                        BundleUtil.getStringFromBundle("dataverse.manageGroups.edit.fail"),
                        ge.getMessage());
                return;
            }
        }

        try {
            eg = engineService.submit( new UpdateExplicitGroupCommand(dvRequestService.getDataverseRequest(), eg));
            List<String> args = Arrays.asList(eg.getDisplayName());
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.manageGroups.save.success", args));

        } catch (CommandException ex) {
            JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,BundleUtil.getStringFromBundle("dataverse.manageGroups.save.fail"),
                    ex.getMessage());
        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotSaved"));
            logger.log(Level.SEVERE, "Error saving role: " + ex.getMessage(), ex);
        }
        showAssignmentMessages();
    }

    public void setExplicitGroupName(String explicitGroupFriendlyName) {
        this.explicitGroupName = explicitGroupFriendlyName;
    }

    public String getExplicitGroupName() {
        return explicitGroupName;
    }

    public void setExplicitGroupIdentifier(String explicitGroupName) {
        this.explicitGroupIdentifier = explicitGroupName;
    }

    public String getExplicitGroupIdentifier() {
        return explicitGroupIdentifier;
    }

    public UIInput getExplicitGroupIdentifierField() {
        return explicitGroupIdentifierField;
    }

    public void setExplicitGroupIdentifierField(UIInput explicitGroupIdentifierField) {
        this.explicitGroupIdentifierField = explicitGroupIdentifierField;
    }

    public void validateGroupIdentifier(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); // Optimistic approach

        if ( context.getExternalContext().getRequestParameterMap().get("DO_GROUP_VALIDATION") != null
                && !StringUtils.isEmpty(value) ) {

            // cheap test - regex
            if (! Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", value) ) {
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("dataverse.permissions.explicitGroupEditDialog.groupIdentifier.invalid")));

            } else if ( explicitGroupService.findInOwner(dataverse.getId(), value) != null ) {
                // Ok, see that the alias is not taken
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("dataverse.permissions.explicitGroupEditDialog.groupIdentifier.taken")));
            }
        }
    }

    public void setNewExplicitGroupRoleAssignees(List<RoleAssignee> newExplicitGroupRoleAssignees) {
        this.newExplicitGroupRoleAssignees = newExplicitGroupRoleAssignees;
    }

    public List<RoleAssignee> getNewExplicitGroupRoleAssignees() {
        return newExplicitGroupRoleAssignees;
    }

    public String getNewExplicitGroupDescription() {
        return newExplicitGroupDescription;
    }

    public void setNewExplicitGroupDescription(String newExplicitGroupDescription) {
        this.newExplicitGroupDescription = newExplicitGroupDescription;
    }


    /*
    ============================================================================
    Internal methods
    ============================================================================
    */

    boolean renderConfigureMessages = false;
    boolean renderAssignmentMessages = false;
    boolean renderRoleMessages = false;

    private void showNoMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = false;
        renderRoleMessages = false;
    }

    private void showAssignmentMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = true;
        renderRoleMessages = false;
    }
}
