/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.gargoylesoftware.htmlunit.javascript.host.Console;
import edu.harvard.iq.dataverse.api.AbstractApiBeanTest;
import edu.harvard.iq.dataverse.api.AdminIT;
import edu.harvard.iq.dataverse.api.BatchImportIT;
import edu.harvard.iq.dataverse.api.BuiltinUsersIT;
import edu.harvard.iq.dataverse.api.ConfirmEmailIT;
import edu.harvard.iq.dataverse.api.DataCiteIT;
import edu.harvard.iq.dataverse.api.DatasetsIT;
import edu.harvard.iq.dataverse.api.DataversesIT;
import edu.harvard.iq.dataverse.api.ExternalToolsIT;
import edu.harvard.iq.dataverse.api.FeedbackApiIT;
import edu.harvard.iq.dataverse.api.FilesIT;
import edu.harvard.iq.dataverse.api.GeoconnectIT;
import edu.harvard.iq.dataverse.api.HarvestingServerIT;
import edu.harvard.iq.dataverse.api.InReviewWorkflowIT;
import edu.harvard.iq.dataverse.api.InfoIT;
import edu.harvard.iq.dataverse.api.IpGroupsIT;
import edu.harvard.iq.dataverse.api.LazyRefTest;
import edu.harvard.iq.dataverse.api.MetricsIT;
import edu.harvard.iq.dataverse.api.ProvIT;
import edu.harvard.iq.dataverse.api.S3AccessIT;
import edu.harvard.iq.dataverse.api.SearchIT;
import edu.harvard.iq.dataverse.api.StorageSitesIT;
import edu.harvard.iq.dataverse.api.SwordIT;
import edu.harvard.iq.dataverse.api.TabularIT;
import edu.harvard.iq.dataverse.api.ThumbnailsIT;
import edu.harvard.iq.dataverse.api.UsersIT;
import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.api.dto.FieldDTOTest;
import edu.harvard.iq.dataverse.api.filesystem.FileRecordJobIT;
import edu.harvard.iq.dataverse.authorization.AuthUtilTest;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfoTest;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderTest;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBeanTest;
import edu.harvard.iq.dataverse.authorization.groups.GroupUtilTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4AddressTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv6AddressTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRangeTest;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressTest;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProviderTest;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2AuthenticationProviderFactoryTest;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2APTest;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2APTest;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2APTest;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtilTest;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUserTest;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailUtilTest;
import edu.harvard.iq.dataverse.dataaccess.FileAccessIOTest;
import edu.harvard.iq.dataverse.dataaccess.StorageIOTest;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIOTest;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBeanIT;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtilTest;
import edu.harvard.iq.dataverse.dataset.DatasetUtilTest;
import edu.harvard.iq.dataverse.datasetutility.FileSizeCheckerTest;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParamsTest;
import edu.harvard.iq.dataverse.dataverse.DataverseUtilTest;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.PermissionTest;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.TestEntityManager;
import edu.harvard.iq.dataverse.engine.TestSettingsServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractDatasetCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.ReturnDatasetToAuthorCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitDatasetForReviewCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommandTest;
import edu.harvard.iq.dataverse.engine.command.impl.UpdatePermissionRootCommandTest;
import edu.harvard.iq.dataverse.export.DDIExporterTest;
import edu.harvard.iq.dataverse.export.SchemaDotOrgExporterTest;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtilTest;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtilTest;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandlerTest;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBeanTest;
import edu.harvard.iq.dataverse.externaltools.ExternalToolTest;
import edu.harvard.iq.dataverse.feedback.FeedbackUtilTest;
import edu.harvard.iq.dataverse.ingest.IngestUtilTest;
import edu.harvard.iq.dataverse.ingest.IngestableDataCheckerTest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReaderTest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReaderTest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DataReaderTest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.NewDTAFileReaderTest;
import edu.harvard.iq.dataverse.locality.StorageSiteUtilTest;
import edu.harvard.iq.dataverse.metrics.MetricsUtilTest;
import edu.harvard.iq.dataverse.mocks.MockBuiltinUserServiceBean;
import edu.harvard.iq.dataverse.mocks.MockExplicitGroupService;
import edu.harvard.iq.dataverse.mocks.MockPasswordValidatorServiceBean;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.mydata.PagerTest;
import edu.harvard.iq.dataverse.mydata.SolrQueryFormatterTest;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetDataTest;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtilTest;
import edu.harvard.iq.dataverse.provenance.ProvInvestigatorTest;
import edu.harvard.iq.dataverse.repositorystorageabstractionlayer.RepositoryStorageAbstractionLayerUtilTest;
import edu.harvard.iq.dataverse.search.IndexUtilTest;
import edu.harvard.iq.dataverse.search.SearchFilesServiceBeanTest;
import edu.harvard.iq.dataverse.search.SearchUtilTest;
import edu.harvard.iq.dataverse.search.SolrSearchResultTest;
import edu.harvard.iq.dataverse.util.BitSetTest;
import edu.harvard.iq.dataverse.util.BundleUtilTest;
import edu.harvard.iq.dataverse.util.CollectionLiterals;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrderTest;
import edu.harvard.iq.dataverse.util.FileUtilTest;
import edu.harvard.iq.dataverse.util.JsfHelperTest;
import edu.harvard.iq.dataverse.util.LruCacheTest;
import edu.harvard.iq.dataverse.util.MailUtilTest;
import edu.harvard.iq.dataverse.util.MarkupCheckerTest;
import edu.harvard.iq.dataverse.util.MockResponse;
import edu.harvard.iq.dataverse.util.StringUtilTest;
import edu.harvard.iq.dataverse.util.json.BriefJsonPrinterTest;
import edu.harvard.iq.dataverse.util.json.JsonParserTest;
import edu.harvard.iq.dataverse.util.json.JsonPrinterTest;
import edu.harvard.iq.dataverse.util.json.JsonUtilTest;
import edu.harvard.iq.dataverse.util.shapefile.ShapefileHandlerTest;
import edu.harvard.iq.dataverse.util.xml.XmlPrinterTest;
import edu.harvard.iq.dataverse.util.xml.XmlValidatorTest;
import edu.harvard.iq.dataverse.validation.PasswordValidatorTest;
import edu.harvard.iq.dataverse.validation.PasswordValidatorUtilTest;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author BMartinez
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    //Test Package: edu.harvard.iq.dataverse
    DatasetFieldTypeTest.class,
    EMailValidatorTest.class,
    PersistentIdentifierServiceBeanTest.class,
    DatasetFieldValidatorTest.class,
    DatasetFieldValueValidatorTest.class,
    DataFileServiceBeanTest.class,
    GlobalIdTest.class,
    DatasetVersionTest.class,
    DatasetTest.class,
    UserNameValidatorTest.class,

    //Test Package: edu.harvard.iq.dataverse.api
    AbstractApiBeanTest.class,
    LazyRefTest.class,
    //then some IT tests.. don't think we want these in here.
    AdminIT.class,
    BatchImportIT.class,
    BuiltinUsersIT.class,
    ConfirmEmailIT.class,
    DataCiteIT.class,
    DatasetsIT.class,
    DataversesIT.class,
    ExternalToolsIT.class,
    FeedbackApiIT.class,
    FilesIT.class,
    GeoconnectIT.class,
    HarvestingServerIT.class,
    InReviewWorkflowIT.class,
    InfoIT.class,
    IpGroupsIT.class,
    MetricsIT.class,
    ProvIT.class,
    S3AccessIT.class,
    SearchIT.class,
    StorageSitesIT.class,
    SwordIT.class,
    TabularIT.class,
    ThumbnailsIT.class,
    UsersIT.class,
    UtilIT.class,
    
    
    
    
    //TestPackage: edu.harvard.iq.dataverse.api.dto
    FieldDTOTest.class,
    
    //TestPackage: edu.harvard.iq.dataverse.api.filesystem
    FileRecordJobIT.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization
    AuthUtilTest.class,
    AuthenticatedUserDisplayInfoTest.class,
    AuthenticationProviderTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.groups
    GroupServiceBeanTest.class,
    GroupUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.groups.impl.explicit
    ExplicitGroupTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress
    IpGroupTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip
    IPv4AddressTest.class,
    IPv6AddressTest.class,
    IpAddressRangeTest.class,
    IpAddressTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.providers.builtin
    BuiltinAuthenticationProviderTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.providers.oauth2
    OAuth2AuthenticationProviderFactoryTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.providers.oauth2.impl
    GitHubOAuth2APTest.class,
    GoogleOAuth2APTest.class,
    OrcidOAuth2APTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.providers.shib
    ShibUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.authorization.users
    AuthenticatedUserTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.branding
    BrandingUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.confirmemail
    ConfirmEmailUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.dataaccess
    FileAccessIOTest.class,
    StorageIOTest.class,
    SwiftAccessIOTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.datacapturemodule
    DataCaptureModuleUtilTest.class,
    //There is an IT in here
    DataCaptureModuleServiceBeanIT.class,
    
    //Test Package: edu.harvard.iq.dataverse.dataset
    DatasetUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.datasetutility
    FileSizeCheckerTest.class,
    OptionalFileParamsTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.dataverse
    DataverseUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.engine
    NoOpTestEntityManager.class,
    PermissionTest.class,
    TestCommandContext.class,
    TestDataverseEngine.class,
    TestEntityManager.class,
    TestSettingsServiceBean.class,
    
    //Test Package: edu.harvard.iq.dataverse.engine.command.impl
    AbstractDatasetCommandTest.class,
    CreateDatasetVersionCommandTest.class,
    CreateDataverseCommandTest.class,
    CreatePrivateUrlCommandTest.class,
    CreateRoleCommandTest.class,
    DeletePrivateUrlCommandTest.class,
    GetLatestPublishedDatasetVersionCommandTest.class,
    GetPrivateUrlCommandTest.class,
    MoveDatasetCommandTest.class,
    MoveDataverseCommandTest.class,
    RequestRsyncScriptCommandTest.class,
    RestrictFileCommandTest.class,
    ReturnDatasetToAuthorCommandTest.class,
    SubmitDatasetForReviewCommandTest.class,
    UpdateDatasetThumbnailCommandTest.class,
    UpdatePermissionRootCommandTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.export
    DDIExporterTest.class,
    SchemaDotOrgExporterTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.export.ddi
    DdiExportUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.export.dublincore
    DublinCoreExportUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.externaltools
    ExternalToolHandlerTest.class,
    ExternalToolServiceBeanTest.class,
    ExternalToolTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.feedback
    FeedbackUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.imports
    
    //Test Package: edu.harvard.iq.dataverse.ingest
    IngestUtilTest.class,
    IngestableDataCheckerTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv
    CSVFileReaderTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta
    DTAFileReaderTest.class,
    DataReaderTest.class,
    NewDTAFileReaderTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.locality
    StorageSiteUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.metrics
    MetricsUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.mocks
    MockBuiltinUserServiceBean.class,
    MockExplicitGroupService.class,
    MockPasswordValidatorServiceBean.class,
    MockRoleAssigneeServiceBean.class,
    
    //Test Package: edu.harvard.iq.dataverse.mydata
    PagerTest.class,
    SolrQueryFormatterTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.passwordreset
    PasswordResetDataTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.privateurl
    PrivateUrlUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.provenance
    ProvInvestigatorTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.repositorystorageabstractionlayer
    RepositoryStorageAbstractionLayerUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.search
    IndexUtilTest.class,
    SearchFilesServiceBeanTest.class,
    SearchUtilTest.class,
    SolrSearchResultTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.util
    BitSetTest.class,
    BundleUtilTest.class,
    CollectionLiterals.class,
    FileSortFieldAndOrderTest.class,
    FileUtilTest.class,
    JsfHelperTest.class,
    LruCacheTest.class,
    MailUtilTest.class,
    MarkupCheckerTest.class,
    MockResponse.class,
    StringUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.util.json
    BriefJsonPrinterTest.class,
    JsonParserTest.class,
    JsonPrinterTest.class,
    JsonUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.util.shapefile
    ShapefileHandlerTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.util.xml
    XmlPrinterTest.class,
    XmlValidatorTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.validation
    PasswordValidatorTest.class,
    PasswordValidatorUtilTest.class,
    
    //Test Package: edu.harvard.iq.dataverse.worldmapauth
    WorldMapTokenTest.class
})
public class EssentialTestSuite {
    
}
