package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.echo.EchoAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.faces.application.FacesMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 * The AuthenticationManager is responsible for registering and listing
 * AuthenticationProviders. There's a single instance per application. 
 * 
 * Register the providers in the {@link #startup()} method.
 */
@Singleton
public class AuthenticationServiceBean {
    private static final Logger logger = Logger.getLogger(AuthenticationServiceBean.class.getName());
    
    /**
     * Where all registered authentication providers live.
     */
    final Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();
    
    final Map<String, AuthenticationProviderFactory> providerFactories = new HashMap<>();
    
    @EJB
    BuiltinUserServiceBean builtinUserServiceBean;
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @PostConstruct
    public void startup() {
        
        // First, set up the factories
        try {
            registerProviderFactory( new BuiltinAuthenticationProviderFactory(builtinUserServiceBean) );
            registerProviderFactory( new EchoAuthenticationProviderFactory() );
            /**
             * Register shib provider factory here. Test enable/disable via Admin API, etc.
             */
            new ShibAuthenticationProvider();
        } catch (AuthorizationSetupException ex) {
            logger.log(Level.SEVERE, "Exception setting up the authentication provider factories: " + ex.getMessage(), ex);
        }
        
        // Now, load the providers.
        for ( AuthenticationProviderRow row : 
                em.createNamedQuery("AuthenticationProviderRow.findAllEnabled", AuthenticationProviderRow.class)
                    .getResultList() 
        ) {
            try {
                registerProvider( loadProvider(row) );
                
            } catch ( AuthenticationProviderFactoryNotFoundException e ) {
                logger.log(Level.SEVERE, "Cannot find authentication provider factory with alias '" + e.getFactoryAlias() + "'",e);
                
            } catch (AuthorizationSetupException ex) {
                logger.log(Level.SEVERE, "Exception setting up the authentication provider '" + row.getId() + "': " + ex.getMessage(), ex);
            }
        }
    }
    
    public void registerProviderFactory(AuthenticationProviderFactory aFactory) 
            throws AuthorizationSetupException 
    {
        if ( providerFactories.containsKey(aFactory.getAlias()) ) {
            throw new AuthorizationSetupException(
                    "Duplicate alias " + aFactory.getAlias() + " for authentication provider factory.");
        }
        providerFactories.put( aFactory.getAlias(), aFactory);
        logger.log( Level.INFO, "Registered Authentication Provider Facotry {0} as {1}", 
                new Object[]{aFactory.getInfo(), aFactory.getAlias()});
    }
    
    /**
     * Tries to load and {@link AuthenticationProvider} using the passed {@link AuthenticationProviderRow}.
     * @param aRow The row to load the provider from.
     * @return The provider, if successful
     * @throws AuthenticationProviderFactoryNotFoundException If the row specifies a non-existent factory
     * @throws AuthorizationSetupException If the factory failed to instantiate a provider from the row.
     */
    public AuthenticationProvider loadProvider( AuthenticationProviderRow aRow )
                throws AuthenticationProviderFactoryNotFoundException, AuthorizationSetupException {
        AuthenticationProviderFactory fact = getProviderFactory(aRow.getFactoryAlias());
        
        if ( fact == null ) throw new AuthenticationProviderFactoryNotFoundException(aRow.getFactoryAlias());
        
        return fact.buildProvider(aRow);
    }
    
    public void registerProvider(AuthenticationProvider aProvider) throws AuthorizationSetupException {
        if ( authenticationProviders.containsKey(aProvider.getId()) ) {
            throw new AuthorizationSetupException(
                    "Duplicate id " + aProvider.getId() + " for authentication provider.");
        }
        authenticationProviders.put( aProvider.getId(), aProvider);
        logger.log( Level.INFO, "Registered Authentication Provider {0} as {1}", new Object[]{aProvider.getInfo().getTitle(), aProvider.getId()});
    }

    public void deregisterProvider( String id ) {
        authenticationProviders.remove( id );
        logger.log(Level.INFO,"Deregistered provider {0}", new Object[]{id});
        logger.log(Level.INFO,"Providers left {0}", new Object[]{getAuthenticationProviderIds()});
    }
    
    public Set<String> getAuthenticationProviderIds() {
        return authenticationProviders.keySet();
    }
    
    public <T extends AuthenticationProvider> Set<String> getAuthenticationProviderIdsOfType( Class<T> aClass ) {
        Set<String> retVal = new TreeSet<>();
        for ( Map.Entry<String, AuthenticationProvider> p : authenticationProviders.entrySet() ) {
            if ( aClass.isAssignableFrom( p.getValue().getClass() ) ) {
                retVal.add( p.getKey() );
            }
        }
        return retVal;
    }
    
    public AuthenticationProviderFactory getProviderFactory( String alias ) {
        return providerFactories.get(alias);
    }
    
    public AuthenticationProvider getAuthenticationProvider( String id ) {
        return authenticationProviders.get( id );
    }
    
    public AuthenticatedUser findByID(Object pk){
        if (pk==null){
            return null;
        }
        return (AuthenticatedUser) em.find(AuthenticatedUser.class, pk);
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
     * method/command.
     */
    public void deleteAuthenticatedUser(Object pk) {
        AuthenticatedUser user = em.find(AuthenticatedUser.class, pk);
        
        
        if (user!=null) {
            ApiToken apiToken = findApiTokenByUser(user);
            if (apiToken != null) {
                em.remove(apiToken);
            }
            if (user.isBuiltInUser()) {
                BuiltinUser builtin = builtinUserServiceBean.findByUserName(user.getUserIdentifier());
                em.remove(builtin);
            }
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

    public AuthenticatedUser authenticate( String authenticationProviderId, AuthenticationRequest req ) throws AuthenticationFailedException {
        AuthenticationProvider prv = getAuthenticationProvider(authenticationProviderId);
        if ( prv == null ) throw new IllegalArgumentException("No authentication provider listed under id " + authenticationProviderId );
        
        AuthenticationResponse resp = prv.authenticate(req);
        
        if ( resp.getStatus() == AuthenticationResponse.Status.SUCCESS ) {
            // yay! see if we already have this user.
            AuthenticatedUser user = lookupUser(authenticationProviderId, resp.getUserId());

            return ( user == null ) ?
                createAuthenticatedUser( authenticationProviderId, resp.getUserId(), resp.getUserDisplayInfo() )
                : updateAuthenticatedUser( user, resp.getUserDisplayInfo() );

        } else { 
            throw new AuthenticationFailedException(resp, "Authentication Failed: " + resp.getMessage());
        }
    }
    
    public AuthenticatedUser lookupUser( String authPrvId, String userPersistentId ) {
        AuthenticatedUserLookup lookup = em.find(AuthenticatedUserLookup.class,
                new AuthenticatedUserLookupId(authPrvId, userPersistentId));
        
        return ( lookup != null ) ? lookup.getAuthenticatedUser() : null;
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
        ApiToken apiToken = null;
        TypedQuery<ApiToken> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ApiToken AS o WHERE o.authenticatedUser = :user", ApiToken.class);
        typedQuery.setParameter("user", au);
        try {
            apiToken = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.log(Level.INFO, "When looking up API token for {0} caught {1}", new Object[]{au, ex});
            apiToken = null;
        }
        
        return apiToken;
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
        
        return apiToken;
    }

    public AuthenticatedUser lookupUser( String apiToken ) {
        ApiToken tkn = findApiToken(apiToken);
        if ( tkn == null ) return null;
        
        if ( tkn.isDisabled() ) return null;
        if ( tkn.getExpireTime() != null ) {
            if ( tkn.getExpireTime().before( new Timestamp(new Date().getTime())) ) {
                em.remove(tkn);
                return null;
            }
        }
        
        return tkn.getAuthenticatedUser();
    }
    
    public AuthenticatedUser save( AuthenticatedUser user ) {
        user.setModificationTime(getCurrentTimestamp());
        em.persist(user);
        em.flush();
        return user;
    }
    
    public AuthenticatedUser update( AuthenticatedUser user ) {
        user.setModificationTime(getCurrentTimestamp());
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
     * Creates an authenticated user based on the passed {@code userDisplayInfo}, and 
     * creates a lookup entry for them based on the authP and persistent id.
     * @param authenticationProviderId
     * @param authPrvUserPersistentId
     * @param userDisplayInfo 
     * @return the newly created user.
     */
    public AuthenticatedUser createAuthenticatedUser(String authenticationProviderId, String authPrvUserPersistentId, RoleAssigneeDisplayInfo userDisplayInfo) {
        AuthenticatedUser auus = new AuthenticatedUser();
        auus.applyDisplayInfo(userDisplayInfo);
        
        // we now select a username
        // TODO make a better username selection
            // Better - throw excpetion to the provider, which has a better chance of getting this right.
        // TODO should lock table authenticated users for write here
        if ( identifierExists(authPrvUserPersistentId) ) {
            int i=1;
            String identifier = authPrvUserPersistentId + i;
            while ( identifierExists(identifier) ) {
                i += 1;
            }
            auus.setUserIdentifier(identifier);
        } else {
            auus.setUserIdentifier(authPrvUserPersistentId);
        }
        auus = save( auus );
        // TODO should unlock table authenticated users for write here
        AuthenticatedUserLookup auusLookup = new AuthenticatedUserLookup(authPrvUserPersistentId, authenticationProviderId, auus);
        em.persist( auusLookup );
        auus.setAuthenticatedUserLookup(auusLookup);
        return auus;
    }
    
    private boolean identifierExists( String idtf ) {
        return em.createNamedQuery("AuthenticatedUser.countOfIdentifier", Number.class)
                .setParameter("identifier", idtf)
                .getSingleResult().intValue() > 0;
    }
    
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser user, RoleAssigneeDisplayInfo userDisplayInfo) {
        user.applyDisplayInfo(userDisplayInfo);
        return update(user);
    }

    public List<AuthenticatedUser> findAllAuthenticatedUsers() {
        return em.createNamedQuery("AuthenticatedUser.findAll", AuthenticatedUser.class).getResultList();
    }

    public List<AuthenticatedUser> findSuperUsers() {
        return em.createNamedQuery("AuthenticatedUser.findSuperUsers", AuthenticatedUser.class).getResultList();
    }
    
    
    public Set<AuthenticationProviderFactory> listProviderFactories() {
        return new HashSet<>( providerFactories.values() ); 
    }
    
    public Timestamp getCurrentTimestamp() {
        return new Timestamp(new Date().getTime());
    }

}
