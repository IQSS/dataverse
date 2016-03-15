package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * An EJB capable of executing {@link Command}s in a JEE environment.
 *
 * @author michael
 */
@Stateless
@Named
public class EjbDataverseEngine {
    private static final Logger logger = Logger.getLogger(EjbDataverseEngine.class.getCanonicalName());
    
    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    DataverseRoleServiceBean roleService;

    @EJB
    DataverseRoleServiceBean rolesService;

    @EJB
    BuiltinUserServiceBean usersService;

    @EJB
    IndexServiceBean indexService;

    @EJB
    SolrIndexServiceBean solrIndexService;

    @EJB
    SearchServiceBean searchService;

    @EJB
    PermissionServiceBean permissionService;

    @EJB
    DvObjectServiceBean dvObjectService;

    @EJB
    DataverseFacetServiceBean dataverseFacetService;

    @EJB
    FeaturedDataverseServiceBean featuredDataverseService;

    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    TemplateServiceBean templateService;
    
    @EJB
    SavedSearchServiceBean savedSearchService;

    @EJB
    DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels;

    @EJB
    DOIEZIdServiceBean doiEZId;
    
    @EJB
    DOIDataCiteServiceBean doiDataCite;
    
    @EJB
    HandlenetServiceBean handleNet;
    
    @EJB
    SettingsServiceBean settings;
    
    @EJB
    GuestbookServiceBean guestbookService;
    
    @EJB
    GuestbookResponseServiceBean responses;
    
    @EJB
    DataverseLinkingServiceBean dvLinking;
    
    @EJB
    DatasetLinkingServiceBean dsLinking;

    @EJB
    ExplicitGroupServiceBean explicitGroups;
    
    @EJB
    RoleAssigneeServiceBean roleAssignees;
    
    @EJB
    UserNotificationServiceBean userNotificationService;   
    
    @EJB
    AuthenticationServiceBean authentication; 
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    ActionLogServiceBean logSvc;

    private CommandContext ctxt;
    
    @TransactionAttribute(REQUIRES_NEW)
    public <R> R submitInNewTransaction(Command<R> aCommand) throws CommandException {
        return submit(aCommand);
    }

