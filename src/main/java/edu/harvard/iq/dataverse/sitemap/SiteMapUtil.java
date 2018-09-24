package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public class SiteMapUtil {

    static final String SITEMAP_OUTFILE = "/tmp/out.xml";

    public static void updateSiteMap() throws ParserConfigurationException, TransformerException {

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
        StreamResult result = new StreamResult(new File(SITEMAP_OUTFILE));
        transformer.transform(source, result);

        // TODO: Remove this once there's a lot of data.
        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);

    }
}
