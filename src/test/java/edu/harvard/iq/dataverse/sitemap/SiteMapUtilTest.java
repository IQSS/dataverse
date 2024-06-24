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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class SiteMapUtilTest {

    // see https://www.sitemaps.org/protocol.html#validating
    final String xsdSitemap = "https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd";
    final String xsdSitemapIndex = "https://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd";

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
        assertTrue(XmlValidator.validateXmlSchema(pathToSiteMap, new URL(xsdSitemap)));

        File sitemapFile = new File(pathToSiteMap);
        String sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath()))));
        //System.out.println("sitemap: " + sitemapString);

        assertTrue(sitemapString.contains("1955-11-12"));
        assertTrue(sitemapString.contains(publishedPid));
        assertFalse(sitemapString.contains(unpublishedPid));
        assertFalse(sitemapString.contains(harvestedPid));
        assertFalse(sitemapString.contains(deaccessionedPid));
    }

    @Test
    void testHugeSiteMap() throws IOException, ParseException, SAXException {
        // given
        final int nbDataverse = 50;
        final int nbDataset = SiteMapUtil.SITEMAP_LIMIT;
        final Timestamp now = new Timestamp(new Date().getTime());
        // Regex validate dataset URL
        final String sitemapUrlRegex = ".*/dataset\\.xhtml\\?persistentId=doi:10\\.666/FAKE/published[0-9]{1,5}</loc>$";
        // Regex validate sitemap URL: must include "/sitemap/" to be accessible because there is no pretty-faces rewrite
        final String sitemapIndexUrlRegex = ".*/sitemap/sitemap[1-2]\\.xml</loc>$";
        final String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern(SiteMapUtil.DATE_PATTERN));

        final List<Dataverse> dataverses = new ArrayList<>(nbDataverse);
        for (int i = 1; i <= nbDataverse; i++) {
            final Dataverse publishedDataverse = new Dataverse();
            publishedDataverse.setAlias(String.format("publishedDv%s", i));
            publishedDataverse.setModificationTime(now);
            publishedDataverse.setPublicationDate(now);
            dataverses.add(publishedDataverse);
        }

        final List<Dataset> datasets = new ArrayList<>(nbDataset);
        for (int i = 1; i <= nbDataset; i++) {
            final Dataset published = new Dataset();
            published.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.666", String.format("FAKE/published%s", i), null, AbstractDOIProvider.DOI_RESOLVER_URL, null));
            published.setPublicationDate(now);
            published.setModificationTime(now);
            datasets.add(published);
        }

        // when
        SiteMapUtil.updateSiteMap(dataverses, datasets);

        // then
        final Path siteMapDir = tempDocroot.resolve("sitemap");
        final String pathToSiteMapIndexFile = siteMapDir.resolve("sitemap_index.xml").toString();
        final String pathToSiteMap1File = siteMapDir.resolve("sitemap1.xml").toString();
        final String pathToSiteMap2File = siteMapDir.resolve("sitemap2.xml").toString();

        // validate sitemap_index.xml file with XSD
        assertDoesNotThrow(() -> XmlValidator.validateXmlWellFormed(pathToSiteMapIndexFile));
        assertTrue(XmlValidator.validateXmlSchema(pathToSiteMapIndexFile, new URL(xsdSitemapIndex)));

        // verify sitemap_index.xml content
        File sitemapFile = new File(pathToSiteMapIndexFile);
        String sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath())), StandardCharsets.UTF_8));
        // System.out.println("sitemap: " + sitemapString);

        String[] lines = sitemapString.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (StringUtils.isNotBlank(line)) {
                if (i == 0) {
                    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", line);
                } else if (i == 1) {
                    assertEquals("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">", line);
                } else if (i == 2) {
                    assertEquals("<sitemap>", line);
                } else if (line.startsWith("<loc>")) {
                    final String errorWithSitemapIndexUrl = String.format("Sitemap URL must match with \"%s\" but was \"%s\"", sitemapIndexUrlRegex, line);
                    assertTrue(line.matches(sitemapIndexUrlRegex), errorWithSitemapIndexUrl);
                } else if (line.startsWith("<lastmod>")) {
                    assertEquals(String.format("<lastmod>%s</lastmod>", today), line);
                }
            }
        }

        // validate sitemap1.xml file with XSD
        assertDoesNotThrow(() -> XmlValidator.validateXmlWellFormed(pathToSiteMap1File));
        assertTrue(XmlValidator.validateXmlSchema(pathToSiteMap1File, new URL(xsdSitemap)));

        // validate sitemap2.xml file with XSD
        assertDoesNotThrow(() -> XmlValidator.validateXmlWellFormed(pathToSiteMap2File));
        assertTrue(XmlValidator.validateXmlSchema(pathToSiteMap2File, new URL(xsdSitemap)));

        // verify sitemap2.xml content
        sitemapFile = new File(pathToSiteMap2File);
        sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath())), StandardCharsets.UTF_8));

        lines = sitemapString.split("\n");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", lines[0].strip());
        assertEquals("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">", lines[1].strip());
        boolean isContainsLocTag = false;
        boolean isContainsLastmodTag = false;
        // loop over 10 lines only, just need to validate the <loc> and <lastmod> tags
        for (int i = 5; i < 15; i++) {
            String line = lines[i].strip();
            if (StringUtils.isNotBlank(line)) {
                if (line.startsWith("<loc>")) {
                    isContainsLocTag = true;
                    final String errorWithSitemapIndexUrl = String.format("Sitemap URL must match with \"%s\" but was \"%s\"", sitemapUrlRegex, line);
                    assertTrue(line.matches(sitemapUrlRegex), errorWithSitemapIndexUrl);
                } else if (line.startsWith("<lastmod>")) {
                    isContainsLastmodTag = true;
                    assertEquals(String.format("<lastmod>%s</lastmod>", today), line);
                }
            }
        }
        assertTrue(isContainsLocTag, "Sitemap file must contains <loc> tag");
        assertTrue(isContainsLastmodTag, "Sitemap file must contains <lastmod> tag");
    }

}
