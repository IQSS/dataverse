package edu.harvard.iq.dataverse.util.xml;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Utility class for XML processing with security settings to prevent XXE attacks.
 */
public class XmlUtil {
    
    private static final Logger logger = Logger.getLogger(XmlUtil.class.getCanonicalName());
    
    /**
     * Creates and returns a DocumentBuilder with security settings to prevent XXE attacks.
     * 
     * @return A secure DocumentBuilder instance or null if configuration fails
     */
    public static DocumentBuilder getSecureDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        try {
            // Disable DTDs (doctypes)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            // Disable external entities
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            // Disable entity expansion
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            
            // Additional security settings
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            return factory.newDocumentBuilder();
            
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.log(Level.SEVERE, "Failed to create secure DocumentBuilder: {0}", pce.getMessage());
            return null;
        }
    }
    
    /**
     * Creates and returns a DocumentBuilderFactory with security settings to prevent XXE attacks.
     * 
     * @return A secure DocumentBuilderFactory instance
     */
    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        try {
            // Disable DTDs (doctypes)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            // Disable external entities
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            // Disable entity expansion
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            
            // Additional security settings
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.log(Level.SEVERE, "Failed to configure secure DocumentBuilderFactory: {0}", pce.getMessage());
        }
        
        return factory;
    }
    
    /**
     * Creates and returns a secure XMLReader with protection against XXE attacks.
     * 
     * @return A secure XMLReader instance
     * @throws SAXException If there's an error creating the XMLReader
     * @throws ParserConfigurationException If there's an error configuring the parser
     */
    public static XMLReader getSecureXMLReader() throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        
        // Configure the parser factory with security features
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        // Create a secure parser
        SAXParser parser = spf.newSAXParser();
        
        // Set additional security properties
        parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        
        // Get the XMLReader from the parser
        XMLReader reader = parser.getXMLReader();
        
        return reader;
    }
    
    /**
     * Creates and returns a secure XMLInputFactory with protection against XXE attacks.
     * 
     * @return A secure XMLInputFactory instance
     */
    public static XMLInputFactory getSecureXMLInputFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        
        // Set coalescing to merge CDATA sections with adjacent text nodes
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        
        // Disable DTDs and external entities
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        
        // Disable entity replacement
        try {
            xmlInputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
            xmlInputFactory.setProperty("javax.xml.stream.supportDTD", Boolean.FALSE);
        } catch (IllegalArgumentException e) {
            // Some implementations might not support these exact property names
            logger.log(Level.FINE, "XMLInputFactory doesn't support some security properties, continuing with defaults", e);
        }
        
        return xmlInputFactory;
    }
}