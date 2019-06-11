package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import edu.harvard.iq.dataverse.util.xml.XmlValidator;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SiteMapUtilTest {

    @Test
    public void testUpdateSiteMap() throws IOException, ParseException {

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
        String publishedPid = "doi:10.666/FAKE/published1";
        published.setGlobalId(new GlobalId(publishedPid));
        published.setPublicationDate(new Timestamp(new Date().getTime()));
        published.setModificationTime(new Timestamp(new Date().getTime()));
        datasets.add(published);

        Dataset unpublished = new Dataset();
        String unpublishedPid = "doi:10.666/FAKE/unpublished1";
        unpublished.setGlobalId(new GlobalId(unpublishedPid));
        Timestamp nullPublicationDateToIndicateNotPublished = null;
        unpublished.setPublicationDate(nullPublicationDateToIndicateNotPublished);
        datasets.add(unpublished);

        Dataset harvested = new Dataset();
        String harvestedPid = "doi:10.666/FAKE/harvested1";
        harvested.setGlobalId(new GlobalId(harvestedPid));
        harvested.setPublicationDate(new Timestamp(new Date().getTime()));
        harvested.setHarvestedFrom(new HarvestingClient());
        datasets.add(harvested);

        Dataset deaccessioned = new Dataset();
        String deaccessionedPid = "doi:10.666/FAKE/harvested1";
        deaccessioned.setGlobalId(new GlobalId(deaccessionedPid));
        deaccessioned.setPublicationDate(new Timestamp(new Date().getTime()));
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        datasetVersions.add(datasetVersion);
        deaccessioned.setVersions(datasetVersions);
        datasets.add(deaccessioned);

        File oldSitemapFile = new File("/tmp/sitemap.xml");
        if (oldSitemapFile.exists()) {
            oldSitemapFile.delete();
        }
        SiteMapUtil.updateSiteMap(dataverses, datasets);

        Exception wellFormedXmlException = null;
        try {
            assertTrue(XmlValidator.validateXmlWellFormed("/tmp/sitemap.xml"));
        } catch (Exception ex) {
            System.out.println("Exception caught checking that XML is well formed: " + ex);
            wellFormedXmlException = ex;
        }
        assertNull(wellFormedXmlException);

        Exception notValidAgainstSchemaException = null;
        try {
            assertTrue(XmlValidator.validateXmlSchema("/tmp/sitemap.xml", new URL("https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd")));
        } catch (MalformedURLException | SAXException ex) {
            System.out.println("Exception caught validating XML against the sitemap schema: " + ex);
            notValidAgainstSchemaException = ex;
        }
        assertNull(notValidAgainstSchemaException);

        File sitemapFile = new File("/tmp/sitemap.xml");
        String sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath()))));
        System.out.println("sitemap: " + sitemapString);

        assertTrue(sitemapString.contains("1955-11-12"));
        assertTrue(sitemapString.contains(publishedPid));
        assertFalse(sitemapString.contains(unpublishedPid));
        assertFalse(sitemapString.contains(harvestedPid));
        assertFalse(sitemapString.contains(deaccessionedPid));

    }

}
