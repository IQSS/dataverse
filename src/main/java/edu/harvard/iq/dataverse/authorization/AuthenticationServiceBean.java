package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.exceptions.DuplicateAuthenticationProviderException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.echo.EchoAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * The AuthenticationManager is responsible for registering and listing
 * AuthenticationProviders. There's a single instance per application. 
 * 
 * Register the providers in the {@link #startup()} method.
 */
@Singleton
@Startup
public class AuthenticationServiceBean {
    private static final Logger logger = Logger.getLogger(AuthenticationServiceBean.class.getName());
    
    /**
     * Where all registered authentication providers live.
     */
    final Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();
    
    @EJB
    BuiltinUserServiceBean builtinUserServiceBean;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @PostConstruct
    public void startup() {
        try {
            registerProvider( new BuiltinAuthenticationProvider(builtinUserServiceBean) );
            registerProvider( new EchoAuthenticationProvider("echo1") );
            
        } catch (DuplicateAuthenticationProviderException ex) {
            Logger.getLogger(AuthenticationServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void registerProvider(AuthenticationProvider aProvider) throws DuplicateAuthenticationProviderException {
        if ( authenticationProviders.containsKey(aProvider.getId()) ) {
            throw new DuplicateAuthenticationProviderException(aProvider.getId(), 
                    authenticationProviders.get(aProvider.getId()),
                    "Duplicate id " + aProvider.getId() + " for authentication provider.");
        }
        authenticationProviders.put( aProvider.getId(), aProvider);
        logger.log( Level.INFO, "Registered Authentication Provider {0} as {1}", new Object[]{aProvider.getInfo().getTitle(), aProvider.getId()});
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
    
    public AuthenticationProvider getAuthenticationProvider( String id ) {
        return authenticationProviders.get( id );
    }
    
    public AuthenticatedUser findByID(Object pk){
        if (pk==null){
            return null;
        }
        return (AuthenticatedUser) em.find(AuthenticatedUser.class, pk);
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

            //this.setBuiltInProviderFlag(prv, user);
            
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
        em.persist(user);
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

}
