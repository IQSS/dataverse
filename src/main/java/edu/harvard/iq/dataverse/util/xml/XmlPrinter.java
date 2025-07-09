package edu.harvard.iq.dataverse.util.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlPrinter {

    private static final Logger logger = Logger.getLogger(XmlPrinter.class.getCanonicalName());

    static public String prettyPrintXml(String xml) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = tf.newTransformer();
            // pretty print with indentation
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // set amount of whitespace during indent
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    
            StreamSource source = new StreamSource(new StringReader(xml));
            StringWriter output = new StringWriter();
    
            transformer.transform(source, new StreamResult(output));
            
            // This hacky hack is necessary due to https://bugs.openjdk.java.net/browse/JDK-7150637
            // which has not been a problem before because of using old xercesImpl 2.8.0 library dated 2006.
            // That old library contains security flaws, so the change was necessary.
            return output
                        .toString()
                        .replaceFirst("encoding=\"UTF-8\"\\?>",
                                      "encoding=\"UTF-8\"?>"+System.lineSeparator());
        } catch (IllegalArgumentException | TransformerException ex) {
            logger.info("Returning XML as-is due to problem pretty printing it: " + ex.toString());
            return xml;
        }
    }

}
