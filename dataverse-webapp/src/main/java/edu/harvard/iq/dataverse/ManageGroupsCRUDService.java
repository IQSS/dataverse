package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import io.vavr.control.Try;
import org.apache.commons.collections4.CollectionUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ManageGroupsCRUDService {

    private EjbDataverseEngine engineService;
    private DataverseRequestServiceBean dvRequestService;
    private ExplicitGroupServiceBean explicitGroupService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageGroupsCRUDService() {
    }

    @Inject
    public ManageGroupsCRUDService(EjbDataverseEngine engineService,
                                   DataverseRequestServiceBean dvRequestService,
                                   ExplicitGroupServiceBean explicitGroupService) {
        this.engineService = engineService;
        this.dvRequestService = dvRequestService;
        this.explicitGroupService = explicitGroupService;
    }

    // -------------------- LOGIC --------------------

    public Try<ExplicitGroup> create(Dataverse dataverse, String explicitGroupName, String explicitGroupIdentifier, String explicitGroupDescription,
                                                              List<RoleAssignee> explicitGroupRoleAssignees) {
        ExplicitGroup explicitGroup = explicitGroupService.getProvider().makeGroup();
        explicitGroup.setDisplayName(explicitGroupName);
        explicitGroup.setGroupAliasInOwner(explicitGroupIdentifier);
        explicitGroup.setDescription(explicitGroupDescription);

        Try<ExplicitGroup> explicitGroupTry = addRoleAssigneesToGroup(explicitGroup, explicitGroupRoleAssignees);

        return Try.of(() -> engineService.submit(new CreateExplicitGroupCommand(dvRequestService.getDataverseRequest(), dataverse, explicitGroupTry.get())))
                .onFailure(throwable -> Logger.getLogger(ManageGroupsCRUDService.class.getCanonicalName()).log(Level.SEVERE, null, throwable))
                ;

    }

    public Try<ExplicitGroup> update(ExplicitGroup group, List<RoleAssignee> newRoleAssignees) {

        Try<ExplicitGroup> explicitGroupTry = addRoleAssigneesToGroup(group, newRoleAssignees);
        return Try.of(() -> engineService.submit(new UpdateExplicitGroupCommand(dvRequestService.getDataverseRequest(), explicitGroupTry.get())))
                .onFailure(throwable -> Logger.getLogger(ManageGroupsCRUDService.class.getCanonicalName()).log(Level.SEVERE, null, throwable))
                ;
    }

    public void delete(ExplicitGroup explicitGroup) throws CommandException {
        engineService.submit(new DeleteExplicitGroupCommand(dvRequestService.getDataverseRequest(), explicitGroup));
    }

    // -------------------- LOGIC --------------------

    private Try<ExplicitGroup> addRoleAssigneesToGroup(ExplicitGroup explicitGroup,
                                                       List<RoleAssignee> explicitGroupRoleAssignees) {
        return Try.of(() -> {
            if(CollectionUtils.isNotEmpty(explicitGroupRoleAssignees)) {
                for (RoleAssignee ra : explicitGroupRoleAssignees) {
                    explicitGroup.add(ra);
                }
            }
            return explicitGroup;
        })
                .onFailure(throwable -> Logger.getLogger(ManageGroupsCRUDService.class.getCanonicalName()).log(Level.SEVERE, null, throwable))
                ;
    }
}
