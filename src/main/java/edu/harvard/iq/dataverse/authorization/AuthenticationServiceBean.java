package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetData;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * AuthenticationService is for general authentication-related operations.
 * It's no longer responsible for registering and listing
 * AuthenticationProviders! A dedicated singleton has been created for that
 * purpose - AuthenticationProvidersRegistrationServiceBean - and all the 
 * related code has been moved there. 
 * 
 */
@Named
@Stateless
public class AuthenticationServiceBean {
    private static final Logger logger = Logger.getLogger(AuthenticationServiceBean.class.getName());
    
    @EJB
    AuthenticationProvidersRegistrationServiceBean authProvidersRegistrationService;
    
    @EJB
    BuiltinUserServiceBean builtinUserServiceBean;
    
    @EJB
    IndexServiceBean indexService;
    
    @EJB
    protected ActionLogServiceBean actionLogSvc;
    
    @EJB
    UserNotificationServiceBean userNotificationService;

    @EJB
    ConfirmEmailServiceBean confirmEmailService;
    
    @EJB
    PasswordResetServiceBean passwordResetServiceBean;

    @EJB
    UserServiceBean userService;

    @EJB
    PasswordValidatorServiceBean passwordValidatorService;
    
    @EJB
    DvObjectServiceBean dvObjSvc;
    
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
    @EJB
    GuestbookResponseServiceBean gbRespSvc;
    
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    
    @EJB 
    ExplicitGroupServiceBean explicitGroupService;
        
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
        
        
    public AbstractOAuth2AuthenticationProvider getOAuth2Provider( String id ) {
        return authProvidersRegistrationService.getOAuth2AuthProvidersMap().get(id);
    }
    
    public Set<AbstractOAuth2AuthenticationProvider> getOAuth2Providers() {
        return new HashSet<>(authProvidersRegistrationService.getOAuth2AuthProvidersMap().values());
    }
    
    public Set<String> getAuthenticationProviderIds() {
        return authProvidersRegistrationService.getAuthenticationProvidersMap().keySet();
    }

    public Collection<AuthenticationProvider> getAuthenticationProviders() {
        return authProvidersRegistrationService.getAuthenticationProvidersMap().values();
    }
    
    public <T extends AuthenticationProvider> Set<String> getAuthenticationProviderIdsOfType( Class<T> aClass ) {
        Set<String> retVal = new TreeSet<>();
        for ( Map.Entry<String, AuthenticationProvider> p : authProvidersRegistrationService.getAuthenticationProvidersMap().entrySet() ) {
            if ( aClass.isAssignableFrom( p.getValue().getClass() ) ) {
                retVal.add( p.getKey() );
            }
        }
        return retVal;
    }
    
    public AuthenticationProviderFactory getProviderFactory( String alias ) {
        return authProvidersRegistrationService.getProviderFactoriesMap().get(alias);
    }
    
    public AuthenticationProvider getAuthenticationProvider( String id ) {
        return authProvidersRegistrationService.getAuthenticationProvidersMap().get( id );
    }
    
    public AuthenticatedUser findByID(Object pk){
        if (pk==null){
            return null;
        }
        return em.find(AuthenticatedUser.class, pk);
    }

    public void removeApiToken(AuthenticatedUser user){
        if (user!=null) {
            ApiToken apiToken = findApiTokenByUser(user);
            if (apiToken != null) {
                em.remove(apiToken);
            }
        }
    }
    
    public boolean isOrcidEnabled() {
        return authProvidersRegistrationService.getOAuth2AuthProvidersMap().values().stream().anyMatch( s -> s.getId().toLowerCase().contains("orcid") );
    }
    
