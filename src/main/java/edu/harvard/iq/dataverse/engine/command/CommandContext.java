package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsValidator;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.IndexBatchServiceBean;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.storageuse.StorageUseServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import java.util.Stack;
import jakarta.persistence.EntityManager;

/**
 * An interface for accessing Dataverse's resources, user info etc. Used by the
 * {@link Command} implementations to perform their intended actions.
 * 
 * @author michael
 */
public interface CommandContext {

    /**
     * Note: While this method is not deprecated *yet*, please consider not
     * using it, and using a method on the service bean instead. Using the em
     * directly makes the command less testable.
     *
     * @return the entity manager
     */
    public EntityManager em();

    public DataverseEngine engine();

    public DvObjectServiceBean dvObjects();

    public DatasetServiceBean datasets();

    public DataverseServiceBean dataverses();

    public DataverseRoleServiceBean roles();

    public BuiltinUserServiceBean builtinUsers();

    public IndexServiceBean index();
    
    public IndexBatchServiceBean indexBatch();

    public SolrIndexServiceBean solrIndex();

    public SearchServiceBean search();
    
    public IngestServiceBean ingest();

    public PermissionServiceBean permissions();

    public RoleAssigneeServiceBean roleAssignees();

    public DataverseFacetServiceBean facets();

    public FeaturedDataverseServiceBean featuredDataverses();

    public DataFileServiceBean files();

    public TemplateServiceBean templates();

    public SavedSearchServiceBean savedSearches();

    public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels();

    public PidProviderFactoryBean pidProviderFactory();

    public GuestbookServiceBean guestbooks();

    public GuestbookResponseServiceBean responses();

    public DataverseLinkingServiceBean dvLinking();

    public DatasetLinkingServiceBean dsLinking();

    public SettingsServiceBean settings();

    public ExplicitGroupServiceBean explicitGroups();

    public GroupServiceBean groups();

    public UserNotificationServiceBean notifications();

    public AuthenticationServiceBean authentication();
    
    public StorageUseServiceBean storageUse();

    public SystemConfig systemConfig();

    public PrivateUrlServiceBean privateUrl();

    public DatasetVersionServiceBean datasetVersion();
    
    public WorkflowServiceBean workflows();

    public DataCaptureModuleServiceBean dataCaptureModule();
    
    public FileDownloadServiceBean fileDownload();
    
    public ConfirmEmailServiceBean confirmEmail();
    
    public ActionLogServiceBean actionLog();

    public MetadataBlockServiceBean metadataBlocks();

    public DatasetTypeServiceBean datasetTypes();

    public void beginCommandSequence();
    
    public boolean completeCommandSequence(Command command);
    
    public void cancelCommandSequence();
    
    public Stack<Command> getCommandsCalled();
    
    public void addCommand(Command command);

    public DatasetFieldServiceBean dsField();

    public DataverseFeaturedItemServiceBean dataverseFeaturedItems();

    public DatasetFieldsValidator datasetFieldsValidator();
}
