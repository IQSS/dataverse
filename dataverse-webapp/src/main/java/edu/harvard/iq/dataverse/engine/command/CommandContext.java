package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.bannersandmessages.messages.DataverseTextMessageServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.notification.UserNotificationServiceBean;
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

    DatasetServiceBean datasets();

    DataverseServiceBean dataverses();

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

    TemplateServiceBean templates();

    SavedSearchServiceBean savedSearches();

    DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels();

    DOIEZIdServiceBean doiEZId();

    DOIDataCiteServiceBean doiDataCite();

    FakePidProviderServiceBean fakePidProvider();

    HandlenetServiceBean handleNet();

    GuestbookServiceBean guestbooks();

    GuestbookResponseServiceBean responses();

    DataverseLinkingServiceBean dvLinking();

    DatasetLinkingServiceBean dsLinking();

    SettingsServiceBean settings();

    ExplicitGroupServiceBean explicitGroups();

    GroupServiceBean groups();

    UserNotificationServiceBean notifications();

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
