package edu.harvard.iq.dataverse.authorization.groups.impl.saml;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;
import edu.harvard.iq.dataverse.persistence.group.SamlGroupRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
public class SamlGroupService {
    private static final Logger logger = LoggerFactory.getLogger(SamlGroupService.class);

    private RoleAssigneeServiceBean roleAssigneeSvc;

    private ActionLogServiceBean actionLogSvc;

    private SamlGroupRepository repository;

    // -------------------- CONSTRUCTORS --------------------

    public SamlGroupService() { }

    @Inject
    public SamlGroupService(RoleAssigneeServiceBean roleAssigneeSvc, ActionLogServiceBean actionLogSvc, SamlGroupRepository repository) {
        this.roleAssigneeSvc = roleAssigneeSvc;
        this.actionLogSvc = actionLogSvc;
        this.repository = repository;
    }

    // -------------------- LOGIC --------------------

    public List<SamlGroup> findAll() {
        return repository.findAll();
    }

    public SamlGroup findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public SamlGroup save(String name, String entityId) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "samlCreate");
        alr.setInfo(name + ": " + entityId);

        SamlGroup saved = repository.saveAndFlush(new SamlGroup(name, entityId));
        actionLogSvc.log(alr);
        return saved;
    }

    public Set<SamlGroup> findFor(AuthenticatedUser authenticatedUser) {
        Set<SamlGroup> groupsForUser = new HashSet<>();
        String samlIdPEntityId = authenticatedUser.getSamlIdPEntityId();
        logger.debug("IdP for user " + authenticatedUser.getIdentifier() + " is " + samlIdPEntityId);
        if (samlIdPEntityId != null) {
            groupsForUser.addAll(repository.findByEntityId(samlIdPEntityId));
        }
        return groupsForUser;
    }

    public void delete(SamlGroup doomed) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "samlDelete");
        alr.setInfo(doomed.getName() + ":" + doomed.getIdentifier());

        List<RoleAssignment> assignments = roleAssigneeSvc.getAssignmentsFor(doomed.getIdentifier());
        if (assignments.isEmpty()) {
            repository.mergeAndDelete(doomed);
            actionLogSvc.log(alr);
        } else {
            List<String> assignmentIds = new ArrayList<>();
            for (RoleAssignment assignment : assignments) {
                assignmentIds.add(assignment.getId().toString());
            }
            String message = String.format("Could not delete Saml group with id [%d] due to existing role assignments: %s",
                    doomed.getId(), assignmentIds);
            logger.info(message);
            actionLogSvc.log(alr.setActionResult(ActionLogRecord.Result.BadRequest)
                                     .setInfo(alr.getInfo() + "// " + message));
            throw new IllegalStateException(message);
        }
    }
}
