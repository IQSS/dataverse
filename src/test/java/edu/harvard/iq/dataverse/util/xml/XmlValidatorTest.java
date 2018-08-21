package edu.harvard.iq.dataverse.util.xml;

import edu.harvard.iq.dataverse.NonEssentialTests;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.xml.sax.SAXException;

public class XmlValidatorTest {

    private static final Logger logger = Logger.getLogger(XmlValidatorTest.class.getCanonicalName());

    //Ignored as this relies on an external resource that has been down occasionally. 
    //May be a good test for our full vs. everytime test classifications (#4896) -MAD 4.9.1
    @Ignore
    @Category(NonEssentialTests.class)
    @Test
    public void testValidateXml() throws IOException, SAXException, ParserConfigurationException {
        assertTrue(XmlValidator.validateXmlSchema("src/test/java/edu/harvard/iq/dataverse/util/xml/sendToDataCite.xml", new URL("https://schema.datacite.org/meta/kernel-3/metadata.xsd")));
        // FIXME: Make sure the DDI we export is valid: https://github.com/IQSS/dataverse/issues/3648
//        assertTrue(XmlValidator.validateXml("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml", new URL("http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd")));
    }

    @Category(NonEssentialTests.class)
    @Test
    public void testWellFormedXml() {

        // well-formed XML
        Exception ex1 = null;
        try {
            assertTrue(XmlValidator.validateXmlWellFormed("src/test/java/edu/harvard/iq/dataverse/util/xml/sendToDataCite.xml"));
        } catch (Exception ex) {
            ex1 = ex;
        }
        Assert.assertNull(ex1);

        // not well-formed XML
        Exception ex2 = null;
        try {
            XmlValidator.validateXmlWellFormed("src/test/java/edu/harvard/iq/dataverse/util/xml/not-well-formed.xml");
        } catch (Exception ex) {
            ex2 = ex;
        }
        Assert.assertNotNull(ex2);
        Assert.assertEquals("XML is not well formed: The element type \"br\" must be terminated by the matching end-tag \"</br>\".", ex2.getMessage());

        // other exception
        Exception ex3 = null;
        try {
            XmlValidator.validateXmlWellFormed("path/to/nowhere.xml");
        } catch (Exception ex) {
            ex3 = ex;
        }
        Assert.assertNotNull(ex3);
        Assert.assertEquals("class java.io.FileNotFoundException", ex3.getClass().toString());

    }

}
