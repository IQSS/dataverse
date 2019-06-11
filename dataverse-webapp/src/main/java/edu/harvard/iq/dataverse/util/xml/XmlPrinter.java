package edu.harvard.iq.dataverse.util.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

public class XmlPrinter {

    private static final Logger logger = Logger.getLogger(XmlPrinter.class.getCanonicalName());

    static public String prettyPrintXml(String xml) {
        try {
            Transformer transformer = SAXTransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            Source source = new SAXSource(new InputSource(new ByteArrayInputStream(xml.getBytes())));
            StreamResult streamResult = new StreamResult(new ByteArrayOutputStream());
            transformer.transform(source, streamResult);
            return new String(((ByteArrayOutputStream) streamResult.getOutputStream()).toByteArray());
        } catch (IllegalArgumentException | TransformerException ex) {
            logger.info("Returning XML as-is due to problem pretty printing it: " + ex.toString());
            return xml;
        }
    }

}
