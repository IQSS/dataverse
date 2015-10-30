package edu.harvard.iq.dataverse.util.xml;

import java.io.File;
import java.io.IOException;
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

        StreamSource schemaFile = new StreamSource(new File(schemaToValidateAgainst));
        Source xmlFile = new StreamSource(new File(fileToValidate));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            logger.info(xmlFile.getSystemId() + " is valid");
            return true;
        } catch (SAXException ex) {
            logger.info(xmlFile.getSystemId() + " is not valid: " + ex.getLocalizedMessage());
            return false;
        }
    }

}
