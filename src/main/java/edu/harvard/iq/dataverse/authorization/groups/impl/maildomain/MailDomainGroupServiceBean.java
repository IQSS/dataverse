package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import org.ocpsoft.rewrite.config.Not;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.NotFoundException;

/**
 * A bean providing the {@link MailDomainGroupProvider}s with container services, such as database connectivity.
 * Also containing the business logic to decide about matching groups.
 */
@Named
@Singleton
@Startup
public class MailDomainGroupServiceBean {
    
    private static final Logger logger = Logger.getLogger(edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean.class.getName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @Inject
    ConfirmEmailServiceBean confirmEmailSvc;
    @Inject
    ActionLogServiceBean actionLogSvc;
    @Resource
    TimerService timerSvc;
	
    MailDomainGroupProvider provider;
    List<MailDomainGroup> groups = Collections.EMPTY_LIST;
    Map<MailDomainGroup, Pattern> groupRegexes = new HashMap<>();
    
    @PostConstruct
    void setup() {
        provider = new MailDomainGroupProvider(this);
        // execute timer every 60 secs, waiting an initial 10 msecs.
        timerSvc.createTimer(10, 60000, null);
    }
    
    /**
     * Update the groups from the database (triggered every 60 seconds).
     * This is done because regex compilation is an expensive operation and should be cached.
     * Regex compilation happens in the model {@link MailDomainGroup} during deserialization from the DB.
     */
    @Timeout
    public void updateGroups() {
        this.groups = findAll();
        
        this.groupRegexes = this.groups.stream()
            .filter(MailDomainGroup::isRegEx)
            .collect(Collectors.toMap(
                mg -> mg,
                mg -> Pattern.compile(mg.getEmailDomains().replace(";","|"))
            ));
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
            
            Set<MailDomainGroup> result = this.groups.stream()
                                                     .filter(mg -> mg.getEmailDomainsAsList().contains(domain))
                                                     .collect(Collectors.toSet());
            result.addAll(this.groups.stream()
                                     .filter(MailDomainGroup::isRegEx)
                                     .filter(mg -> groupRegexes.get(mg).matcher(domain).matches())
                                     .collect(Collectors.toSet()));
            return result;
            
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
    
    /**
     * Update an existing instance (if found) or create a new (if groupName = null or groupName matches alias of grp).
     * This method is idempotent.
     * This being an EJB bean makes this method transactional, rolling back on unchecked exceptions.
     * @param groupAlias String with the group alias of the group to update or empty if new entity
     * @param grp The group to update or add
     * @return The saved entity, including updated group provider attribute
     * @throws NotFoundException if groupName does not match both a group in database and the alias of the provided group
     */
    public MailDomainGroup saveOrUpdate(Optional<String> groupAlias, MailDomainGroup grp ) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "mailDomainCreate");
        alr.setInfo(grp.getIdentifier());
        
        // groupAlias present means PUT means idempotence.
        if (groupAlias.isPresent()) {
            Optional<MailDomainGroup> old = findByAlias(groupAlias.get());
    
            // if an old instance is found, update:
            // (triggering persistence once we leave the function)
            if (old.isPresent()) {
                old.get().update(grp);
                
                alr.setActionSubType("mailDomainUpdate");
                actionLogSvc.log( alr );
                
                return grp;
            }
    
            // otherwise check if path param and supplied group match. (so people use it according to RFC-2616)
            // if not -> throw exception!
            if (!groupAlias.get().equals(grp.getPersistedGroupAlias())) {
                throw new NotFoundException();
            }
        }
        // or add new ...
        em.persist(grp);
        actionLogSvc.log( alr );
        
        return grp;
    }
    
    /**
     * Delete a mail domain group if exists.
     * @param groupAlias
     * @throws NotFoundException if no group with given groupAlias exists.
     */
    public void delete(String groupAlias) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "mailDomainDelete");
        alr.setInfo(groupAlias);
    
        Optional<MailDomainGroup> tbd = findByAlias(groupAlias);
        em.remove(tbd.orElseThrow(() -> new NotFoundException("Cannot find a group with alias "+groupAlias)));
        actionLogSvc.log( alr );
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
