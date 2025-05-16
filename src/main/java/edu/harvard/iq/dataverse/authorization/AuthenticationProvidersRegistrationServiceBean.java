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
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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

    private AuthenticationProvider orcidProvider;
    
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
        em.createNamedQuery("AuthenticationProviderRow.findAll", AuthenticationProviderRow.class)
                .getResultList().forEach((row) -> {
                    if(row.isEnabled()) {
                    try {
                        AuthenticationProvider authProvider = loadProvider(row);
                        
                        registerProvider( authProvider );
                        
                        // For shibboleth specifically, we will call shibd to 
                        // look up and cache its EntityId:
                                                
                        if ("shib".equals(authProvider.getId())) {
                            String spEntityId = lookupShibbolethEntityId();
                            logger.info("Looked up the entityId of the shibboleth service provider (via a call to shibd): "
                            +spEntityId);
                            if (spEntityId == null) {
                                // we'll make this educated guess - it may or may not help us later on:
                                spEntityId = SystemConfig.getDataverseSiteUrlStatic() + "/sp";
                            }
                            ((ShibAuthenticationProvider)authProvider).setServiceProviderEntityId(spEntityId);
                        }
                        
                    } catch ( AuthenticationProviderFactoryNotFoundException e ) {
                        logger.log(Level.SEVERE, "Cannot find authentication provider factory with alias '" + e.getFactoryAlias() + "'",e);
                        
                    } catch (AuthorizationSetupException ex) {
                        logger.log(Level.SEVERE, "Exception setting up the authentication provider '" + row.getId() + "': " + ex.getMessage(), ex);
                        }
                    } else {
                        // We still use an ORCID provider that is not enabled for login as a way to
                        // authenticate ORCIDs being added to account profiles
                        Map<String, String> data = OAuth2AuthenticationProviderFactory
                                .parseFactoryData(row.getFactoryData());
                        if ("orcid".equals(data.get("type"))) {
                            try {
                                setOrcidProvider(loadProvider(row));
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Cannot register ORCID provider '" + row.getId());
                            }
                        }
                    }
                });
        // If there is an enabled ORCID provider, we'll still use that in preference to a disabled one (there should only be one but this would handle a case where, for example, someone has a disabled sandbox ORCID provider and a real enabled ORCID provider)
        // Could be changed in the future if there's a need for two different clients for login and adding ORCIDs to profiles
        for (AuthenticationProvider provider : authenticationProviders.values()) {
            if (provider instanceof OrcidOAuth2AP) {
                setOrcidProvider(provider);
            }
        }
        // Add providers registered via MPCONFIG
        if (JvmSettings.OIDC_ENABLED.lookupOptional(Boolean.class).orElse(false)) {
            try {
                registerProvider(OIDCAuthenticationProviderFactory.buildFromSettings());
            } catch (AuthorizationSetupException e) {
                logger.log(Level.SEVERE, "Exception setting up an OIDC auth provider via MicroProfile Config", e);
            }
        }
    }

    private void setOrcidProvider(AuthenticationProvider provider) {
        orcidProvider = provider;
        
    }

    public AuthenticationProvider getOrcidProvider() {
        return orcidProvider;
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
    
    private String lookupShibbolethEntityId() {
        
        String urlString = SystemConfig.getDataverseSiteUrlStatic() + "/Shibboleth.sso/Metadata";
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.warning(ex.toString());
            return null;
        }
        
        if (url == null) {
            logger.warning("url object was null after parsing " + urlString);
            return null;
        }
        
        HttpURLConnection metadataRequest = null;
        try {
            metadataRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            logger.warning(ex.toString());
            return null;
        }
        if (metadataRequest == null) {
            logger.warning("http request was null for a local /Shibboleth.sso/Metadata call");
            return null;
        }
        try {
            metadataRequest.connect();
        } catch (IOException ex) {
            logger.warning(ex.toString());
            return null;
        }
        
        XMLStreamReader xmlr = null;

        try {
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlr =  xmlFactory.createXMLStreamReader(new InputStreamReader((InputStream) metadataRequest.getInputStream()));
            
            while ( xmlr.next() == XMLStreamConstants.COMMENT);
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "EntityDescriptor");
            
            return xmlr.getAttributeValue(null, "entityID");
            
        } catch (IOException ioex) {
            logger.warning("IOException instantiating a stream reader of the /Shibboleth.sso/Metadata output" + ioex.getMessage());
        } catch (XMLStreamException xsex) {
            logger.warning("Failed to parse the xml output of the /Shibboleth.sso/Metadata; " + xsex.getMessage());
        } finally {
            if (xmlr != null) {
                try {
                    logger.fine("closing xml reader");
                    xmlr.close();
                } catch (XMLStreamException xsex) {
                    // we don't care
                }
            }
        }
        return null; 
    }

}
