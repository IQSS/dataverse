/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import static jakarta.ejb.LockType.READ;
import static jakarta.ejb.LockType.WRITE;
import jakarta.ejb.Singleton;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Leonid Andreev
 */
/**
 * The AuthenticationProvidersRegistrationService is responsible for registering and listing
 * AuthenticationProviders. There's a single instance per application. 
 * 
 * Register the providers in the {@link #startup()} method.
 */
@Named
@Lock(READ)
@Singleton
public class AuthenticationProvidersRegistrationServiceBean {

    private static final Logger logger = Logger.getLogger(AuthenticationProvidersRegistrationServiceBean.class.getName());
    
    @EJB
    BuiltinUserServiceBean builtinUserServiceBean;

    @EJB
    PasswordValidatorServiceBean passwordValidatorService;
    
    @EJB
    protected ActionLogServiceBean actionLogSvc;
    
    @EJB
    AuthenticationServiceBean authenticationService;
    
    /**
     * The maps below (the objects themselves) are "final", but the
     * values will be populated in @PostConstruct (see below) during 
     * the initialization and in later calls to the service. 
     * This is a @Singleton, so we are guaranteed that there is only
     * one application-wide copy of each of these maps.
     */
    
    /**
     * Authentication Provider Factories:
     */
    final Map<String, AuthenticationProviderFactory> providerFactories = new HashMap<>();

    /**
     * Where all registered authentication providers live.
     */
    final Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();
    
    /**
     * Index of all OAuth2 providers mapped to {@link #authenticationProviders}.
     */
    final Map<String, AbstractOAuth2AuthenticationProvider> oAuth2authenticationProviders = new HashMap<>();
        
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    // does this method also need an explicit @Lock(WRITE)? 
    // - I'm assuming not; since it's guaranteed to only be called once,
    // via @PostConstruct in this @Singleton. -- L.A.
    @PostConstruct
    public void startup() {
        
        // First, set up the factories
        try {
            // @todo: Instead of hard-coding the factories here, consider 
            // using @AutoService - similiarly how we are using with the 
            // metadata Exporter classes. (may not necessarily be possible, or 
            // easy; hence "consider" -- L.A.)
            registerProviderFactory( new BuiltinAuthenticationProviderFactory(builtinUserServiceBean, passwordValidatorService, authenticationService) );
            registerProviderFactory( new ShibAuthenticationProviderFactory() );
            registerProviderFactory( new OAuth2AuthenticationProviderFactory() );
            registerProviderFactory( new OIDCAuthenticationProviderFactory() );
        
        } catch (AuthorizationSetupException ex) { 
            logger.log(Level.SEVERE, "Exception setting up the authentication provider factories: " + ex.getMessage(), ex);
        }
        
        // Now, load the providers.
        em.createNamedQuery("AuthenticationProviderRow.findAllEnabled", AuthenticationProviderRow.class)
                .getResultList().forEach((row) -> {
                    try {
                        registerProvider( loadProvider(row) );
                        
                    } catch ( AuthenticationProviderFactoryNotFoundException e ) {
                        logger.log(Level.SEVERE, "Cannot find authentication provider factory with alias '" + e.getFactoryAlias() + "'",e);
                        
                    } catch (AuthorizationSetupException ex) {
                        logger.log(Level.SEVERE, "Exception setting up the authentication provider '" + row.getId() + "': " + ex.getMessage(), ex);
                    }
        });
    }

    private void registerProviderFactory(AuthenticationProviderFactory aFactory) 
            throws AuthorizationSetupException 
    {
        if ( providerFactories.containsKey(aFactory.getAlias()) ) {
            throw new AuthorizationSetupException(
                    "Duplicate alias " + aFactory.getAlias() + " for authentication provider factory.");
        }
        providerFactories.put( aFactory.getAlias(), aFactory);
        logger.log( Level.FINE, "Registered Authentication Provider Factory {0} as {1}", 
                new Object[]{aFactory.getInfo(), aFactory.getAlias()});
    }
    
    /**
     * Tries to load and {@link AuthenticationProvider} using the passed {@link AuthenticationProviderRow}.
     * @param aRow The row to load the provider from.
     * @return The provider, if successful
     * @throws AuthenticationProviderFactoryNotFoundException If the row specifies a non-existent factory
     * @throws AuthorizationSetupException If the factory failed to instantiate a provider from the row.
     */
    @Lock(WRITE)
    public AuthenticationProvider loadProvider( AuthenticationProviderRow aRow )
                throws AuthenticationProviderFactoryNotFoundException, AuthorizationSetupException {
        AuthenticationProviderFactory fact = providerFactories.get((aRow.getFactoryAlias()));
        
        if ( fact == null ) throw new AuthenticationProviderFactoryNotFoundException(aRow.getFactoryAlias());
        
        return fact.buildProvider(aRow);
    }
    
