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
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Leonid Andreev
 */
/**
 * The AuthenticationManager is responsible for registering and listing
 * AuthenticationProviders. There's a single instance per application. 
 * 
 * Register the providers in the {@link #startup()} method.
 */
@Named
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
     * Where all registered authentication providers live.
     */
    final Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();
    
    /**
     * Index of all OAuth2 providers. They also live in {@link #authenticationProviders}.
     */
    final Map<String, AbstractOAuth2AuthenticationProvider> oAuth2authenticationProviders = new HashMap<>();
    
    final Map<String, AuthenticationProviderFactory> providerFactories = new HashMap<>();
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @PostConstruct
    public void startup() {
        
        // First, set up the factories
        try {
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
    
    public void registerProviderFactory(AuthenticationProviderFactory aFactory) 
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
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "registerProvider")
            .setInfo(aProvider.getId() + ":" + aProvider.getInfo().getTitle()));
        if ( aProvider instanceof AbstractOAuth2AuthenticationProvider ) {
            oAuth2authenticationProviders.put(aProvider.getId(), (AbstractOAuth2AuthenticationProvider) aProvider);
        }
        
    }
    
    public AbstractOAuth2AuthenticationProvider getOAuth2Provider( String id ) {
        return oAuth2authenticationProviders.get(id);
    }
    
    public Set<AbstractOAuth2AuthenticationProvider> getOAuth2Providers() {
        return new HashSet<>(oAuth2authenticationProviders.values());
    }
    
    public void deregisterProvider( String id ) {
        oAuth2authenticationProviders.remove( id );
        if ( authenticationProviders.remove(id) != null ) {
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Auth, "deregisterProvider")
                .setInfo(id));
            logger.log(Level.INFO,"Deregistered provider {0}", new Object[]{id});
            logger.log(Level.INFO,"Providers left {0}", new Object[]{getAuthenticationProviderIds()});
        }
    }
    
    public Set<String> getAuthenticationProviderIds() {
        return authenticationProviders.keySet();
    }

    public Collection<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders.values();
    }
    
    public <T extends AuthenticationProvider> Set<String> getAuthenticationProviderIdsOfType( Class<T> aClass ) {
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
    
    public AuthenticationProviderFactory getProviderFactory( String alias ) {
        return providerFactories.get(alias);
    }
    
    public AuthenticationProvider getAuthenticationProvider( String id ) {
        //logger.info("inside getAuthenticationProvider()");
        return authenticationProviders.get( id );
    }
    
    public AuthenticationProvider lookupProvider( AuthenticatedUser user )  {
        return authenticationProviders.get(user.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }

    public Set<AuthenticationProviderFactory> listProviderFactories() {
        return new HashSet<>( providerFactories.values() ); 
    }
    
    public boolean isOrcidEnabled() {
        return oAuth2authenticationProviders.values().stream().anyMatch( s -> s.getId().toLowerCase().contains("orcid") );
    }

}
