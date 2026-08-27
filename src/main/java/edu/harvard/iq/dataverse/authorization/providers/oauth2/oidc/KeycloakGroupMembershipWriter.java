package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes membership changes for one Keycloak-managed group, each in its own transaction.
 * <p>
 * The full reconciliation sweep touches every tenant, and a single failure -- a stale entity,
 * a constraint, a lock -- must not roll back the groups already reconciled. Giving each group
 * its own transaction keeps a bad tenant from poisoning the good ones, so a sweep makes
 * partial progress instead of none.
 */
@Stateless
public class KeycloakGroupMembershipWriter {

    private static final Logger logger = Logger.getLogger(KeycloakGroupMembershipWriter.class.getName());

    @EJB
    ExplicitGroupServiceBean explicitGroupService;

    @EJB
    RoleAssigneeServiceBean roleAssigneeService;

    /**
     * Apply one group's membership delta.
     *
     * @param groupAlias the group's installation-wide alias, e.g. {@code 2-kc-admins}
     * @param toRemove   role assignee identifiers to drop
     * @param toAdd      role assignee identifiers to add
     * @return true when the change was written, false when it failed and was skipped
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean apply(String groupAlias, Set<String> toRemove, Set<String> toAdd) {
        try {
            ExplicitGroup group = explicitGroupService.getProvider().get(groupAlias);
            if (group == null) {
                logger.warning("Keycloak reconciliation: group " + groupAlias + " no longer exists; skipping.");
                return false;
            }

            toRemove.forEach(group::removeByRoleAssgineeIdentifier);

            for (String identifier : toAdd) {
                RoleAssignee assignee = roleAssigneeService.getRoleAssignee(identifier);
                if (assignee == null) {
                    logger.warning("Keycloak reconciliation: no role assignee " + identifier
                            + "; not adding it to " + groupAlias + ".");
                    continue;
                }
                try {
                    group.add(assignee);
                } catch (GroupException ex) {
                    logger.log(Level.WARNING, "Keycloak reconciliation: could not add " + identifier
                            + " to " + groupAlias, ex);
                }
            }

            explicitGroupService.persist(group);
            if (!toRemove.isEmpty()) {
                logger.info("Keycloak reconciliation: removed " + toRemove + " from " + groupAlias);
            }
            if (!toAdd.isEmpty()) {
                logger.info("Keycloak reconciliation: added " + toAdd + " to " + groupAlias);
            }
            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Keycloak reconciliation: failed to update " + groupAlias
                    + "; the rest of the sweep continues.", ex);
            return false;
        }
    }
}