    /**
     * Use with care! This method was written primarily for developers
     * interested in API testing who want to:
     * 
     * 1. Create a temporary user and get an API token.
     * 
     * 2. Do some work with that API token.
     * 
     * 3. Delete all the stuff that was created with the API token.
     * 
     * 4. Delete the temporary user.
     * 
     * Before calling this method, make sure you've deleted all the stuff tied
     * to the user, including stuff they've created, role assignments, group
     * assignments, etc.
     * 
     * Longer term, the intention is to have a "disableAuthenticatedUser"
     * method/command. See https://github.com/IQSS/dataverse/issues/2419
     */
    public void deleteAuthenticatedUser(Object pk) {
        AuthenticatedUser user = em.find(AuthenticatedUser.class, pk);

        if (user != null) {
            ApiToken apiToken = findApiTokenByUser(user);
            if (apiToken != null) {
                em.remove(apiToken);
            }
            // @todo: this should be handed down to the service instead of doing it here.
            ConfirmEmailData confirmEmailData = confirmEmailService.findSingleConfirmEmailDataByUser(user);
            if (confirmEmailData != null) {
                /**
                 * @todo This could probably be a cascade delete instead.
                 */
                em.remove(confirmEmailData);
            }
            userNotificationService.findByUser(user.getId()).forEach(userNotificationService::delete);
            
            AuthenticationProvider prv = lookupProvider(user);
            if ( prv != null && prv.isUserDeletionAllowed() ) {
                prv.deleteUser(user.getAuthenticatedUserLookup().getPersistentUserId());
            }
            
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "deleteUser")
                .setInfo(user.getUserIdentifier()));
            em.remove(user.getAuthenticatedUserLookup());         
            em.remove(user);

        }
    }
            
    public AuthenticatedUser getAuthenticatedUser( String identifier ) {
        try {
            return em.createNamedQuery("AuthenticatedUser.findByIdentifier", AuthenticatedUser.class)
                    .setParameter("identifier", identifier)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public AuthenticatedUser getAuthenticatedUserWithProvider( String identifier ) {
        try {
            AuthenticatedUser authenticatedUser = em.createNamedQuery("AuthenticatedUser.findByIdentifier", AuthenticatedUser.class)
                    .setParameter("identifier", identifier)
                    .getSingleResult();
            AuthenticatedUserLookup aul = em.createNamedQuery("AuthenticatedUserLookup.findByAuthUser", AuthenticatedUserLookup.class)
                    .setParameter("authUser", authenticatedUser)
                    .getSingleResult();
            authenticatedUser.setAuthProviderId(aul.getAuthenticationProviderId());
            
            return authenticatedUser;
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public AuthenticatedUser getAdminUser() {
        try {
            return em.createNamedQuery("AuthenticatedUser.findAdminUser", AuthenticatedUser.class)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public AuthenticatedUser getAuthenticatedUserByEmail( String email ) {
        try {
            return em.createNamedQuery("AuthenticatedUser.findByEmail", AuthenticatedUser.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch ( NoResultException ex ) {
            logger.log(Level.INFO, "no user found using {0}", email);
            return null;
        } catch ( NonUniqueResultException ex ) {
            logger.log(Level.INFO, "multiple users found using {0}: {1}", new Object[]{email, ex});
            return null;
        }
    }

    /**
     * Returns an {@link AuthenticatedUser} matching the passed provider id and the authentication request. If
     *  no such user exist, it is created and then returned.
     * 
     * <strong>Invariant:</strong> upon successful return from this call, an {@link AuthenticatedUser} record 
     * matching the request and provider exists in the database.
     * 
     * @param authenticationProviderId
     * @param req
     * @return The authenticated user for the passed provider id and authentication request.
     * @throws AuthenticationFailedException 
     */
    public AuthenticatedUser getUpdateAuthenticatedUser( String authenticationProviderId, AuthenticationRequest req ) throws AuthenticationFailedException {
        AuthenticationProvider prv = getAuthenticationProvider(authenticationProviderId);
        if ( prv == null ) throw new IllegalArgumentException("No authentication provider listed under id " + authenticationProviderId );
        if ( ! (prv instanceof CredentialsAuthenticationProvider) ) {
            throw new IllegalArgumentException( authenticationProviderId + " does not support credentials-based authentication." );
        }
        AuthenticationResponse resp = ((CredentialsAuthenticationProvider)prv).authenticate(req);
        
        if ( resp.getStatus() == AuthenticationResponse.Status.SUCCESS ) {
            // yay! see if we already have this user.
            AuthenticatedUser user = lookupUser(authenticationProviderId, resp.getUserId());

            if (user != null){
                user = userService.updateLastLogin(user);
            }
            
            if ( user == null ) {
                throw new IllegalStateException("Authenticated user does not exist. The functionality to support creating one at this point in authentication has been removed.");
                //return createAuthenticatedUser(
                //        new UserRecordIdentifier(authenticationProviderId, resp.getUserId()), resp.getUserId(), resp.getUserDisplayInfo(), true );
            } else {
                if (BuiltinAuthenticationProvider.PROVIDER_ID.equals(user.getAuthenticatedUserLookup().getAuthenticationProviderId())) {
                    return user;
                } else {
                    return updateAuthenticatedUser(user, resp.getUserDisplayInfo());
                }
            }
        } else { 
            throw new AuthenticationFailedException(resp, "Authentication Failed: " + resp.getMessage());
        }
    }
    
    /**
     * @param email
     * @return {@code true} iff the none of the authenticated users has the passed email address.
     */
    public boolean isEmailAddressAvailable(String email) {
        return em.createNamedQuery("AuthenticatedUser.findByEmail", AuthenticatedUser.class)
                 .setParameter("email", email)
                 .getResultList().isEmpty();
    }
    
    public AuthenticatedUser lookupUser(UserRecordIdentifier id) {
        return lookupUser(id.repoId, id.userIdInRepo);
    }
    
    public AuthenticatedUser lookupUser(String authPrvId, String userPersistentId) {
        TypedQuery<AuthenticatedUserLookup> typedQuery = em.createNamedQuery("AuthenticatedUserLookup.findByAuthPrvID_PersUserId", AuthenticatedUserLookup.class);
        typedQuery.setParameter("authPrvId", authPrvId);
        typedQuery.setParameter("persUserId", userPersistentId);
        try {
            AuthenticatedUserLookup au = typedQuery.getSingleResult();
            return au.getAuthenticatedUser();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
    public AuthenticationProvider lookupProvider( AuthenticatedUser user )  {
        return authProvidersRegistrationService.getAuthenticationProvidersMap().get(user.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }
    
    public ApiToken findApiToken(String token) {
        try {
            return em.createNamedQuery("ApiToken.findByTokenString", ApiToken.class)
                    .setParameter("tokenString", token)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    public ApiToken findApiTokenByUser(AuthenticatedUser au) {
        if (au == null) {
            return null;
        }
        TypedQuery<ApiToken> typedQuery = em.createNamedQuery("ApiToken.findByUser", ApiToken.class);
        typedQuery.setParameter("user", au);
        List<ApiToken> tokens = typedQuery.getResultList();
        Timestamp latest = new Timestamp(java.time.Instant.now().getEpochSecond()*1000);
        if (tokens.isEmpty()) {
            // Normal case - no token exists
            return null;
        }
        if (tokens.size() == 1) {
            // Normal case - one token that may or may not have expired
            ApiToken token = tokens.get(0);
            if (token.getExpireTime().before(latest)) {
                // Don't return an expired token which is unusable, delete it instead
                em.remove(token);
                return null;
            } else {
                return tokens.get(0);
            }
        } else {
            // We have more than one due to https://github.com/IQSS/dataverse/issues/6389 or
            // similar, so we should delete all but one token.
            // Since having an expired token also makes no sense, if we only have an expired
            // token, remove that as well
            ApiToken goodToken = null;
            for (ApiToken token : tokens) {
                Timestamp time = token.getExpireTime();
                if (time.before(latest)) {
                    em.remove(token);
                } else {
                    if(goodToken != null) {
                      em.remove(goodToken);
                      goodToken = null;
                    }
                    latest = time;
                    goodToken = token;
                }
            }
            // Null if there are no un-expired ones
            return goodToken;
        }
    }
    
    
    // A method for generating a new API token;
    // TODO: this is a simple, one-size-fits-all solution; we'll need
    // to expand this system, to be able to generate tokens with different
    // lifecycles/valid for specific actions only, etc. 
    // -- L.A. 4.0 beta12
    public ApiToken generateApiTokenForUser(AuthenticatedUser au) {
        if (au == null) {
            return null;
        }

        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString(java.util.UUID.randomUUID().toString());
        apiToken.setAuthenticatedUser(au);
        Calendar c = Calendar.getInstance();
        apiToken.setCreateTime(new Timestamp(c.getTimeInMillis()));
        c.roll(Calendar.YEAR, 1);
        apiToken.setExpireTime(new Timestamp(c.getTimeInMillis()));
        save(apiToken);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "generateApiToken")
            .setInfo("user:" + au.getIdentifier() + " token:" +  apiToken.getTokenString()));

        return apiToken;
    }

    public AuthenticatedUser lookupUser( String apiToken ) {
        ApiToken tkn = findApiToken(apiToken);
        if ( tkn == null ) return null;
        
        if ( tkn.isDisabled() ) return null;
        if ( tkn.getExpireTime() != null ) {
            if ( tkn.getExpireTime().before( new Timestamp(new Date().getTime())) ) {
                em.remove(tkn);
		logger.info("attempted access with expired token: " + apiToken);
                return null;
            }
        }
        
        return tkn.getAuthenticatedUser();
    }
    
    public AuthenticatedUser lookupUserForWorkflowInvocationID(String wfId) {
        try {
            PendingWorkflowInvocation pwfi = em.find(PendingWorkflowInvocation.class, wfId);
            if (pwfi == null) {
                return null;
            }
            if (pwfi.getUserId().startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                if (pwfi.getLocalData().containsKey(PendingWorkflowInvocation.AUTHORIZED)
                        && Boolean.parseBoolean(pwfi.getLocalData().get(PendingWorkflowInvocation.AUTHORIZED))) {
                    return getAuthenticatedUser(
                            pwfi.getUserId().substring(AuthenticatedUser.IDENTIFIER_PREFIX.length()));
                }
            }
        } catch (NoResultException ex) {
            return null;
        }
        return null;
    }
    
    /*
    getDeleteUserErrorMessages( AuthenticatedUser au )
    method which checks for reasons that a user may not be deleted
    -has created dvObjects
    -has roles
    -has guestbook records

    An empty string is returned if the user is 'deletable'
    */
    
    public String getDeleteUserErrorMessages(AuthenticatedUser au) {
        String retVal = "";
        List<String> reasons= new ArrayList();
        if (!dvObjSvc.findByAuthenticatedUserId(au).isEmpty()) {
            reasons.add(BundleUtil.getStringFromBundle("admin.api.deleteUser.failure.dvobjects"));
        }

        if (!roleAssigneeSvc.getAssignmentsFor(au.getIdentifier()).isEmpty()) {
            reasons.add(BundleUtil.getStringFromBundle("admin.api.deleteUser.failure.roleAssignments"));
        }

        if (!gbRespSvc.findByAuthenticatedUserId(au).isEmpty()) {
            reasons.add( BundleUtil.getStringFromBundle("admin.api.deleteUser.failure.gbResps"));
        }

        if (!datasetVersionService.getDatasetVersionUsersByAuthenticatedUser(au).isEmpty()) {
            reasons.add(BundleUtil.getStringFromBundle("admin.api.deleteUser.failure.versionUser"));
        }
        
        if (!reasons.isEmpty()) {
            retVal = BundleUtil.getStringFromBundle("admin.api.deleteUser.failure.prefix", Arrays.asList(au.getIdentifier()));
            retVal += " " + reasons.stream().collect(Collectors.joining("; ")) + ".";
        }
        


        return retVal;
    }
    
    public void removeAuthentictedUserItems(AuthenticatedUser au){
        /* if the user has pending access requests, is the member of a group or 
        we will delete them here 
        */

        deletePendingAccessRequests(au);
        
        deleteBannerMessages(au);
               
        if (!explicitGroupService.findGroups(au).isEmpty()) {
            for(ExplicitGroup explicitGroup: explicitGroupService.findGroups(au)){
                explicitGroup.removeByRoleAssgineeIdentifier(au.getIdentifier());
            }            
        }
        
    }
    
    private void deleteBannerMessages(AuthenticatedUser  au){
        
       em.createNativeQuery("delete from userbannermessage where user_id  = "+au.getId()).executeUpdate();
        
    }
    
    private void deletePendingAccessRequests(AuthenticatedUser  au){
        
       em.createNativeQuery("delete from fileaccessrequests where authenticated_user_id  = "+au.getId()).executeUpdate();
        
    }
    
    
    public AuthenticatedUser save( AuthenticatedUser user ) {
        em.persist(user);
        em.flush();
        return user;
    }
    
    public AuthenticatedUser update( AuthenticatedUser user ) {
        return em.merge(user);
    }
    
    public ApiToken save( ApiToken aToken ) {
        if ( aToken.getId() == null ) {
            em.persist(aToken);
            return aToken;
        } else { 
            return em.merge( aToken );
            
        }
    }
    
    /**
     * Associates the passed {@link AuthenticatedUser} with a new provider.
     * @param authenticatedUser the authenticated being re-associated
     * @param authenticationProviderId Id of the new provider
     * @param persistentIdInProvider Id of the user in the new provider
     * @return {@code true} iff the change was successful.
     */
    public boolean updateProvider( AuthenticatedUser authenticatedUser, String authenticationProviderId, String persistentIdInProvider ) {
        try {
            AuthenticatedUserLookup aul = em.createNamedQuery("AuthenticatedUserLookup.findByAuthUser", AuthenticatedUserLookup.class)
                    .setParameter("authUser", authenticatedUser)
                    .getSingleResult();
            aul.setAuthenticationProviderId(authenticationProviderId);
            aul.setPersistentUserId(persistentIdInProvider);
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth,
                    authenticatedUser.getIdentifier() + " now associated with provider " + authenticationProviderId + " id: " + persistentIdInProvider) );
            return true;
            
        } catch ( NoResultException | NonUniqueResultException ex ) {
            logger.log(Level.WARNING, "Error converting user " + authenticatedUser.getUserIdentifier() + ": " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Creates an authenticated user based on the passed
     * {@code userDisplayInfo}, a lookup entry for them based
     * UserIdentifier.getLookupStringPerAuthProvider (within the supplied
     * authentication provider), and internal user identifier (used for role
     * assignments, etc.) based on UserIdentifier.getInternalUserIdentifer.
     *
     * @param userRecordId
     * @param proposedAuthenticatedUserIdentifier
     * @param userDisplayInfo
     * @param generateUniqueIdentifier if {@code true}, create a new, unique user identifier for the created user, if the suggested one exists.
     * @return the newly created user, or {@code null} if the proposed identifier exists and {@code generateUniqueIdentifier} was {@code false}.
     * @throws EJBException which may wrap an ConstraintViolationException if the proposed user does not pass bean validation.
     */
    public AuthenticatedUser createAuthenticatedUser(UserRecordIdentifier userRecordId,
            String proposedAuthenticatedUserIdentifier,
            AuthenticatedUserDisplayInfo userDisplayInfo,
            boolean generateUniqueIdentifier) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        // set account creation time & initial login time (same timestamp)
        authenticatedUser.setCreatedTime(new Timestamp(new Date().getTime()));
        authenticatedUser.setLastLoginTime(authenticatedUser.getCreatedTime());
        
        authenticatedUser.applyDisplayInfo(userDisplayInfo);

        // we have no desire for leading or trailing whitespace in identifiers
        if (proposedAuthenticatedUserIdentifier != null) {
            proposedAuthenticatedUserIdentifier = proposedAuthenticatedUserIdentifier.trim();
        }
        // we now select a username for the generated AuthenticatedUser, or give up
        String internalUserIdentifer = proposedAuthenticatedUserIdentifier;
        // TODO should lock table authenticated users for write here
        if ( identifierExists(internalUserIdentifer) ) {
            if ( ! generateUniqueIdentifier ) {
                return null;
            }
            int i=1;
            String identifier = internalUserIdentifer + i;
            while ( identifierExists(identifier) ) {
                i += 1;
            }
            authenticatedUser.setUserIdentifier(identifier);
        } else {
            authenticatedUser.setUserIdentifier(internalUserIdentifer);
        }
        authenticatedUser = save( authenticatedUser );
        // TODO should unlock table authenticated users for write here
        AuthenticatedUserLookup auusLookup = userRecordId.createAuthenticatedUserLookup(authenticatedUser);
        em.persist( auusLookup );
        authenticatedUser.setAuthenticatedUserLookup(auusLookup);

        if (ShibAuthenticationProvider.PROVIDER_ID.equals(auusLookup.getAuthenticationProviderId())) {
            Timestamp emailConfirmedNow = new Timestamp(new Date().getTime());
            // Email addresses for Shib users are confirmed by the Identity Provider.
            authenticatedUser.setEmailConfirmed(emailConfirmedNow);
            authenticatedUser = save(authenticatedUser);
        } else {
            /* @todo Rather than creating a token directly here it might be
             * better to do something like "startConfirmEmailProcessForNewUser". */
            confirmEmailService.createToken(authenticatedUser);
        }
        
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "createUser")
            .setInfo(authenticatedUser.getIdentifier()));

        return authenticatedUser;
    }
    
    /**
     * Checks whether the {@code idtf} is already taken by another {@link AuthenticatedUser}.
     * @param idtf
     * @return {@code true} iff there's already a user by that username.
     */
    public boolean identifierExists( String idtf ) {
        return em.createNamedQuery("AuthenticatedUser.countOfIdentifier", Number.class)
                .setParameter("identifier", idtf)
                .getSingleResult().intValue() > 0;
    }
    
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser user, AuthenticatedUserDisplayInfo userDisplayInfo) {
        user.applyDisplayInfo(userDisplayInfo);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "updateUser")
            .setInfo(user.getIdentifier()));
        return update(user);
    }
    
    public List<AuthenticatedUser> findAllAuthenticatedUsers() {
        return em.createNamedQuery("AuthenticatedUser.findAll", AuthenticatedUser.class).getResultList();
    }

    public List<AuthenticatedUser> findSuperUsers() {
        return em.createNamedQuery("AuthenticatedUser.findSuperUsers", AuthenticatedUser.class).getResultList();
    }
    
    
    public Set<AuthenticationProviderFactory> listProviderFactories() {
        return new HashSet<>( authProvidersRegistrationService.getProviderFactoriesMap().values() ); 
    }
    
    public Timestamp getCurrentTimestamp() {
        return new Timestamp(new Date().getTime());
    }

    // TODO should probably be moved to the Shib provider - this is a classic Shib-specific
    //      use case. This class should deal with general autnetications.
    @Deprecated
    /**
     * @deprecated. Switch to convertBuiltInUserToRemoteUser instead.
     * @todo. Switch to convertBuiltInUserToRemoteUser instead.
     */
    public AuthenticatedUser convertBuiltInToShib(AuthenticatedUser builtInUserToConvert, String shibProviderId, UserIdentifier newUserIdentifierInLookupTable) {
        logger.info("converting user " + builtInUserToConvert.getId() + " from builtin to shib");
        String builtInUserIdentifier = builtInUserToConvert.getIdentifier();
        logger.info("builtin user identifier: " + builtInUserIdentifier);
        TypedQuery<AuthenticatedUserLookup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM AuthenticatedUserLookup AS o WHERE o.authenticatedUser = :auid", AuthenticatedUserLookup.class);
        typedQuery.setParameter("auid", builtInUserToConvert);
        AuthenticatedUserLookup authuserLookup;
        try {
            authuserLookup = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("exception caught: " + ex);
            return null;
        }
        if (authuserLookup == null) {
            return null;
        }

        String oldProviderId = authuserLookup.getAuthenticationProviderId();
        logger.info("we expect this to be 'builtin': " + oldProviderId);
        authuserLookup.setAuthenticationProviderId(shibProviderId);
        String oldUserLookupIdentifier = authuserLookup.getPersistentUserId();
        logger.info("this should be 'pete' or whatever the old builtin username was: " + oldUserLookupIdentifier);
        String perUserShibIdentifier = newUserIdentifierInLookupTable.getLookupStringPerAuthProvider();
        authuserLookup.setPersistentUserId(perUserShibIdentifier);
        /**
         * @todo this should be a transaction of some kind. We want to update
         * the authenticateduserlookup and also delete the row from the
         * builtinuser table in a single transaction.
         */
        em.persist(authuserLookup);
        String builtinUsername = builtInUserIdentifier.replaceFirst(AuthenticatedUser.IDENTIFIER_PREFIX, "");
        BuiltinUser builtin = builtinUserServiceBean.findByUserName(builtinUsername);
        if (builtin != null) {
            // These were created by AuthenticationResponse.Status.BREAKOUT in canLogInAsBuiltinUser
            List<PasswordResetData> oldTokens = passwordResetServiceBean.findPasswordResetDataByDataverseUser(builtin);
            for (PasswordResetData oldToken : oldTokens) {
                em.remove(oldToken);
            }
            em.remove(builtin);
        } else {
            logger.info("Couldn't delete builtin user because could find it based on username " + builtinUsername);
        }
        AuthenticatedUser shibUser = lookupUser(shibProviderId, perUserShibIdentifier);
        if (shibUser != null) {
            return shibUser;
        }
        return null;
    }

    public AuthenticatedUser convertBuiltInUserToRemoteUser(AuthenticatedUser builtInUserToConvert, String newProviderId, UserIdentifier newUserIdentifierInLookupTable) {
        logger.info("converting user " + builtInUserToConvert.getId() + " from builtin to remote");
        String builtInUserIdentifier = builtInUserToConvert.getIdentifier();
        logger.info("builtin user identifier: " + builtInUserIdentifier);
        TypedQuery<AuthenticatedUserLookup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM AuthenticatedUserLookup AS o WHERE o.authenticatedUser = :auid", AuthenticatedUserLookup.class);
        typedQuery.setParameter("auid", builtInUserToConvert);
        AuthenticatedUserLookup authuserLookup;
        try {
            authuserLookup = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("exception caught: " + ex);
            return null;
        }
        if (authuserLookup == null) {
            return null;
        }

        String oldProviderId = authuserLookup.getAuthenticationProviderId();
        logger.info("we expect this to be 'builtin': " + oldProviderId);
        authuserLookup.setAuthenticationProviderId(newProviderId);
        String oldUserLookupIdentifier = authuserLookup.getPersistentUserId();
        logger.info("this should be 'pete' or whatever the old builtin username was: " + oldUserLookupIdentifier);
        String perUserIdentifier = newUserIdentifierInLookupTable.getLookupStringPerAuthProvider();
        authuserLookup.setPersistentUserId(perUserIdentifier);
        /**
         * @todo this should be a transaction of some kind. We want to update
         * the authenticateduserlookup and also delete the row from the
         * builtinuser table in a single transaction.
         */
        em.persist(authuserLookup);
        String builtinUsername = builtInUserIdentifier.replaceFirst(AuthenticatedUser.IDENTIFIER_PREFIX, "");
        BuiltinUser builtin = builtinUserServiceBean.findByUserName(builtinUsername);
        if (builtin != null) {
            // These were created by AuthenticationResponse.Status.BREAKOUT in canLogInAsBuiltinUser
            List<PasswordResetData> oldTokens = passwordResetServiceBean.findPasswordResetDataByDataverseUser(builtin);
            for (PasswordResetData oldToken : oldTokens) {
                em.remove(oldToken);
            }
            em.remove(builtin);
        } else {
            logger.info("Couldn't delete builtin user because could find it based on username " + builtinUsername);
        }
        AuthenticatedUser nonBuiltinUser = lookupUser(newProviderId, perUserIdentifier);
        if (nonBuiltinUser != null) {
            return nonBuiltinUser;
        }
        return null;
    }

    /**
     * @param idOfAuthUserToConvert The id of the remote AuthenticatedUser
     * (Shibboleth user or OAuth user) to convert to a BuiltinUser.
     * @param newEmailAddress The new email address that will be used instead of
     * the user's old email address from the institution that they have left.
     * @return BuiltinUser
     * @throws java.lang.Exception You must catch and report back to the user (a
     * superuser) any Exceptions.
     */
    public BuiltinUser convertRemoteToBuiltIn(Long idOfAuthUserToConvert, String newEmailAddress) throws Exception {
        AuthenticatedUser authenticatedUser = findByID(idOfAuthUserToConvert);
        if (authenticatedUser == null) {
            throw new Exception("User id " + idOfAuthUserToConvert + " not found.");
        }
        AuthenticatedUser existingUserWithSameEmail = getAuthenticatedUserByEmail(newEmailAddress);
        if (existingUserWithSameEmail != null) {
            throw new Exception("User id " + idOfAuthUserToConvert + " (" + authenticatedUser.getIdentifier() + ") cannot be converted from remote to BuiltIn because the email address " + newEmailAddress + " is already in use by user id " + existingUserWithSameEmail.getId() + " (" + existingUserWithSameEmail.getIdentifier() + ").");
        }
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName(authenticatedUser.getUserIdentifier());
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<BuiltinUser>> violations = validator.validate(builtinUser);
        int numViolations = violations.size();
        if (numViolations > 0) {
            StringBuilder logMsg = new StringBuilder();
            for (ConstraintViolation<?> violation : violations) {
                logMsg.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
            }
            throw new Exception("User id " + idOfAuthUserToConvert + " cannot be converted from remote to BuiltIn because of constraint violations on the BuiltIn user that would be created: " + numViolations + ". Details: " + logMsg);
        }
        try {
            builtinUser = builtinUserServiceBean.save(builtinUser);
        } catch (IllegalArgumentException ex) {
            throw new Exception("User id " + idOfAuthUserToConvert + " cannot be converted from remote to BuiltIn because of an IllegalArgumentException creating the row in the builtinuser table: " + ex);
        }
        AuthenticatedUserLookup lookup = authenticatedUser.getAuthenticatedUserLookup();
        if (lookup == null) {
            throw new Exception("User id " + idOfAuthUserToConvert + " does not have an 'authenticateduserlookup' row");
        }
        String providerId = lookup.getAuthenticationProviderId();
        if (providerId == null) {
            throw new Exception("User id " + idOfAuthUserToConvert + " provider id is null.");
        }
        String builtinProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
        if (providerId.equals(builtinProviderId)) {
            throw new Exception("User id " + idOfAuthUserToConvert + " cannot be converted from remote to BuiltIn because current provider id is '" + providerId + "' which is the same as '" + builtinProviderId + "'. This user is already a BuiltIn user.");
        }
        lookup.setAuthenticationProviderId(BuiltinAuthenticationProvider.PROVIDER_ID);
        lookup.setPersistentUserId(authenticatedUser.getUserIdentifier());
        em.persist(lookup);
        authenticatedUser.setEmail(newEmailAddress);
        em.persist(authenticatedUser);
        em.flush();
        return builtinUser;
    }

    public AuthenticatedUser canLogInAsBuiltinUser(String username, String password) {
        logger.fine("checking to see if " + username + " knows the password...");
        if (password == null) {
            logger.info("password was null");
            return null;
        }

        AuthenticationRequest authReq = new AuthenticationRequest();
        /**
         * @todo Should the credential key really be a Bundle key?
         * BuiltinAuthenticationProvider.KEY_USERNAME_OR_EMAIL, for example, is
         * "login.builtin.credential.usernameOrEmail" as of this writing.
         */
        authReq.putCredential(BuiltinAuthenticationProvider.KEY_USERNAME_OR_EMAIL, username);
        authReq.putCredential(BuiltinAuthenticationProvider.KEY_PASSWORD, password);
        /**
         * @todo Should probably set IP address here.
         */
//        authReq.setIpAddress(session.getUser().getRequestMetadata().getIpAddress());

        String credentialsAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
        try {
            AuthenticatedUser au = getUpdateAuthenticatedUser(credentialsAuthProviderId, authReq);
            logger.fine("User authenticated:" + au.getEmail());
            return au;
        } catch (AuthenticationFailedException ex) {
            logger.info("The username and/or password entered is invalid: " + ex.getResponse().getMessage());
            if (AuthenticationResponse.Status.BREAKOUT.equals(ex.getResponse().getStatus())) {
                /**
                 * Note that this "BREAKOUT" status creates PasswordResetData!
                 * We'll delete it just before blowing away the BuiltinUser in
                 * AuthenticationServiceBean.convertBuiltInToShib
                 */
                logger.info("AuthenticationFailedException caught in canLogInAsBuiltinUser: The username and/or password entered is invalid: " + ex.getResponse().getMessage() + " - Maybe the user (" + username + ") hasn't upgraded their password? Checking the old password...");
                BuiltinUser builtinUser = builtinUserServiceBean.findByUserName(username);
                if (builtinUser != null) {
                    boolean userAuthenticated = PasswordEncryption.getVersion(builtinUser.getPasswordEncryptionVersion()).check(password, builtinUser.getEncryptedPassword());
                    if (userAuthenticated == true) {
                        AuthenticatedUser authUser = lookupUser(BuiltinAuthenticationProvider.PROVIDER_ID, builtinUser.getUserName());
                        if (authUser != null) {
                            return authUser;
                        } else {
                            logger.info("canLogInAsBuiltinUser: Couldn't find AuthenticatedUser based on BuiltinUser username " + builtinUser.getUserName());
                        }
                    } else {
                        logger.info("canLogInAsBuiltinUser: User doesn't know old pre-bcrypt password either.");
                    }
                } else {
                    logger.info("canLogInAsBuiltinUser: Couldn't run `check` because no BuiltinUser found with username " + username);
                }
            }
            return null;
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage()).append(" ");
                /**
                 * @todo Investigate why authSvc.authenticate is throwing
                 * NullPointerException. If you convert a Shib user or an OAuth
                 * user to a Builtin user, the password will be null.
                 */
                if (cause instanceof NullPointerException) {
                    for (int i = 0; i < 2; i++) {
                        StackTraceElement stacktrace = cause.getStackTrace()[i];
                        if (stacktrace != null) {
                            String classCanonicalName = stacktrace.getClass().getCanonicalName();
                            String methodName = stacktrace.getMethodName();
                            int lineNumber = stacktrace.getLineNumber();
                            String error = "at " + stacktrace.getClassName() + "." + stacktrace.getMethodName() + "(" + stacktrace.getFileName() + ":" + lineNumber + ") ";
                            sb.append(error);
                        }
                    }
                }
            }
            logger.info("When trying to validate password, exception calling authSvc.authenticate: " + sb.toString());
            return null;
        }
    }
    
    public List <WorkflowComment> getWorkflowCommentsByAuthenticatedUser(AuthenticatedUser user){ 
        Query query = em.createQuery("SELECT wc FROM WorkflowComment wc WHERE wc.authenticatedUser.id = :auid");
        query.setParameter("auid", user.getId());       
        return query.getResultList();
    }

}
