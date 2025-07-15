package edu.harvard.iq.dataverse.util.xml;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlValidator {

    private static final Logger logger = Logger.getLogger(XmlValidator.class.getCanonicalName());

    public static boolean validateXmlSchema(String fileToValidate, URL schemaToValidateAgainst) throws MalformedURLException, SAXException, IOException {
        
        Source xmlFile = new StreamSource(new File(fileToValidate));
        return validateXmlSchema(xmlFile, schemaToValidateAgainst);
    }
    
    public static boolean validateXmlSchema(Source xmlFile, URL schemaToValidateAgainst) throws MalformedURLException, SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // Additional protection
            schemaFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            schemaFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            schemaFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (SAXException e) {
            logger.warning("Could not set XML security features: " + e.getMessage());
        }
        
        Schema schema = schemaFactory.newSchema(schemaToValidateAgainst);
        Validator validator = schema.newValidator();

        try {
            validator.validate(xmlFile);
            logger.info(xmlFile.getSystemId() + " is valid");
            return true;
        } catch (SAXException ex) {
            System.out.print(ex.getMessage());
            System.out.print(ex.getLocalizedMessage());
            logger.info(xmlFile.getSystemId() + " is not valid: " + ex.getLocalizedMessage());
            return false;
        }
    }

    /**
     * @param filename XML file on disk to check for well-formedness.
     * @return true if well-formed or an exception with a message about why if
     * not.
     * @throws Exception if the XML is not well-formed with a message about why.
     */
    public static boolean validateXmlWellFormed(String filename) throws Exception {
        DocumentBuilderFactory factory = XmlUtil.getSecureDocumentBuilderFactory();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new SimpleErrorHandler());
        try {
            Document document = builder.parse(new InputSource(filename));
            return true;
        } catch (SAXException ex) {
            throw new Exception("XML is not well formed: " + ex.getMessage(), ex);
        }

    }

    public static class SimpleErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException e) throws SAXException {
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
        }
    }
}