    public <R> R submit(Command<R> aCommand) throws CommandException {
        
        final ActionLogRecord logRec = new ActionLogRecord(ActionLogRecord.ActionType.Command, aCommand.getClass().getCanonicalName());
        
        try {
            logRec.setUserIdentifier( aCommand.getRequest().getUser().getIdentifier() );
            
            // Check permissions - or throw an exception
            Map<String, ? extends Set<Permission>> requiredMap = aCommand.getRequiredPermissions();
            if (requiredMap == null) {
                throw new RuntimeException("Command " + aCommand + " does not define required permissions.");
            }

            DataverseRequest dvReq = aCommand.getRequest();
            
            Map<String, DvObject> affectedDvObjects = aCommand.getAffectedDvObjects();
            logRec.setInfo( describe(affectedDvObjects) );
            for (Map.Entry<String, ? extends Set<Permission>> pair : requiredMap.entrySet()) {
                String dvName = pair.getKey();
                if (!affectedDvObjects.containsKey(dvName)) {
                    throw new RuntimeException("Command instance " + aCommand + " does not have a DvObject named '" + dvName + "'");
                }
                DvObject dvo = affectedDvObjects.get(dvName);

                Set<Permission> granted = (dvo != null) ? permissionService.permissionsFor(dvReq, dvo)
                        : EnumSet.allOf(Permission.class);
                Set<Permission> required = requiredMap.get(dvName);
                
                if (!granted.containsAll(required)) {
                    required.removeAll(granted);
                    logRec.setActionResult(ActionLogRecord.Result.PermissionError);
                    throw new PermissionException("Can't execute command " + aCommand
                            + ", because request " + aCommand.getRequest()
                            + " is missing permissions " + required
                            + " on Object " + dvo.accept(DvObject.NamePrinter),
                            aCommand,
                            required, dvo);
                }
            }
            try {
                return aCommand.execute(getContext());
                
            } catch ( EJBException ejbe ) {
                logRec.setActionResult(ActionLogRecord.Result.InternalError);                
                throw new CommandException("Command " + aCommand.toString() + " failed: " + ejbe.getMessage(), ejbe.getCausedByException(), aCommand);
            }
            
        } catch ( RuntimeException re ) {
            logRec.setActionResult(ActionLogRecord.Result.InternalError);
            logRec.setInfo( re.getMessage() );   
            
            Throwable cause = re;          
            while (cause != null) {
                if (cause instanceof ConstraintViolationException) {
                    StringBuilder sb = new StringBuilder(); 
                    sb.append("Unexpected bean validation constraint exception:"); 
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
                    }
                    logger.log(Level.SEVERE, sb.toString());
                    // set this more detailed info in action log
                    logRec.setInfo( sb.toString() );
                }
                cause = cause.getCause();
            }           
            
            throw re;
            
        } finally {
            if ( logRec.getActionResult() == null ) {
                logRec.setActionResult( ActionLogRecord.Result.OK );
            }
            logRec.setEndTime( new java.util.Date() );
            logSvc.log(logRec);
        }
    }

    public CommandContext getContext() {
        if (ctxt == null) {
            ctxt = new CommandContext() {
                
                @Override
                public DatasetServiceBean datasets() {
                    return datasetService;
                }

                @Override
                public DataverseServiceBean dataverses() {
                    return dataverseService;
                }

                @Override
                public DataverseRoleServiceBean roles() {
                    return rolesService;
                }

                @Override
                public BuiltinUserServiceBean builtinUsers() {
                    return usersService;
                }

                @Override
                public IndexServiceBean index() {
                    return indexService;
                }

                @Override
                public SolrIndexServiceBean solrIndex() {
                    return solrIndexService;
                }

                @Override
                public SearchServiceBean search() {
                    return searchService;
                }

                @Override
                public PermissionServiceBean permissions() {
                    return permissionService;
                }

                @Override
                public DvObjectServiceBean dvObjects() {
                    return dvObjectService;
                }

                @Override
                public DataFileServiceBean files() {
                    return dataFileService;
                }

                @Override
                public EntityManager em() {
                    return em;
                }

                @Override
                public DataverseFacetServiceBean facets() {
                    return dataverseFacetService;
                }

                @Override
                public FeaturedDataverseServiceBean featuredDataverses() {
                    return featuredDataverseService;
                }

                @Override
                public TemplateServiceBean templates() {
                    return templateService;
                }
                
                @Override
                public SavedSearchServiceBean savedSearches() {
                    return savedSearchService;
                }

                @Override
                public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels() {
                    return fieldTypeInputLevels;
                }

                @Override
                public DOIEZIdServiceBean doiEZId() {
                    return doiEZId;
                }
                
                @Override
                public DOIDataCiteServiceBean doiDataCite() {
                    return doiDataCite;
                }
                
                @Override
                public HandlenetServiceBean handleNet() {
                    return handleNet;
                }

                @Override
                public SettingsServiceBean settings() {
                    return settings;
                }
                
                @Override
                public GuestbookServiceBean guestbooks() {
                    return guestbookService;
                }

                @Override
                public GuestbookResponseServiceBean responses() {
                    return responses;
                }
                
                @Override
                public DataverseLinkingServiceBean dvLinking() {
                    return dvLinking;
                }
                                
                @Override
                public DatasetLinkingServiceBean dsLinking() {
                    return dsLinking;
                }
                @Override
                public DataverseEngine engine() {
                    return new DataverseEngine() {
                        @Override
                        public <R> R submit(Command<R> aCommand) throws CommandException {
                            return EjbDataverseEngine.this.submit(aCommand);
                        }
                    };
                }

                @Override
                public ExplicitGroupServiceBean explicitGroups() {
                    return explicitGroups;
                }

                @Override
                public RoleAssigneeServiceBean roleAssignees() {
                    return roleAssignees;
                }
                
                @Override
                public UserNotificationServiceBean notifications() {
                    return userNotificationService;
                } 
                
                @Override
                public AuthenticationServiceBean authentication() {
                    return authentication;
                } 
                
            };
        }

        return ctxt;
    }
    
    
    private String describe( Map<String, DvObject> dvObjMap ) {
        StringBuilder sb = new StringBuilder();
        for ( Map.Entry<String, DvObject> ent : dvObjMap.entrySet() ) {
            DvObject value = ent.getValue();
            sb.append(ent.getKey()).append(":");
            sb.append( (value!=null) ? value.accept(DvObject.NameIdPrinter) : "<null>");
            sb.append(" ");
        }
        return sb.toString();
    }
}
