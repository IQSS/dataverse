package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;

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

    protected Map<String, RoleAssignee> predefinedRoleAssignees = new TreeMap<>();

    @PostConstruct
    protected void setup() {
        GuestUser gu = GuestUser.get();
        predefinedRoleAssignees.put(gu.getIdentifier(), gu);
        predefinedRoleAssignees.put(AuthenticatedUsers.get().getIdentifier(), AuthenticatedUsers.get());
        predefinedRoleAssignees.put(AllUsers.get().getIdentifier(), AllUsers.get());
    }

    /**
     * @param identifier An identifier beginning with ":" (builtin), "@"
     * ({@link AuthenticatedUser}), "&" ({@link Group}), or "#"
     * ({@link PrivateUrlUser}).
     *
     * @return A RoleAssignee (User or Group) or null.
     *
     * @throws IllegalArgumentException if you pass null, empty string, or an
     * identifier that doesn't start with one of the supported characters.
     */
    public RoleAssignee getRoleAssignee(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty string.");
        }
        return (getRoleAssignee(identifier, false));
    }
    
    /**
     * @param identifier An identifier beginning with ":" (builtin), "@"
     * @param augmented boolean to decide whether to get provider information
     * ({@link AuthenticatedUser}), "&" ({@link Group}), or "#"
     * ({@link PrivateUrlUser}).
     *
     * @return A RoleAssignee (User or Group) or null.
     *
     * @throws IllegalArgumentException if you pass null, empty string, or an
     * identifier that doesn't start with one of the supported characters.
     */
    public RoleAssignee getRoleAssignee(String identifier, Boolean augmented) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty string.");
        }
        switch (identifier.substring(0,1)) {
            case ":":
                return predefinedRoleAssignees.get(identifier);
            case AuthenticatedUser.IDENTIFIER_PREFIX:
                if (!augmented){
                    return authSvc.getAuthenticatedUser(identifier.substring(1));
                } else {
                    return authSvc.getAuthenticatedUserWithProvider(identifier.substring(1));
                }
            case Group.IDENTIFIER_PREFIX:
                return groupSvc.getGroup(identifier.substring(1));
            case PrivateUrlUser.PREFIX:
                return PrivateUrlUtil.identifier2roleAssignee(identifier);
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
        List<AuthenticatedUser> explicitUsers = new ArrayList<>();
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

    /**
     * Retrieves the list of {@link DataverseRole}s relevant for the given {@link DataverseRequest}.
     * <p>
     * - If the user is a superuser, all roles are returned.<br>
     * - If the user is not a superuser, their assigned roles are returned. If none are assigned,
     *   all roles are returned as a fallback.
     * <p>
     * This method is based on the logic from {@code MyDataPage.getRolesUsedToCreateCheckboxes}.
     * It has been implemented in this service to make the logic reusable from parts of the application
     * other than the UI.
     *
     * @param request the dataverse request containing user context
     * @return a list of relevant {@link DataverseRole}s for the user
     * @throws NullPointerException if the request is null
     */
    public List<DataverseRole> getSuperuserOrAssigneeDataverseRolesFor(DataverseRequest request) {
        if (request == null) {
            throw new NullPointerException("DataverseRequest cannot be null.");
        }

        User user = request.getUser();

        if (user.isSuperuser()) {
            return dataverseRoleService.findAll();
        }

        List<DataverseRole> assignedRoles = getAssigneeDataverseRoleFor(request);

        return assignedRoles.isEmpty() ? dataverseRoleService.findAll() : assignedRoles;
    }

    public List<DataverseRole> getAssigneeDataverseRoleFor(DataverseRequest dataverseRequest) {
        
        if (dataverseRequest == null){
            throw new NullPointerException("dataverseRequest cannot be null!");
        }
        AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null){
            return null;
        }
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();

        List<DataverseRole> retList = new ArrayList<>();
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(au);
        List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
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

    public List<Object[]> getAssigneeAndRoleIdListFor(MyDataFilterParams filterParams) {
         
        if (filterParams == null){
            throw new NullPointerException("Cannot be null! filterParams must be an instance of MyDataFilterParams");
        }
        
        AuthenticatedUser au = filterParams.getAuthenticatedUser();
        List<Long> roleIdList = filterParams.getRoleIds();
                        //filterParams.getAuthenticatedUser()
                //                       , this.filterParams.getRoleIds());
        
        if (au.getUserIdentifier() == null) {
            return null;
        }
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();
        
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userExplicitGroups = getUserExplicitGroups(au);
        List<String> userRunTimeGroups = getUserRuntimeGroups(filterParams.getDataverseRequest());
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
        return em.createNativeQuery(qstr).getResultList();

    }

    public List<Long> getRoleIdListForGivenAssigneeDvObject(DataverseRequest dataverseRequest, List<Long> roleIdList, Long defPointId) {
        if (dataverseRequest == null){
            throw new NullPointerException("dataverseRequest cannot be null!");
        }
        AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null){
            return null;
        }
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();
        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(au);
        List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
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

        return em.createNativeQuery(qstr).getResultList();

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

    public List<Object[]> getRoleIdsFor(DataverseRequest dataverseRequest, List<Long> dvObjectIdList) {
        if (dataverseRequest == null){
            throw new NullPointerException("dataverseRequest cannot be null!");
        }
        AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null){
            return null;
        }
        String roleAssigneeIdentifier = "@" + au.getUserIdentifier();

        roleAssigneeIdentifier = roleAssigneeIdentifier.replaceAll("\\s", "");   // remove spaces from string
        List<String> userGroups = getUserExplicitGroups(au);
        List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
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

        return em.createNativeQuery(qstr).getResultList();

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
     * @param ra
     * @todo Support groups within groups: https://github.com/IQSS/dataverse/issues/3056
     * @return List of aliases of all explicit groups {@code ra} is in.
     */
    public List<String> getUserExplicitGroups(RoleAssignee ra) {
        return explicitGroupSvc.findGroups(ra).stream()
                               .map( g -> g.getAlias())
                               .collect(Collectors.toList());
    }

    private List<String> getUserRuntimeGroups(DataverseRequest dataverseRequest) {
        List<String> retVal = new ArrayList<>();

        //Set<Group> groups = groupSvc.groupsFor(dataverseRequest, null);
        Set<Group> groups = groupSvc.collectAncestors(groupSvc.groupsFor(dataverseRequest));
        for (Group group : groups) {
            logger.fine("found group " + group.getIdentifier() + " with alias " + group.getAlias());
           // if (group.getGroupProvider().getGroupProviderAlias().equals("shib") || group.getGroupProvider().getGroupProviderAlias().equals("ip")) {
                String groupAlias = group.getAlias();
                if (groupAlias != null && !groupAlias.isEmpty()) {
                    if( group instanceof ExplicitGroup){
                        retVal.add("&explicit/" + groupAlias);
                    } else{
                        retVal.add('&' + groupAlias);
                    }
                }
            //}
        }
        logger.fine("retVal: " + retVal);
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
                    if (!ra.isDeactivated()) {
                        roleAssigneeList.add(ra);
                    }
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
    

    public List<String> findAssigneesWithPermissionOnDvObject(Long objectId, Permission permission) {
        int bitpos = 63 - permission.ordinal();
        return em.createNamedQuery("RoleAssignment.findAssigneesWithPermissionOnDvObject", String.class)
                 .setParameter(1, bitpos)
                 .setParameter(2, objectId)
                 .getResultList();
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
