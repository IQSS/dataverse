package edu.harvard.iq.dataverse.util.xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.harvard.iq.dataverse.util.testing.Tags;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class XmlValidatorTest {

    //Ignored as this relies on an external resource that has been down occasionally. 
    //May be a good test for our full vs. everytime test classifications (#4896) -MAD 4.9.1
    @Disabled
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @Test
    public void testValidateXml() throws IOException, SAXException, ParserConfigurationException {
        assertTrue(XmlValidator.validateXmlSchema("src/test/java/edu/harvard/iq/dataverse/util/xml/sendToDataCite.xml", new URL("https://schema.datacite.org/meta/kernel-3/metadata.xsd")));
        // FIXME: Make sure the DDI we export is valid: https://github.com/IQSS/dataverse/issues/3648
        // assertTrue(XmlValidator.validateXml("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml", new URL("http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd")));
    }

    @Test
    public void validateXmlWellFormed_validXML() {
        String xmlFile = "src/test/java/edu/harvard/iq/dataverse/util/xml/sendToDataCite.xml";
        try {
            assertTrue(XmlValidator.validateXmlWellFormed(xmlFile));
        } catch(Exception e) {
            fail();
        }
    }

    @Test
    public void validateXmlWellFormed_invalidXML() {
        String xmlFile = "src/test/java/edu/harvard/iq/dataverse/util/xml/not-well-formed.xml";
        try {
            assertTrue(XmlValidator.validateXmlWellFormed(xmlFile));
            fail("validateXmlWellFormed() should throw exception on malformed XML");
        } catch(Exception e) {
            return;
        }
    }

    @Test
    public void validateXmlWellFormed_exceptionMessage() {
        String xmlFile = "src/test/java/edu/harvard/iq/dataverse/util/xml/not-well-formed.xml";
        String expectedExceptionMessage = "XML is not well formed: The element type \"br\" must be terminated by the matching end-tag \"</br>\".";
        String actualExceptionMessage = "";

        try {
            XmlValidator.validateXmlWellFormed(xmlFile);
            fail("validateXmlWellFormed() should throw exception on malformed XML");
        } catch (Exception ex) {
            actualExceptionMessage = ex.getMessage();
        }
        assertEquals(expectedExceptionMessage, actualExceptionMessage);
    }

    @Test
    public void validateXmlWellFormed_nonexistentFile() {
        String xmlFile = "path/to/nowhere.xml";
        try {
            XmlValidator.validateXmlWellFormed(xmlFile);
            fail("validateXmlWellFormed() should throw exception on malformed XML");
        } catch(Exception e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }
    }
}
