package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import edu.harvard.iq.dataverse.util.xml.XmlValidator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class SiteMapUtilTest {

    @TempDir
    Path tempDir;
    Path tempDocroot;
    
    @BeforeEach
    void setup() throws IOException {
        // NOTE: This might be unsafe for parallel tests, but our @SystemProperty helper does not yet support
        //       lookups from vars or methods.
        System.setProperty("test.filesDir", tempDir.toString());
        this.tempDocroot = tempDir.resolve("docroot");
        Files.createDirectory(tempDocroot);
    }
    
    @AfterEach
    void teardown() {
        System.clearProperty("test.filesDir");
    }
    
    @Test
    void testUpdateSiteMap() throws IOException, ParseException, SAXException {
        // given
        List<Dataverse> dataverses = new ArrayList<>();
        String publishedDvString = "publishedDv1";
        Dataverse publishedDataverse = new Dataverse();
        publishedDataverse.setAlias(publishedDvString);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dvModifiedDate = dateFmt.parse("1955-11-12 22:04:00");
        publishedDataverse.setModificationTime(new Timestamp(dvModifiedDate.getTime()));
        publishedDataverse.setPublicationDate(new Timestamp(dvModifiedDate.getTime()));
        dataverses.add(publishedDataverse);

        List<Dataset> datasets = new ArrayList<>();

        Dataset published = new Dataset();
        published.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.666", "FAKE/published1", null, AbstractDOIProvider.DOI_RESOLVER_URL, null));
        String publishedPid = published.getGlobalId().asString();
        published.setPublicationDate(new Timestamp(new Date().getTime()));
        published.setModificationTime(new Timestamp(new Date().getTime()));
        datasets.add(published);

        Dataset unpublished = new Dataset();
        unpublished.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.666", "FAKE/unpublished1", null, AbstractDOIProvider.DOI_RESOLVER_URL, null));
        String unpublishedPid = unpublished.getGlobalId().asString();

        Timestamp nullPublicationDateToIndicateNotPublished = null;
        unpublished.setPublicationDate(nullPublicationDateToIndicateNotPublished);
        datasets.add(unpublished);

        Dataset harvested = new Dataset();
        harvested.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.666", "FAKE/harvested1", null, AbstractDOIProvider.DOI_RESOLVER_URL, null));
        String harvestedPid = harvested.getGlobalId().asString();
        harvested.setPublicationDate(new Timestamp(new Date().getTime()));
        harvested.setHarvestedFrom(new HarvestingClient());
        datasets.add(harvested);

        Dataset deaccessioned = new Dataset();
        deaccessioned.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.666", "FAKE/deaccessioned1", null, AbstractDOIProvider.DOI_RESOLVER_URL, null));
        String deaccessionedPid = deaccessioned.getGlobalId().asString();

        deaccessioned.setPublicationDate(new Timestamp(new Date().getTime()));
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        datasetVersions.add(datasetVersion);
        deaccessioned.setVersions(datasetVersions);
        datasets.add(deaccessioned);
        
        // when
        SiteMapUtil.updateSiteMap(dataverses, datasets);
        
        // then
        String pathToSiteMap = tempDocroot.resolve("sitemap").resolve("sitemap.xml").toString();
        assertDoesNotThrow(() -> XmlValidator.validateXmlWellFormed(pathToSiteMap));
        assertTrue(XmlValidator.validateXmlSchema(pathToSiteMap, new URL("https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd")));

        File sitemapFile = new File(pathToSiteMap);
        String sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath()))));
        //System.out.println("sitemap: " + sitemapString);

        assertTrue(sitemapString.contains("1955-11-12"));
        assertTrue(sitemapString.contains(publishedPid));
        assertFalse(sitemapString.contains(unpublishedPid));
        assertFalse(sitemapString.contains(harvestedPid));
        assertFalse(sitemapString.contains(deaccessionedPid));

    }

}
