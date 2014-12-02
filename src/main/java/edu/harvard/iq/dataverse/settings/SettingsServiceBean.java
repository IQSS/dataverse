package edu.harvard.iq.dataverse.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Service bean accessing a persistent hash map, used as settings in the application.
 * @author michael
 */
@Stateless
@Named
public class SettingsServiceBean {
    
    /**
     * Some convenient keys for the settings. Note that the setting's 
     * name is really a {@code String}, but it's good to have the compiler look
     * over your shoulder when typing strings in various places of a large app. 
     * So there.
     */
    public enum Key {
        /**
         * A boolean defining if indexing and search should respect the concept
         * of "permission root".
         *
         * <p>
         *
         * If we ignore permissionRoot at index time, we should blindly give
         * search ("discoverability") access to people and group who have access
         * defined in a parent dataverse, all the way back to the root.
         *
         * <p>
         *
         * If we respect permissionRoot, this means that the dataverse being
         * indexed is an island of permissions all by itself. We should not look
         * to its parent to see if more people and groups might be able to
         * search the DvObjects within it. We would assume no implicit
         * inheritance of permissions. In this mode, all permissions must be
         * explicitly defined on DvObjects. No implied inheritance.
         *
         */
        SearchRespectPermissionRoot,
        /** Solr hostname and port, such as "localhost:8983". */
        SolrHostColonPort,
        /** Key for limiting the number of bytes uploaded via the Data Deposit API. */
        DataDepositApiMaxUploadInBytes,
        /** Key for if Shibboleth is enabled or disabled. */
        ShibEnabled,
        /** Key for the url to send users who want to sign up to. */
        SignUpUrl,
        /** Key for whether we allow users to sign up */
        AllowSignUp,
        /** protocol for global id */
        Protocol,
        /** authority for global id */
        Authority,
        /** DoiProvider for global id */
        DoiProvider,
        DoiSeparator,
        /* Removed for now - tried to add here but DOI Service Bean didn't like it at start-up
        DoiUsername,
        DoiPassword,
        DoiBaseurlstring,
        */
        /* TwoRavens location */
        TwoRavensUrl,
        /* zip download size limit */
        ZipDonwloadLimit;
        
        @Override
        public String toString() {
            return ":" + name();
        }
    }
    
    @PersistenceContext
    EntityManager em;
    
    /**
     * Values that are considered as "true".
     * @see #isTrue(java.lang.String, boolean) 
     */
    private static final Set<String> trueValues = Collections.unmodifiableSet(
            new TreeSet<>( Arrays.asList("1","yes", "true","allow")));
    
    /**
     * Basic functionality - get the name, return the setting, or {@code null}.
     * @param name of the setting
     * @return the actual setting, or {@code null}.
     */
    public String get( String name ) {
        Setting s = em.find( Setting.class, name );
        return (s!=null) ? s.getContent() : null;
    }
    
    /**
     * Same as {@link #get(java.lang.String)}, but with static checking.
     * @param key Enum value of the name.
     * @return The setting, or {@code null}.
     */
    public String getValueForKey( Key key ) {
        return get(key.toString());
    }
    
    /**
     * Return the value stored, or the default value, in case no setting by that
     * name exists. The main difference between this method and the other {@code get()}s
     * is that is never returns null (unless {@code defaultValue} is {@code null}.
     * 
     * @param name Name of the setting.
     * @param defaultValue The value to return if no setting is found in the DB.
     * @return Either the stored value, or the default value.
     */
    public String get( String name, String defaultValue ) {
        String val = get(name);
        return (val!=null) ? val : defaultValue;
    }
    
    public String getValueForKey( Key key, String defaultValue ) {
        return get( key.toString(), defaultValue );
    }
    
    public Setting set( String name, String content ) {
        Setting s = new Setting( name, content );
        s = em.merge(s);
        return s;
    }
    
    public Setting setValueForKey( Key key, String content ) {
        Setting s = new Setting( key.toString(), content );
        s = em.merge(s);
        return s;
    }
    
    /**
     * The correct way to decide whether a string value in the
     * settings table should be considered as {@code true}.
     * @param name name of the setting.
     * @param defaultValue logical value of {@code null}.
     * @return boolean value of the setting.
     */
    public boolean isTrue( String name, boolean defaultValue ) {
        String val = get(name);
        return ( val==null ) ? defaultValue : trueValues.contains(val.trim().toLowerCase() );
    }
    
    public boolean isTrueForKey( Key key, boolean defaultValue ) {
        return isTrue( key.toString(), defaultValue );
    }
            
    public void deleteValueForKey( Key name ) {
        delete( name.toString() );
    }
    
    public void delete( String name ) {
        em.createNamedQuery("Setting.deleteByName")
                .setParameter("name", name)
                .executeUpdate();
    }
    
    public Set<Setting> listAll() {
        return new HashSet<>(em.createNamedQuery("Setting.findAll", Setting.class).getResultList());
    }
    
}
