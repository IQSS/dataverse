package edu.harvard.iq.dataverse.search.query;

import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class PermissionFilterQueryBuilder {

    private static final Logger logger = Logger.getLogger(SearchServiceBean.class.getCanonicalName());
    
    private GroupServiceBean groupService;

    // -------------------- CONSTRUCTORS --------------------
    
    @Deprecated
    public PermissionFilterQueryBuilder() {
        // JEE requirement
    }
    
    @Inject
    public PermissionFilterQueryBuilder(GroupServiceBean groupService) {
        this.groupService = groupService;
    }

    // -------------------- LOGIC --------------------

    public String buildPermissionFilterQuery(DataverseRequest dataverseRequest) {

        User user = dataverseRequest.getUser();
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }
 
        if (user.isSuperuser()) {
            return StringUtils.EMPTY;
        }
        
        List<String> allUserGroups = new ArrayList<>();
        
        if (user.isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) user;
            allUserGroups.add(IndexServiceBean.getGroupPerUserPrefix() + au.getId());
        }
        
        List<String> userGroupStrings = collectUserGroups(dataverseRequest);
        logger.fine(userGroupStrings.toString());
        allUserGroups.addAll(userGroupStrings);
        
        String permissionFilterQuery = buildJoinQuery(allUserGroups);
        
        logger.fine(permissionFilterQuery);

        return permissionFilterQuery;

    }
    
    // -------------------- PRIVATE --------------------
    
    private String buildJoinQuery(List<String> discoverableByGroups) {
        String discoverableByQueryPart = SearchFields.DISCOVERABLE_BY + ":(" + StringUtils.join(discoverableByGroups, " OR ") + ")";
        String discoverableByPublicQueryPart = SearchFields.DISCOVERABLE_BY_PUBLIC_FROM + ":[* TO NOW]";
        
        String experimentalJoin = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}"
                + discoverableByQueryPart + " OR "
                + discoverableByPublicQueryPart;
        return experimentalJoin;
    }
    
    private List<String> collectUserGroups(DataverseRequest dataverseRequest) {
        /**
         * From a search perspective, we don't care about if the group was
         * created within one dataverse or another. We just want a list of *all*
         * the groups the user is part of. We are greedy. We want all BuiltIn
         * Groups, Shibboleth Groups, IP Groups, "system" groups, everything.
         *
         * A JOIN on "permission documents" will determine if the user can find
         * a given "content document" (dataset version, etc) in Solr.
         */
        return groupService.collectAncestors(groupService.groupsFor(dataverseRequest))
                .stream()
                .map(group -> group.getAlias())
                .filter(alias -> StringUtils.isNotEmpty(alias))
                .map(alias -> IndexServiceBean.getGroupPrefix() + alias)
                .collect(Collectors.toList());
    }
    
}
