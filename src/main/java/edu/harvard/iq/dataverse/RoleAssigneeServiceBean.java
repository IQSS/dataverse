package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang.StringUtils;

/**
 * The place to obtain {@link RoleAssignee}s, based on their identifiers.
 *
 * @author michael
 */
@Stateless
@Named
public class RoleAssigneeServiceBean {

    private static final Logger logger = Logger.getLogger(RoleAssigneeServiceBean.class.getName());
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    AuthenticationServiceBean authSvc;

    @EJB
    GroupServiceBean groupSvc;

    @EJB
    ExplicitGroupServiceBean explicitGroupSvc;

    @EJB
    DataverseRoleServiceBean dataverseRoleService;

    Map<String, RoleAssignee> predefinedRoleAssignees = new TreeMap<>();

    @PostConstruct
    void setup() {
        GuestUser gu = GuestUser.get();
        predefinedRoleAssignees.put(gu.getIdentifier(), gu);
        predefinedRoleAssignees.put(AuthenticatedUsers.get().getIdentifier(), AuthenticatedUsers.get());
        predefinedRoleAssignees.put(AllUsers.get().getIdentifier(), AllUsers.get());
    }

    public RoleAssignee getRoleAssignee(String identifier) {
        switch (identifier.charAt(0)) {
            case ':':
                return predefinedRoleAssignees.get(identifier);
            case '@':
                return authSvc.getAuthenticatedUser(identifier.substring(1));
            case '&':
                return groupSvc.getGroup(identifier.substring(1));
            default:
                throw new IllegalArgumentException("Unsupported assignee identifier '" + identifier + "'");
        }
    }

    public List<RoleAssignment> getAssignmentsFor(String roleAssigneeIdentifier) {
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier", RoleAssignment.class)
                .setParameter("assigneeIdentifier", roleAssigneeIdentifier)
                .getResultList();
    }

    public List<AuthenticatedUser> getExplicitUsers(RoleAssignee ra) {
        List<AuthenticatedUser> explicitUsers = new ArrayList();
        if (ra instanceof AuthenticatedUser) {
            explicitUsers.add((AuthenticatedUser) ra);
        } else if (ra instanceof ExplicitGroup) {
            ExplicitGroup group = (ExplicitGroup) ra;
            for (String raIdentifier : group.getContainedRoleAssgineeIdentifiers()) {
                explicitUsers.addAll(getExplicitUsers(getRoleAssignee(raIdentifier)));
            }
        }

        return explicitUsers;
    }

    private String getRoleIdListClause(List<Long> roleIdList) {
        if (roleIdList == null) {
            return "";
        }
        List<String> outputList = new ArrayList<>();

        for (Long r : roleIdList) {
            if (r != null) {
                outputList.add(r.toString());
            }
        }
        if (outputList.isEmpty()) {
            return "";
        }
        return " AND r.role_id IN (" + StringUtils.join(outputList, ",") + ")";
    }

    public List<DataverseRole> getAssigneeDataverseRoleFor(AuthenticatedUser au) {
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();
        if (roleAssigneeIdentifier == null) {
            return null;
        }
        List<DataverseRole> retList = new ArrayList();
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(roleAssigneeIdentifier.replace("@", ""));
        List<String> userRunTimeGroups = getUserRuntimeGroups(au);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {

            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, userGroups, userRunTimeGroups);
        }

        String qstr = "SELECT distinct r.role_id";
        qstr += " FROM RoleAssignment r";
        qstr += identifierClause;
        qstr += ";";
        msg("qstr: " + qstr);

