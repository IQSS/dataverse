package edu.harvard.iq.dataverse.util.xml;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
}