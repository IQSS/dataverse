package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.sql.Timestamp;
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

// We are aware of https://github.com/dfabulich/sitemapgen4j but haven't tried it.
public class SiteMapUtil {

    private static final Logger logger = Logger.getLogger(SiteMapUtil.class.getCanonicalName());

    static final String SITEMAP_FILENAME = "sitemap.xml";

    public static void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {

        String sitemapPath = "/tmp";
        String sitemapPathAndFile;
        // i.e. /usr/local/glassfish4/glassfish/domains/domain1
        String domainRoot = System.getProperty("com.sun.aas.instanceRoot");
        if (domainRoot != null) {
            // Note that we write to a directory called "sitemap" but we serve just "/sitemap.xml" using PrettyFaces.
            sitemapPath = domainRoot + File.separator + "docroot" + File.separator + "sitemap";
        }
        sitemapPathAndFile = sitemapPath + File.separator + SITEMAP_FILENAME;

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
            Timestamp lastModified = dataverse.getModificationTime();
            // TODO: Decide if YYYY-MM-DD is enough. https://www.sitemaps.org/protocol.html
            // says "The date of last modification of the file. This date should be in W3C Datetime format.
            // This format allows you to omit the time portion, if desired, and use YYYY-MM-DD."
            String date = new SimpleDateFormat("yyyy-MM-dd").format(lastModified);
            lastmod.appendChild(document.createTextNode(date));
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
            Timestamp publicationDate = dataset.getPublicationDate();
            // TODO: Decide if YYYY-MM-DD is enough. https://www.sitemaps.org/protocol.html
            // says "The date of last modification of the file. This date should be in W3C Datetime format.
            // This format allows you to omit the time portion, if desired, and use YYYY-MM-DD."
            String date = new SimpleDateFormat("yyyy-MM-dd").format(publicationDate);
            lastmod.appendChild(document.createTextNode(date));
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
        File directory = new File(sitemapPath);
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

        logger.info("Writing sitemap to " + sitemapPathAndFile);
        StreamResult result = new StreamResult(new File(sitemapPathAndFile));
        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            logger.warning("Unable to write sitemap to " + sitemapPathAndFile + ". TransformerException: " + ex.getLocalizedMessage());
        }

    }
}