        for (Object o : em.createNativeQuery(qstr).getResultList()) {
            retList.add(dataverseRoleService.find((Long) o));
        }
        return retList;
    }

    public List<Object[]> getAssigneeAndRoleIdListFor(AuthenticatedUser au, List<Long> roleIdList) {

        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();

        if (roleAssigneeIdentifier == null) {
            return null;
        }
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userExplicitGroups = getUserExplicitGroups(roleAssigneeIdentifier.replace("@", ""));
        List<String> userRunTimeGroups = getUserRuntimeGroups(au);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userExplicitGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, userExplicitGroups, userRunTimeGroups);
        }

        String qstr = "SELECT r.definitionpoint_id, r.role_id";
        qstr += " FROM RoleAssignment r";
        qstr += identifierClause;
        qstr += getRoleIdListClause(roleIdList);
        qstr += ";";
        msg("qstr: " + qstr);
        return em.createNativeQuery(qstr)
                .getResultList();

    }

    public List<Long> getRoleIdListForGivenAssigneeDvObject(AuthenticatedUser au, List<Long> roleIdList, Long defPointId) {
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();
        if (roleAssigneeIdentifier == null) {
            return null;
        }
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(roleAssigneeIdentifier.replace("@", ""));
        List<String> userRunTimeGroups = getUserRuntimeGroups(au);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, userGroups, userRunTimeGroups);
        }

        String qstr = "SELECT r.role_id";
        qstr += " FROM RoleAssignment r";
        qstr += identifierClause;
        qstr += getRoleIdListClause(roleIdList);
        qstr += " and r.definitionpoint_id = " + defPointId;
        qstr += ";";
        msg("qstr: " + qstr);

        return em.createNativeQuery(qstr)
                .getResultList();

    }

    private String getGroupIdentifierClause(String roleAssigneeIdentifier, List<String> userExplicitGroups, List<String> userRunTimeGroups) {

        if (userExplicitGroups == null && userRunTimeGroups == null) {
            return "";
        }
        List<String> outputExplicitList = new ArrayList<>();
        String explicitString = "";

        if (userExplicitGroups != null) {
            for (String r : userExplicitGroups) {
                if (r != null) {
                    outputExplicitList.add(r);
                }
            }

            if (!outputExplicitList.isEmpty()) {
                explicitString = ",'&explicit/" + StringUtils.join(outputExplicitList, "','&explicit/") + "'";
            }

        }

        List<String> outputRuntimeList = new ArrayList<>();
        String runTimeString = "";

        if (userRunTimeGroups != null) {
            for (String r : userRunTimeGroups) {
                if (r != null) {
                    outputRuntimeList.add(r);
                }
            }

            if (!outputRuntimeList.isEmpty()) {
                runTimeString = ",'" + StringUtils.join(outputRuntimeList, "','") + "'";
            }

        }
        return " WHERE r.assigneeIdentifier in ( '" + roleAssigneeIdentifier + "'" + explicitString + runTimeString + ")";

    }

    public List<Object[]> getRoleIdsFor(AuthenticatedUser au, List<Long> dvObjectIdList) {
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();
        if (roleAssigneeIdentifier == null) {
            return null;
        }

        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(roleAssigneeIdentifier.replace("@", ""));
        List<String> userRunTimeGroups = getUserRuntimeGroups(au);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, userGroups, userRunTimeGroups);
        }

        String qstr = "SELECT r.definitionpoint_id, r.role_id";
        qstr += " FROM RoleAssignment r";
        qstr += identifierClause;
        qstr += getDvObjectIdListClause(dvObjectIdList);
        qstr += ";";
        //msg("qstr: " + qstr);

        return em.createNativeQuery(qstr)
                .getResultList();

    }

    private String getDvObjectIdListClause(List<Long> dvObjectIdList) {
        if (dvObjectIdList == null) {
            return "";
        }
        List<String> outputList = new ArrayList<>();

        for (Long r : dvObjectIdList) {
            if (r != null) {
                outputList.add(r.toString());
            }
        }
        if (outputList.isEmpty()) {
            return "";
        }
        return " AND r.definitionpoint_id IN (" + StringUtils.join(outputList, ",") + ")";
    }

    /**
     * @todo Support groups within groups:
     * https://github.com/IQSS/dataverse/issues/3056
     */
    public List<String> getUserExplicitGroups(String roleAssigneeIdentifier) {

        String qstr = "select  groupalias from explicitgroup";
        qstr += " where id in ";
        qstr += " (select explicitgroup_id from explicitgroup_authenticateduser where containedauthenticatedusers_id = ";
        qstr += " (select id from authenticateduser where useridentifier ='" + roleAssigneeIdentifier + "'";
        qstr += "));";
        //msg("qstr: " + qstr);

        return em.createNativeQuery(qstr)
                .getResultList();
    }

    private List<String> getUserRuntimeGroups(AuthenticatedUser au) {
        List<String> retVal = new ArrayList();

        Set<Group> groups = groupSvc.groupsFor(au, null);
        StringBuilder sb = new StringBuilder();
        for (Group group : groups) {
            logger.fine("found group " + group.getIdentifier() + " with alias " + group.getAlias());
            if (group.getGroupProvider().getGroupProviderAlias().equals("shib") || group.getGroupProvider().getGroupProviderAlias().equals("ip")) {
                String groupAlias = group.getAlias();
                if (groupAlias != null && !groupAlias.isEmpty()) {
                    retVal.add('&' + groupAlias);
                }
            }
        }
        return retVal;
    }

    public List<RoleAssignee> filterRoleAssignees(String query, DvObject dvObject, List<RoleAssignee> roleAssignSelectedRoleAssignees) {
        List<RoleAssignee> roleAssigneeList = new ArrayList<>();

        // we get the users through a query that does the filtering through the db,
        // so that we don't have to instantiate all of the RoleAssignee objects
        em.createNamedQuery("AuthenticatedUser.filter", AuthenticatedUser.class)
                .setParameter("query", "%" + query + "%")
                .getResultList().stream()
                .filter(ra -> roleAssignSelectedRoleAssignees == null || !roleAssignSelectedRoleAssignees.contains(ra))
                .forEach((ra) -> {
                    roleAssigneeList.add(ra);
                });

        // now we add groups to the list, both global and explicit
        Set<Group> groups = groupSvc.findGlobalGroups();
        groups.addAll(explicitGroupSvc.findAvailableFor(dvObject));
        groups.stream()
                .filter(ra -> StringUtils.containsIgnoreCase(ra.getDisplayInfo().getTitle(), query)
                        || StringUtils.containsIgnoreCase(ra.getIdentifier(), query))
                .filter(ra -> roleAssignSelectedRoleAssignees == null || !roleAssignSelectedRoleAssignees.contains(ra))
                .forEach((ra) -> {
                    roleAssigneeList.add(ra);
                });

        return roleAssigneeList;
    }

    private void msg(String s) {
        //System.out.println(s);
    }

    private void msgt(String s) {
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }

}
