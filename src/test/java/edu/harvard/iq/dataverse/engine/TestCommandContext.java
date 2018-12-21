package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.IndexBatchServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import javax.persistence.EntityManager;

/**
 * A base CommandContext for tests. Provides no-op implementations. Should
 * probably be overridden for actual tests.
 *
 * @author michael
 */
public class TestCommandContext implements CommandContext {

    TestSettingsServiceBean settings = new TestSettingsServiceBean();

    @Override
    public DatasetServiceBean datasets() {
        return null;
    }

    @Override
    public DataverseServiceBean dataverses() {
        return null;
    }

    @Override
    public DataverseRoleServiceBean roles() {
        return null;
    }

    @Override
    public BuiltinUserServiceBean builtinUsers() {
        return null;
    }

    @Override
    public IndexServiceBean index() {
        return null;
    }
        
    @Override
	public IndexBatchServiceBean indexBatch() {
		return null;
	}

    @Override
    public SolrIndexServiceBean solrIndex() {
        return null;
    }

    @Override
    public SearchServiceBean search() {
        return null;
    }

    @Override
    public IngestServiceBean ingest() {
        return null;
    }

    @Override
    public PermissionServiceBean permissions() {
        return null;
    }

    @Override
    public DvObjectServiceBean dvObjects() {
        return null;
    }

    @Override
    public EntityManager em() {
        return null;
    }

    @Override
    public DataverseFacetServiceBean facets() {
        return null;
    }

    @Override
    public FeaturedDataverseServiceBean featuredDataverses() {
        return null;
    }

    @Override
    public TemplateServiceBean templates() {
        return null;
    }

    @Override
    public SavedSearchServiceBean savedSearches() {
        return null;
    }

    @Override
    public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels() {
        return null;
    }

    @Override
    public DOIEZIdServiceBean doiEZId() {
        return null;
    }

    @Override
    public DOIDataCiteServiceBean doiDataCite() {
        return null;
    }

    @Override
    public FakePidProviderServiceBean fakePidProvider() {
        return null;
    }

    @Override
    public HandlenetServiceBean handleNet() {
        return null;
    }

    @Override
    public SettingsServiceBean settings() {
        return settings;
    }

    @Override
    public GuestbookServiceBean guestbooks() {
        return null;
    }

    @Override
    public GuestbookResponseServiceBean responses() {
        return null;
    }

    @Override
    public DataverseLinkingServiceBean dvLinking() {
        return null;
    }

    @Override
    public DatasetLinkingServiceBean dsLinking() {
        return null;
    }

    @Override
    public AuthenticationServiceBean authentication() {
        return null;
    }

    @Override
    public DataverseEngine engine() {
        return new TestDataverseEngine(this);
    }

    @Override
    public DataFileServiceBean files() {
        return null;
    }

    @Override
    public ExplicitGroupServiceBean explicitGroups() {
        return null;
    }
    
    @Override
    public GroupServiceBean groups() {
        return null;
    }

    @Override
    public RoleAssigneeServiceBean roleAssignees() {
        return null;
    }

    @Override
    public UserNotificationServiceBean notifications() {
        return null;
    }

    @Override
    public SystemConfig systemConfig() {
        return null;
    }

    @Override
    public PrivateUrlServiceBean privateUrl() {
        return null;
    }

    @Override
    public DatasetVersionServiceBean datasetVersion() {
        return null;
    }

    @Override
    public WorkflowServiceBean workflows() {
        return null;
    }

    @Override
    public MapLayerMetadataServiceBean mapLayerMetadata() {
        return null;
    }

    @Override
    public DataCaptureModuleServiceBean dataCaptureModule() {
        return null;
    }
    
    @Override
    public FileDownloadServiceBean fileDownload() {
        return null;
    }
}
