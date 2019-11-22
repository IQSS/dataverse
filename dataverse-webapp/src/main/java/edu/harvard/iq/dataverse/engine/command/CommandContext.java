package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseLinkingDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.bannersandmessages.messages.DataverseTextMessageServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataverse.template.TemplateDao;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
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
    EntityManager em();

    DataverseEngine engine();

    DvObjectServiceBean dvObjects();

    DatasetDao datasets();

    DataverseDao dataverses();

    DataverseRoleServiceBean roles();

    BuiltinUserServiceBean builtinUsers();

    IndexServiceBean index();

    IndexBatchServiceBean indexBatch();

    SolrIndexServiceBean solrIndex();

    SearchServiceBean search();

    IngestServiceBean ingest();

    PermissionServiceBean permissions();

    RoleAssigneeServiceBean roleAssignees();

    DataverseFacetServiceBean facets();

    FeaturedDataverseServiceBean featuredDataverses();

    DataFileServiceBean files();

    TemplateDao templates();

    SavedSearchServiceBean savedSearches();

    DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels();

    DOIEZIdServiceBean doiEZId();

    DOIDataCiteServiceBean doiDataCite();

    FakePidProviderServiceBean fakePidProvider();

    HandlenetServiceBean handleNet();

    GuestbookServiceBean guestbooks();

    GuestbookResponseServiceBean responses();

    DataverseLinkingDao dvLinking();

    DatasetLinkingServiceBean dsLinking();

    SettingsServiceBean settings();

    ExplicitGroupServiceBean explicitGroups();

    GroupServiceBean groups();

    UserNotificationService notifications();

    AuthenticationServiceBean authentication();

    SystemConfig systemConfig();

    PrivateUrlServiceBean privateUrl();

    DatasetVersionServiceBean datasetVersion();

    WorkflowServiceBean workflows();

    MapLayerMetadataServiceBean mapLayerMetadata();

    DataCaptureModuleServiceBean dataCaptureModule();

    FileDownloadServiceBean fileDownload();

    DataverseTextMessageServiceBean dataverseTextMessages();
}