    @Lock(WRITE)
    public void registerProvider(AuthenticationProvider aProvider) throws AuthorizationSetupException {
        if ( authenticationProviders.containsKey(aProvider.getId()) ) {
            throw new AuthorizationSetupException(
                    "Duplicate id " + aProvider.getId() + " for authentication provider.");
        }
        authenticationProviders.put( aProvider.getId(), aProvider);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "registerProvider")
            .setInfo(aProvider.getId() + ":" + aProvider.getInfo().getTitle()));
        if ( aProvider instanceof AbstractOAuth2AuthenticationProvider ) {
            oAuth2authenticationProviders.put(aProvider.getId(), (AbstractOAuth2AuthenticationProvider) aProvider);
        }
    }
    
    @Lock(READ)
    public Map<String, AbstractOAuth2AuthenticationProvider> getOAuth2AuthProvidersMap() {
        return oAuth2authenticationProviders;
    }
    
    /*
        the commented-out methods below were moved into this service in 
        the quick patch produced for 4.20; but have been modified and moved 
        back into AuthenticationServiceBean again for v5.0. -- L.A.
    
    @Lock(READ)
    public AbstractOAuth2AuthenticationProvider getOAuth2Provider( String id ) {
        return oAuth2authenticationProviders.get(id);
    }
    
    @Lock(READ)
    public Set<AbstractOAuth2AuthenticationProvider> getOAuth2Providers() {
        return new HashSet<>(oAuth2authenticationProviders.values());
    }*/
    
    @Lock(READ)
    public Map<String, AuthenticationProvider> getAuthenticationProvidersMap() {
        return authenticationProviders;
    }
    
    @Lock(WRITE)
    public void deregisterProvider( String id ) {
        oAuth2authenticationProviders.remove( id );
        if ( authenticationProviders.remove(id) != null ) {
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "deregisterProvider")
                .setInfo(id));
            logger.log(Level.INFO,"Deregistered provider {0}", new Object[]{id});
            logger.log(Level.INFO,"Providers left {0}", new Object[]{authenticationProviders.values()});
        }
    }
    
    /*
    @Lock(READ)
    public Set<String> getAuthenticationProviderIds() {
        return authenticationProviders.keySet();
    }

    @Lock(READ)
    public Collection<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders.values();
    }
    
    @Lock(READ)
    public <T extends AuthenticationProvider> Set<String> getAuthenticationProviderIdsOfType( Class<T> aClass ) {
        // @todo: remove this!
        //logger.info("inside getAuthenticationProviderIdsOfType and sleeping for 20 seconds");
        //try {
        //    Thread.sleep(20000);
        //} catch (Exception ex) {
        //    logger.warning("Failed to sleep for 20 seconds.");
        //}
        Set<String> retVal = new TreeSet<>();
        for ( Map.Entry<String, AuthenticationProvider> p : authenticationProviders.entrySet() ) {
            if ( aClass.isAssignableFrom( p.getValue().getClass() ) ) {
                retVal.add( p.getKey() );
            }
        }
        //logger.info("done with getAuthenticationProviderIdsOfType.");
        return retVal;
    }
    */
    
    @Lock(READ)
    public Map<String, AuthenticationProviderFactory> getProviderFactoriesMap() {
        return providerFactories; 
    }
    
    /*
    @Lock(READ)
    public AuthenticationProviderFactory getProviderFactory( String alias ) {
        return providerFactories.get(alias);
    }
    
    @Lock(READ)
    public AuthenticationProvider getAuthenticationProvider( String id ) {
        //logger.info("inside getAuthenticationProvider()");
        return authenticationProviders.get( id );
    }
    
    @Lock(READ)
    public AuthenticationProvider lookupProvider( AuthenticatedUser user )  {
        return authenticationProviders.get(user.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }

    @Lock(READ)
    public Set<AuthenticationProviderFactory> listProviderFactories() {
        return new HashSet<>( providerFactories.values() ); 
    }
    
    @Lock(READ)
    public boolean isOrcidEnabled() {
        return oAuth2authenticationProviders.values().stream().anyMatch( s -> s.getId().toLowerCase().contains("orcid") );
    }
    */

}
