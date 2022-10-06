package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.AcceptedConsentDao;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.OAuthTokenDataDao;
import edu.harvard.iq.dataverse.authorization.groups.ExplicitGroupDao;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.datafile.FileAccessRequestDao;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.ConfirmEmailData;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.Optional;

@Stateless
public class MergeInAccountService {

    @EJB private AuthenticationServiceBean authenticationService;
    @EJB private RoleAssigneeServiceBean roleAssigneeService;
    @EJB private SolrIndexServiceBean solrIndexService;
    @EJB private IndexServiceBean indexService;
    @EJB private DatasetDao datasetDao;
    @EJB private DvObjectServiceBean dvObjectService;
    @EJB private GuestbookResponseServiceBean guestbookResponseService;
    @EJB private UserNotificationRepository userNotificationRepository;
    @EJB private SavedSearchServiceBean savedSearchService;
    @EJB private ConfirmEmailServiceBean confirmEmailService;
    @EJB private BuiltinUserServiceBean builtinUserService;
    @EJB private GenericDao genericDao;
    @EJB private FileAccessRequestDao fileAccessRequestDao;
    @EJB private ExplicitGroupDao explicitGroupDao;
    @EJB private AcceptedConsentDao acceptedConsentDao;
    @EJB private OAuthTokenDataDao oAuthTokenDataDao;

    // -------------------- LOGIC --------------------

