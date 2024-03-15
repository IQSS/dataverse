package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.DOIServiceBean;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
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
        published.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, "10.666", "FAKE/published1", null, DOIServiceBean.DOI_RESOLVER_URL, null));
        String publishedPid = published.getGlobalId().asString();
        published.setPublicationDate(new Timestamp(new Date().getTime()));
        published.setModificationTime(new Timestamp(new Date().getTime()));
        datasets.add(published);

        Dataset unpublished = new Dataset();
        unpublished.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, "10.666", "FAKE/unpublished1", null, DOIServiceBean.DOI_RESOLVER_URL, null));
        String unpublishedPid = unpublished.getGlobalId().asString();

        Timestamp nullPublicationDateToIndicateNotPublished = null;
        unpublished.setPublicationDate(nullPublicationDateToIndicateNotPublished);
        datasets.add(unpublished);

        Dataset harvested = new Dataset();
        harvested.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, "10.666", "FAKE/harvested1", null, DOIServiceBean.DOI_RESOLVER_URL, null));
        String harvestedPid = harvested.getGlobalId().asString();
        harvested.setPublicationDate(new Timestamp(new Date().getTime()));
        harvested.setHarvestedFrom(new HarvestingClient());
        datasets.add(harvested);

        Dataset deaccessioned = new Dataset();
        deaccessioned.setGlobalId(new GlobalId(DOIServiceBean.DOI_PROTOCOL, "10.666", "FAKE/deaccessioned1", null, DOIServiceBean.DOI_RESOLVER_URL, null));
        String deaccessionedPid = deaccessioned.getGlobalId().asString();

        deaccessioned.setPublicationDate(new Timestamp(new Date().getTime()));
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        datasetVersions.add(datasetVersion);
        deaccessioned.setVersions(datasetVersions);
        datasets.add(deaccessioned);

        Path tmpDirPath = Files.createTempDirectory(null);
        String tmpDir = tmpDirPath.toString();
        File docroot = new File(tmpDir + File.separator + "docroot");
        docroot.mkdirs();
        System.setProperty("com.sun.aas.instanceRoot", tmpDir);

        SiteMapUtil.updateSiteMap(dataverses, datasets);

        String pathToTest = tmpDirPath + File.separator + "docroot" + File.separator + "sitemap";
        String pathToSiteMap = pathToTest + File.separator + "sitemap.xml";

        Exception wellFormedXmlException = null;
        try {
            assertTrue(XmlValidator.validateXmlWellFormed(pathToSiteMap));
        } catch (Exception ex) {
            System.out.println("Exception caught checking that XML is well formed: " + ex);
            wellFormedXmlException = ex;
        }
        assertNull(wellFormedXmlException);

        Exception notValidAgainstSchemaException = null;
        try {
            assertTrue(XmlValidator.validateXmlSchema(pathToSiteMap, new URL("https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd")));
        } catch (MalformedURLException | SAXException ex) {
            System.out.println("Exception caught validating XML against the sitemap schema: " + ex);
            notValidAgainstSchemaException = ex;
        }
        assertNull(notValidAgainstSchemaException);

        File sitemapFile = new File(pathToSiteMap);
        String sitemapString = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(sitemapFile.getAbsolutePath()))));
        System.out.println("sitemap: " + sitemapString);

        assertTrue(sitemapString.contains("1955-11-12"));
        assertTrue(sitemapString.contains(publishedPid));
        assertFalse(sitemapString.contains(unpublishedPid));
        assertFalse(sitemapString.contains(harvestedPid));
        assertFalse(sitemapString.contains(deaccessionedPid));

        System.clearProperty("com.sun.aas.instanceRoot");

    }

}
