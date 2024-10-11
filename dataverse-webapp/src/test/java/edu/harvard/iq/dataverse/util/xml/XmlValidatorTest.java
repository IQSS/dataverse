package edu.harvard.iq.dataverse.util.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlValidatorTest {

    private static final Logger logger = Logger.getLogger(XmlValidatorTest.class.getCanonicalName());

    //Ignored as this relies on an external resource that has been down occasionally. 
    //May be a good test for our full vs. everytime test classifications (#4896) -MAD 4.9.1
    @Disabled
    @Tag("NonEssentialTests")
    @Test
    public void testValidateXml() throws IOException, SAXException, ParserConfigurationException {
        assertTrue(XmlValidator.validateXmlSchema("src/test/java/edu/harvard/iq/dataverse/util/xml/sendToDataCite.xml", new URL("https://schema.datacite.org/meta/kernel-3/metadata.xsd")));
        // FIXME: Make sure the DDI we export is valid: https://github.com/IQSS/dataverse/issues/3648
//        assertTrue(XmlValidator.validateXml("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml", new URL("http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd")));
    }

    @Tag("NonEssentialTests")
    @Test
    public void testWellFormedXml() {

        // well-formed XML
        Exception ex1 = null;
        try {
            assertTrue(XmlValidator.validateXmlWellFormed(getClass().getClassLoader().getResource("xml/util/sendToDataCite.xml").toURI().toString()));
        } catch (Exception ex) {
            ex1 = ex;
        }
        Assertions.assertNull(ex1);

        // not well-formed XML
        Exception ex2 = null;
        try {
            XmlValidator.validateXmlWellFormed(getClass().getClassLoader().getResource("xml/util/not-well-formed.xml").toURI().toString());
        } catch (Exception ex) {
            ex2 = ex;
        }
        Assertions.assertNotNull(ex2);
        Assertions.assertEquals("XML is not well formed: The element type \"br\" must be terminated by the matching end-tag \"</br>\".", ex2.getMessage());

        // other exception
        Exception ex3 = null;
        try {
            XmlValidator.validateXmlWellFormed("path/to/nowhere.xml");
        } catch (Exception ex) {
            ex3 = ex;
        }
        Assertions.assertNotNull(ex3);
        Assertions.assertEquals("class java.io.FileNotFoundException", ex3.getClass().toString());

    }

}