    @LoggedCall
    @SuperuserRequired
    public void mergeAccounts(String consumedIdentifier, String baseIdentifier)
        throws IllegalArgumentException {

        if(null == baseIdentifier || baseIdentifier.isEmpty()) {
            throw new IllegalArgumentException("Base identifier provided to change is empty.");
        } else if(null == consumedIdentifier || consumedIdentifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier to merge in is empty.");
        } else if(baseIdentifier.equals(consumedIdentifier)) {
            throw new IllegalArgumentException("You cannot merge account to itself.");
        }

        AuthenticatedUser baseAuthenticatedUser = authenticationService.getAuthenticatedUser(baseIdentifier);
        if (baseAuthenticatedUser == null) {
            throw new IllegalArgumentException("User " + baseIdentifier + " not found in AuthenticatedUser");
        }

        AuthenticatedUser consumedAuthenticatedUser = authenticationService.getAuthenticatedUser(consumedIdentifier);
        if (consumedAuthenticatedUser == null) {
            throw new IllegalArgumentException("User " + consumedIdentifier + " not found in AuthenticatedUser");
        }

        updateRoleAssignments(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateDatasetVersionUser(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateDatasetLocks(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateDvObject(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateGuestbookResponse(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateUserNotification(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateUserNotificationRequestor(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateSavedSearch(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateWorkflowComment(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateFileAccessRequest(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateExplicitGroup_AuthenticatedUser(consumedAuthenticatedUser, baseAuthenticatedUser);
        updateAcceptedConsents(consumedAuthenticatedUser, baseAuthenticatedUser);

        removeConfirmEmailData(consumedAuthenticatedUser);
        removeOAuth2TokenData(consumedAuthenticatedUser);
        removeApiToken(consumedAuthenticatedUser);
        removeUser(consumedAuthenticatedUser);
    }

    // -------------------- PRIVATE --------------------

    private void updateRoleAssignments(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        List<RoleAssignment> baseRAList = roleAssigneeService.getAssignmentsFor(baseAU.getIdentifier());
        List<RoleAssignment> consumedRAList = roleAssigneeService.getAssignmentsFor(consumedAU.getIdentifier());

        addNewRolesToBaseUser(baseAU, baseRAList, consumedRAList);
        removeRemainingRolesFor(consumedAU);
    }

    private void removeRemainingRolesFor(AuthenticatedUser consumedAU) {
        roleAssigneeService.removeAllRolesForUserByIdentifier(consumedAU.getIdentifier());
    }

    /***
     * Only authenticated users accounts can be merge.
     * These accounts are prefixed with '@' in {@link RoleAssignment}.
     * Throw {@link IllegalArgumentException} when a given role doesn't belong to authenticated user.
     *
     * Algorithm:
     * 1. For each consumed user role check if base user already has it
     * 2. If NO: merge the role to base user and update solr
     * 3. If YES: skip it, they will be removed in one call after checking all roles.
     *
     * @param baseAU - user to which we merge account
     * @param baseRAList - baseAU roles list
     * @param consumedRAList - user whose account is to be merged roles list
     */
    private void addNewRolesToBaseUser(AuthenticatedUser baseAU, List<RoleAssignment> baseRAList, List<RoleAssignment> consumedRAList) {
        for(RoleAssignment consumedRA : consumedRAList) {
            if(consumedRA.getAssigneeIdentifier().charAt(0) == '@') {

                Optional<RoleAssignment> sameRoleForBothUsers = baseRAList.stream()
                        .filter(baseRa -> baseRa
                                .getDefinitionPoint()
                                .getId()
                                .equals(consumedRA.getDefinitionPoint().getId())
                                && baseRa.getRole().equals(consumedRA.getRole()))
                        .findAny();

                if (!sameRoleForBothUsers.isPresent()) {
                    consumedRA.setAssigneeIdentifier(baseAU.getIdentifier());
                    genericDao.merge(consumedRA);
                    solrIndexService.indexPermissionsForOneDvObject(consumedRA.getDefinitionPoint());
                    indexService.indexDvObject(consumedRA.getDefinitionPoint());
                }
            } else {
                throw new IllegalArgumentException("Original userIdentifier provided does not seem to be an AuthenticatedUser");
            }
        }
    }

    private void updateDatasetVersionUser(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (DatasetVersionUser user : datasetDao.getDatasetVersionUsersByAuthenticatedUser(consumedAU)) {
            user.setAuthenticatedUser(baseAU);
            genericDao.merge(user);
        }
    }

    private void updateDatasetLocks(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (DatasetLock lock : datasetDao.getDatasetLocksByUser(consumedAU)) {
            lock.setUser(baseAU);
            genericDao.merge(lock);
        }
    }

    private void updateDvObject(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (DvObject dvo : dvObjectService.findByAuthenticatedUserId(consumedAU)) {
            if (dvo.getCreator().equals(consumedAU)){
                dvo.setCreator(baseAU);
            }
            if (dvo.getReleaseUser() != null &&  dvo.getReleaseUser().equals(consumedAU)){
                dvo.setReleaseUser(baseAU);
            }
            genericDao.merge(dvo);
        }
    }

    private void updateGuestbookResponse(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (GuestbookResponse gbr : guestbookResponseService.findByAuthenticatedUserId(consumedAU)) {
            gbr.setAuthenticatedUser(baseAU);
            genericDao.merge(gbr);
        }
    }

    private void updateUserNotification(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (UserNotification note : userNotificationRepository.findByUser(consumedAU.getId())) {
            note.setUser(baseAU);
            userNotificationRepository.save(note);
        }
    }

    private void updateUserNotificationRequestor(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        Long oldId = consumedAU.getId();
        Long newId = baseAU.getId();
        userNotificationRepository.updateRequestor(oldId, newId);
    }

    private void updateSavedSearch(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (SavedSearch search : savedSearchService.findByAuthenticatedUser(consumedAU)) {
            search.setCreator(baseAU);
            genericDao.merge(search);
        }
    }

    private void updateWorkflowComment(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        for (WorkflowComment wc : authenticationService.getWorkflowCommentsByAuthenticatedUser(consumedAU)) {
            wc.setAuthenticatedUser(baseAU);
            genericDao.merge(wc);
        }
    }

    private void updateFileAccessRequest(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        fileAccessRequestDao.updateAuthenticatedUser(consumedAU.getId(), baseAU.getId());
    }

    private void updateExplicitGroup_AuthenticatedUser(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        explicitGroupDao.updateAuthenticatedUser(consumedAU.getId(), baseAU.getId());
    }

    private void updateAcceptedConsents(AuthenticatedUser consumedAU, AuthenticatedUser baseAU) {
        acceptedConsentDao.updateAuthenticatedUser(consumedAU.getId(), baseAU.getId());
    }

    private void removeConfirmEmailData(AuthenticatedUser consumedAU) {
        ConfirmEmailData confirmEmailData = confirmEmailService.findSingleConfirmEmailDataByUser(consumedAU);
        if (confirmEmailData != null){
            genericDao.remove(confirmEmailData);
        }
    }

    private void removeOAuth2TokenData(AuthenticatedUser consumedAU) {
        oAuthTokenDataDao.removeAuthenticatedUserTokenData(consumedAU.getId());
    }

    private void removeApiToken(AuthenticatedUser consumedAU) {
        ApiToken toRemove = authenticationService.findApiTokenByUser(consumedAU);
        if(null != toRemove) {
            genericDao.remove(toRemove);
        }
    }

    private void removeUser(AuthenticatedUser consumedAU) {
        AuthenticatedUserLookup consumedAUL = consumedAU.getAuthenticatedUserLookup();
        genericDao.remove(consumedAUL);
        genericDao.remove(consumedAU);
        BuiltinUser consumedBuiltinUser = builtinUserService.findByUserName(consumedAU.getUserIdentifier());
        if (consumedBuiltinUser != null){
            genericDao.remove(consumedBuiltinUser);
        }
    }
}
