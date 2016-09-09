package edu.harvard.iq.dataverse.util.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class XmlValidator {

    private static final Logger logger = Logger.getLogger(XmlValidator.class.getCanonicalName());

    public static boolean validateXml(String fileToValidate, String schemaToValidateAgainst) throws IOException, SAXException {
        System.out.print(" before get schema file " + schemaToValidateAgainst);
        StreamSource schemaFile = new StreamSource(new File(schemaToValidateAgainst));
       // StreamSource schemaFile = new StreamSource(new URL(schemaToValidateAgainst).openStream());
        System.out.print(" after get schema file ");
        Source xmlFile = new StreamSource(new File(fileToValidate));
        System.out.print(" after get file to validate ");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        System.out.print(" after get schema factory ");
        Schema schema = schemaFactory.newSchema(schemaFile);
        System.out.print(" after instantiate Schema ");
        Validator validator = schema.newValidator();
        System.out.print(" after instantiate Validator ");
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

}
