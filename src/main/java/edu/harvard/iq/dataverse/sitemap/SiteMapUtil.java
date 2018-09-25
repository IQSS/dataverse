package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
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

    public static void updateSiteMap() throws ParserConfigurationException, TransformerException, IOException {

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
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element urlSet = document.createElement("urlset");
        document.appendChild(urlSet);

        Element url = document.createElement("url");
        urlSet.appendChild(url);

        Element loc = document.createElement("loc");
        loc.appendChild(document.createTextNode(SystemConfig.getDataverseSiteUrlStatic() + "/"));
        url.appendChild(loc);

        Element lastmod = document.createElement("lastmod");
        LocalDateTime localDateTime = LocalDateTime.now();
        // TODO: Decide if YYYY-MM-DD is enough. https://www.sitemaps.org/protocol.html
        // says "The date of last modification of the file. This date should be in W3C Datetime format.
        // This format allows you to omit the time portion, if desired, and use YYYY-MM-DD."
        DateTimeFormatter w3cDatetimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = localDateTime.format(w3cDatetimeFormater);
        lastmod.appendChild(document.createTextNode(date));
        url.appendChild(lastmod);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(document);
        File directory = new File(sitemapPath);
        if (!directory.exists()) {
            directory.mkdir();
        }
        logger.info("Writing sitemap to  " + sitemapPathAndFile);
        StreamResult result = new StreamResult(new File(sitemapPathAndFile));
        transformer.transform(source, result);

        // TODO: Remove this once there's a lot of data.
        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);

    }
}
