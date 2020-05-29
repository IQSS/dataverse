package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * A bean providing the {@link MailDomainGroupProvider}s with container services, such as database connectivity.
 * Also containing the business logic to decide about matching groups.
 */
@Named
@Stateless
public class MailDomainGroupServiceBean {
    
    private static final Logger logger = Logger.getLogger(edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean.class.getName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @Inject
    ConfirmEmailServiceBean confirmEmailSvc;
	
    MailDomainGroupProvider provider;
    
    @PostConstruct
    void setup() {
        provider = new MailDomainGroupProvider(this);
    }
    
    public MailDomainGroupProvider getProvider() {
        return provider;
    }
    
    /**
     * Find groups for users mail address. Only done when email has been verified.
     * @param user
     * @return A collection of groups with matching email domains
     */
    public Set<MailDomainGroup> findAllWithDomain(AuthenticatedUser user) {
        
        // if the mail address is not verified, escape...
        if (!confirmEmailSvc.hasVerifiedEmail(user)) { return Collections.emptySet(); }
        
        // otherwise start to bisect the mail and lookup groups.
        // NOTE: the email from the user has been validated via {@link EMailValidator} when persisted.
        Optional<String> oDomain = getDomainFromMail(user.getEmail());
        if ( oDomain.isPresent() ) {
            // transform to lowercase, in case someone uses uppercase letters. (we store the comparison values in lowercase)
            String domain = oDomain.get().toLowerCase();
            
            // get all groups and filter
            List<MailDomainGroup> rs = em.createNamedQuery("MailDomainGroup.findAll", MailDomainGroup.class).getResultList();
            for(MailDomainGroup g : rs) {
                if ( g.getEmailDomainsAsList().contains(domain) == false ) rs.remove(g);
            }
            
            return new HashSet<>(rs);
        }
        return Collections.emptySet();
    }
    
    /**
     * Get all mail domain groups from the database.
     * @return A result list from the database. May be null if no results found.
     */
    public List<MailDomainGroup> findAll() {
        return em.createNamedQuery("MailDomainGroup.findAll", MailDomainGroup.class).getResultList();
    }
    
    /**
     * Find a specific mail domain group by it's alias.
     * @param groupAlias
     * @return
     */
    Optional<MailDomainGroup> findByAlias(String groupAlias) {
        try  {
            return Optional.of(
                em.createNamedQuery("MailDomainGroup.findByPersistedGroupAlias", MailDomainGroup.class)
                    .setParameter("persistedGroupAlias", groupAlias)
                    .getSingleResult());
        } catch ( NoResultException nre ) {
            return Optional.empty();
        }
    }
    
    /*
    public MailDomainGroup persist( MailDomainGroup g ) {
        if ( g.getId() == null ) {
            em.persist( g );
            return g;
        } else {
            // clean stale data once in a while
            if ( Math.random() >= 0.5 ) {
                Set<String> stale = new TreeSet<>();
                for ( String idtf : g.getContainedRoleAssignees()) {
                    if ( roleAssigneeSvc.getRoleAssignee(idtf) == null ) {
                        stale.add(idtf);
                    }
                }
                if ( ! stale.isEmpty() ) {
                    g.getContainedRoleAssignees().removeAll(stale);
                }
            }
            
            return em.merge( g );
        }
    }
    */
    
    public void removeGroup(MailDomainGroup mailDomainGroup) {
        em.remove( mailDomainGroup );
    }
    
    /**
     * Retrieve the domain part only from a given email.
     * @param email
     * @return Domain part or empty Optional
     */
    static Optional<String> getDomainFromMail(String email) {
        String[] parts = email.split("@");
        if (parts.length < 2) { return Optional.empty(); }
        return Optional.of(parts[parts.length-1]);
    }
    
}
