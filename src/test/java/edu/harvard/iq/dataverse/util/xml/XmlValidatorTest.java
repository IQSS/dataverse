package edu.harvard.iq.dataverse.util.xml;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XmlValidatorTest {

    private static final Logger logger = Logger.getLogger(XmlValidatorTest.class.getCanonicalName());

    @Test
    public void testValidateXml() throws IOException, SAXException, ParserConfigurationException {
        assertTrue(XmlValidator.validateXml("scripts/issues/3845/sendToDataCite.xml", new URL("http://schema.datacite.org/meta/kernel-3/metadata.xsd")));
        // FIXME: Make sure the DDI we export is valid: https://github.com/IQSS/dataverse/issues/3648
//        assertTrue(XmlValidator.validateXml("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml", new URL("http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd")));
    }

    @Test
    public void testWellFormedXml() throws ParserConfigurationException, SAXException, IOException {
        assertTrue(XmlValidator.xmlWellFormed("pom.xml"));
        assertFalse(XmlValidator.xmlWellFormed("scripts/issues/3845/not-well-formed.xml"));
        assertTrue(XmlValidator.xmlWellFormed("scripts/issues/3845/sendToDataCite.xml"));
    }

}
