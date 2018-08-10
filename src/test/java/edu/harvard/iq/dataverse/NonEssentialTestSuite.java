/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
import edu.harvard.iq.dataverse.api.filesystem.FileRecordJobIT;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBeanIT;
import edu.harvard.iq.dataverse.provenance.ProvInvestigatorTest;
import edu.harvard.iq.dataverse.util.xml.XmlValidatorTest;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author BMartinez
 */
@RunWith(Suite.class)
@Categories.IncludeCategory(NonEssentialTests.class)
@Categories.ExcludeCategory(EssentialTests.class)
@Suite.SuiteClasses({

    //Test Package: edu.harvard.iq.dataverse.worldmapauth
    WorldMapTokenTest.class,
            
    //Test Package: edu.harvard.iq.dataverse.api
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

    //Test Package: edu.harvard.iq.dataverse.datacapturemodule
    DataCaptureModuleServiceBeanIT.class,
    
    //Test Package: edu.harvard.iq.dataverse.provenance
    ProvInvestigatorTest.class,
    
    //TestPackage: edu.harvard.iq.dataverse.api.filesystem
    FileRecordJobIT.class,
    
    //Test Package: edu.harvard.iq.dataverse.util.xml
    XmlValidatorTest.class,
    
})
public class NonEssentialTestSuite {
    
    
}
