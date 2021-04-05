package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.IndexBatchServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.log4j.lf5.LogLevel;

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
    DataverseRoleServiceBean rolesService;

    @EJB
    BuiltinUserServiceBean usersService;

    @EJB
    IndexServiceBean indexService;
    
    @EJB
    IndexBatchServiceBean indexBatchService;

    @EJB
    SolrIndexServiceBean solrIndexService;

    @EJB
    SearchServiceBean searchService;
    
    @EJB
    IngestServiceBean ingestService;

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
    FakePidProviderServiceBean fakePidProvider;

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
    GroupServiceBean groups;

    @EJB
    RoleAssigneeServiceBean roleAssignees;
    
    @EJB
    UserNotificationServiceBean userNotificationService;   
    
    @EJB
    AuthenticationServiceBean authentication; 

    @EJB
    SystemConfig systemConfig;

    @EJB
    PrivateUrlServiceBean privateUrlService;

    @EJB
    DatasetVersionServiceBean datasetVersionService;

    @EJB
    DataCaptureModuleServiceBean dataCaptureModule;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    ActionLogServiceBean logSvc;
    
    @EJB
    WorkflowServiceBean workflowService;
    
    @EJB
    FileDownloadServiceBean fileDownloadService;
    
    @EJB
    ConfirmEmailServiceBean confirmEmailService;
    
    @EJB
    EjbDataverseEngineInner innerEngine;
    
    
    @Resource
    EJBContext ejbCtxt;

    private CommandContext ctxt;
    
    @TransactionAttribute(REQUIRES_NEW)
    public <R> R submitInNewTransaction(Command<R> aCommand) throws CommandException {
        return submit(aCommand);
    }
    
    private DvObject getRetType(Object r){

        return (DvObject) r;
       
    }


    @TransactionAttribute(SUPPORTS)
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

            AuthenticatedUser authenticatedUser = dvReq.getAuthenticatedUser();
            if (authenticatedUser != null) {
                AuthenticatedUser auFreshLookup = authentication.findByID(authenticatedUser.getId());
                if (auFreshLookup == null) {
                    logger.fine("submit method found user no longer exists (was deleted).");
                    throw new CommandException(BundleUtil.getStringFromBundle("command.exception.user.deleted", Arrays.asList(aCommand.getClass().getSimpleName())), aCommand);
                } else {
                    if (auFreshLookup.isDeactivated()) {
                        logger.fine("submit method found user is deactivated.");
                        throw new CommandException(BundleUtil.getStringFromBundle("command.exception.user.deactivated", Arrays.asList(aCommand.getClass().getSimpleName())), aCommand);
                    }
                }
            }

            Map<String, DvObject> affectedDvObjects = aCommand.getAffectedDvObjects();
            logRec.setInfo(aCommand.describe());
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
                    /**
                     * @todo Is there any harm in showing the "granted" set
                     * since we already show "required"? It would help people
                     * reason about the mismatch.
                     */
                    throw new PermissionException("Can't execute command " + aCommand
                            + ", because request " + aCommand.getRequest()
                            + " is missing permissions " + required
                            + " on Object " + dvo.accept(DvObject.NamePrinter),
                            aCommand,
                            required, dvo);
                }
            }
            try {
                if (getContext().getCommandsCalled() == null){
                    getContext().beginCommandSequence();
                }
                getContext().addCommand(aCommand);
                //This list of commands is held by the outermost command's context
                //to be run on completeCommand method when the outermost command is completed
                Stack<Command> previouslyCalled = getContext().getCommandsCalled();
                R r = innerEngine.submit(aCommand, getContext());   
                if (getContext().getCommandsCalled().empty() && !previouslyCalled.empty()){
                    for (Command c: previouslyCalled){
                        getContext().getCommandsCalled().add(c);
                    }
                }
                //This runs the onSuccess Methods for all commands in the stack when the outermost command completes
                this.completeCommand(aCommand, r, getContext().getCommandsCalled());
                return r;
                
            } catch ( EJBException ejbe ) {
                throw new CommandException("Command " + aCommand.toString() + " failed: " + ejbe.getMessage(), ejbe.getCausedByException(), aCommand);
            } 
        } catch (CommandException cmdEx) {
            if (!(cmdEx instanceof PermissionException)) {            
                logRec.setActionResult(ActionLogRecord.Result.InternalError); 
            } 
            logRec.setInfo(logRec.getInfo() + " (" + cmdEx.getMessage() +")");
            throw cmdEx;
        } catch ( RuntimeException re ) {
            logRec.setActionResult(ActionLogRecord.Result.InternalError);
            logRec.setInfo(logRec.getInfo() + " (" + re.getMessage() +")");   
            
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
                    logRec.setInfo(logRec.getInfo() + " (" +  sb.toString() +")");
                }
                cause = cause.getCause();
            }           
            
            throw re;
            
        } finally {
            //when we get here we need to wipe out the command list so that
            //failed commands don't have their onSuccess methods run.
            getContext().cancelCommandSequence();
            if (logRec.getActionResult() == null) {
                logRec.setActionResult(ActionLogRecord.Result.OK);
            } else {
                try{
                     ejbCtxt.setRollbackOnly();
                } catch (IllegalStateException isEx){
                    //Not in a transaction nothing to rollback
                }                  
            }
            logRec.setEndTime(new java.util.Date());
            logSvc.log(logRec);
        }
    }
    
    protected void completeCommand(Command command, Object r, Stack<Command> called) {
        
        if (called.isEmpty()){
            return;
        }
        
        Command test = called.get(0);
        if (!test.equals(command)) {
            //if it's not the first command on the stack it must be an "inner" command
            //and we don't want to run its onSuccess until all commands have comepleted successfully
            return;
        }
        
        for (Command commandLoop : called) {
           commandLoop.onSuccess(ctxt, r);
        }
        
    }
    

    public CommandContext getContext() {
        if (ctxt == null) {
            ctxt = new CommandContext() {

                public Stack<Command> commandsCalled;

                @Override
                public void addCommand(Command command) {

                    if (logger.isLoggable(Level.FINE) && !commandsCalled.isEmpty()) {
                        int instance = (int) (100 * Math.random());
                        try {
                            logger.fine("Current Command Stack (" + instance + "): ");
                            commandsCalled.forEach((c) -> {
                                logger.fine("Command (" + instance + "): " + c.getClass().getSimpleName()
                                        + "for DvObjects");
                                for (Map.Entry<String, DvObject> e : ((Map<String, DvObject>) c.getAffectedDvObjects())
                                        .entrySet()) {
                                    logger.fine("(" + instance + "): " + e.getKey() + " : " + e.getValue().getId());
                                }
                            });
                            logger.fine("Adding command(" + instance + "): " + command.getClass().getSimpleName()
                                    + " for DvObjects");
                            for (Map.Entry<String, DvObject> e : ((Map<String, DvObject>) command
                                    .getAffectedDvObjects()).entrySet()) {
                                logger.fine(e.getKey() + " : " + e.getValue().getId());
                            }
                        } catch (Exception e) {
                            logger.fine("Exception logging command stack(" + instance + "): " + e.getMessage());
                        }
                    }
					commandsCalled.push(command);
                }
                
                
                @Override
                public Stack<Command> getCommandsCalled(){
                    return commandsCalled;
                }
                
                
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
                public IndexBatchServiceBean indexBatch() {
                    return indexBatchService;
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
                public IngestServiceBean ingest() {
                    return ingestService;
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
                public FakePidProviderServiceBean fakePidProvider() {
                    return fakePidProvider;
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
                public GroupServiceBean groups() {
                    return groups;
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

                @Override
                public SystemConfig systemConfig() {
                    return systemConfig;
                }

                @Override
                public PrivateUrlServiceBean privateUrl() {
                    return privateUrlService;
                }

                @Override
                public DatasetVersionServiceBean datasetVersion() {
                    return datasetVersionService;
                }
                
                @Override
                public WorkflowServiceBean workflows() {
                    return workflowService;
                }

                @Override
                public DataCaptureModuleServiceBean dataCaptureModule() {
                    return dataCaptureModule;
                }
                
                @Override
                public FileDownloadServiceBean fileDownload() {
                    return fileDownloadService;
                }
                
                @Override
                public ConfirmEmailServiceBean confirmEmail() {
                    return confirmEmailService;
                }
                
                @Override
                public ActionLogServiceBean actionLog() {
                    return logSvc;
                }

                @Override
                public void beginCommandSequence() {
                    this.commandsCalled = new Stack();
                }

                @Override
                public boolean completeCommandSequence(Command command) {
                    this.commandsCalled.clear();
                    return true;
                }

                @Override
                public void cancelCommandSequence() {
                    this.commandsCalled = new Stack();
                }

            };
        }

        return ctxt;
    }

}
