package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.settings.ConfigCheckService;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.xml.XmlValidator;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SiteMapUtil {

    private static final Logger logger = Logger.getLogger(SiteMapUtil.class.getCanonicalName());

    static final String SITEMAP_FILENAME_FINAL = "sitemap.xml";
    static final String SITEMAP_FILENAME_STAGED = "sitemap.xml.staged";

    /**
     * TODO: Handle more than 50,000 entries in the sitemap.
     *
     * (As of this writing Harvard Dataverse only has ~3000 dataverses and
     * ~30,000 datasets.)
     *
     * "each Sitemap file that you provide must have no more than 50,000 URLs"
     * https://www.sitemaps.org/protocol.html
     *
     * Consider using a third party library: "One sitemap can contain a maximum
     * of 50,000 URLs. (Some sitemaps, like Google News sitemaps, can contain
     * only 1,000 URLs.) If you need to put more URLs than that in a sitemap,
     * you'll have to use a sitemap index file. Fortunately, WebSitemapGenerator
     * can manage the whole thing for you."
     * https://github.com/dfabulich/sitemapgen4j
     */
    public static void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {

        logger.info("BEGIN updateSiteMap");

        String sitemapPathString = getSitemapPathString();
        String stagedSitemapPathAndFileString = sitemapPathString + File.separator + SITEMAP_FILENAME_STAGED;
        String finalSitemapPathAndFileString = sitemapPathString + File.separator + SITEMAP_FILENAME_FINAL;

        Path stagedPath = Paths.get(stagedSitemapPathAndFileString);
        if (Files.exists(stagedPath)) {
            logger.warning("Unable to update sitemap! The staged file from a previous run already existed. Delete " + stagedSitemapPathAndFileString + " and try again.");
            return;
        }

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            logger.warning("Unable to update sitemap! ParserConfigurationException: " + ex.getLocalizedMessage());
            return;
        }
        Document document = documentBuilder.newDocument();

        Element urlSet = document.createElement("urlset");
        urlSet.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
        urlSet.setAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
        document.appendChild(urlSet);

        for (Dataverse dataverse : dataverses) {
            if (!dataverse.isReleased()) {
                continue;
            }
            Element url = document.createElement("url");
            urlSet.appendChild(url);

            Element loc = document.createElement("loc");
            String dataverseAlias = dataverse.getAlias();
            loc.appendChild(document.createTextNode(SystemConfig.getDataverseSiteUrlStatic() + "/dataverse/" + dataverseAlias));
            url.appendChild(loc);

            Element lastmod = document.createElement("lastmod");
            lastmod.appendChild(document.createTextNode(getLastModDate(dataverse)));
            url.appendChild(lastmod);
        }

        for (Dataset dataset : datasets) {
            if (!dataset.isReleased()) {
                continue;
            }
            if (dataset.isHarvested()) {
                continue;
            }
            // The deaccessioned check is last because it has to iterate through dataset versions.
            if (dataset.isDeaccessioned()) {
                continue;
            }
            Element url = document.createElement("url");
            urlSet.appendChild(url);

            Element loc = document.createElement("loc");
            String datasetPid = dataset.getGlobalId().asString();
            loc.appendChild(document.createTextNode(SystemConfig.getDataverseSiteUrlStatic() + "/dataset.xhtml?persistentId=" + datasetPid));
            url.appendChild(loc);

            Element lastmod = document.createElement("lastmod");
            lastmod.appendChild(document.createTextNode(getLastModDate(dataset)));
            url.appendChild(lastmod);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException ex) {
            logger.warning("Unable to update sitemap! TransformerConfigurationException: " + ex.getLocalizedMessage());
            return;
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(document);
        File directory = new File(sitemapPathString);
        if (!directory.exists()) {
            directory.mkdir();
        }

        boolean debug = false;
        if (debug) {
            logger.info("Writing sitemap to console/logs");
            StreamResult consoleResult = new StreamResult(System.out);
            try {
                transformer.transform(source, consoleResult);
            } catch (TransformerException ex) {
                logger.warning("Unable to print sitemap to the console: " + ex.getLocalizedMessage());
            }
        }

        logger.info("Writing staged sitemap to " + stagedSitemapPathAndFileString);
        StreamResult result = new StreamResult(new File(stagedSitemapPathAndFileString));
        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            logger.warning("Unable to update sitemap! Unable to write staged sitemap to " + stagedSitemapPathAndFileString + ". TransformerException: " + ex.getLocalizedMessage());
            return;
        }

        logger.info("Checking staged sitemap for well-formedness. The staged file is " + stagedSitemapPathAndFileString);
        try {
            XmlValidator.validateXmlWellFormed(stagedSitemapPathAndFileString);
        } catch (Exception ex) {
            logger.warning("Unable to update sitemap! Staged sitemap file is not well-formed XML! The exception for " + stagedSitemapPathAndFileString + " is " + ex.getLocalizedMessage());
            return;
        }

        logger.info("Checking staged sitemap against XML schema. The staged file is " + stagedSitemapPathAndFileString);
        URL schemaUrl = null;
        try {
            schemaUrl = new URL("https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
        } catch (MalformedURLException ex) {
            // This URL is hard coded and it's fine. We should never get MalformedURLException so we just swallow the exception and carry on.
        }
        try {
            XmlValidator.validateXmlSchema(stagedSitemapPathAndFileString, schemaUrl);
        } catch (SAXException | IOException ex) {
            logger.warning("Unable to update sitemap! Exception caught while checking XML staged file (" + stagedSitemapPathAndFileString + " ) against XML schema: " + ex.getLocalizedMessage());
            return;
        }

        Path finalPath = Paths.get(finalSitemapPathAndFileString);
        logger.info("Copying staged sitemap from " + stagedSitemapPathAndFileString + " to " + finalSitemapPathAndFileString);
        try {
            Files.move(stagedPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.warning("Unable to update sitemap! Unable to copy staged sitemap from " + stagedSitemapPathAndFileString + " to " + finalSitemapPathAndFileString + ". IOException: " + ex.getLocalizedMessage());
            return;
        }

        logger.info("END updateSiteMap");
    }

    private static String getLastModDate(DvObjectContainer dvObjectContainer) {
        // TODO: Decide if YYYY-MM-DD is enough. https://www.sitemaps.org/protocol.html
        // says "The date of last modification of the file. This date should be in W3C Datetime format.
        // This format allows you to omit the time portion, if desired, and use YYYY-MM-DD."
        return new SimpleDateFormat("yyyy-MM-dd").format(dvObjectContainer.getModificationTime());
    }

    public static boolean stageFileExists() {
        String sitemapPathString = getSitemapPathString();
        String stagedSitemapPathAndFileString = sitemapPathString + File.separator + SITEMAP_FILENAME_STAGED;
        Path stagedPath = Paths.get(stagedSitemapPathAndFileString);
        if (Files.exists(stagedPath)) {
            logger.warning("Unable to update sitemap! The staged file from a previous run already existed. Delete " + stagedSitemapPathAndFileString + " and try again.");
            return true;
        }
        return false;
    }
    
    /**
     * Lookup the location where to generate the sitemap.
     *
     * Note: the location is checked to be configured, does exist and is writeable in
     * {@link ConfigCheckService#checkSystemDirectories()}
     *
     * @return Sitemap storage location ([docroot]/sitemap)
     */
    private static String getSitemapPathString() {
        return JvmSettings.DOCROOT_DIRECTORY.lookup() + File.separator + "sitemap";

    }
}
