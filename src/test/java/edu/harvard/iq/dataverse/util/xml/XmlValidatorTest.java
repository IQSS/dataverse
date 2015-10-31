package edu.harvard.iq.dataverse.util.xml;

import java.io.IOException;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XmlValidatorTest {

    private static final Logger logger = Logger.getLogger(XmlValidatorTest.class.getCanonicalName());

    @Test
    public void testValidateXml() throws IOException, SAXException {
        String dir = "src/test/java/edu/harvard/iq/dataverse/export/ddi/";
        assertEquals(true, XmlValidator.validateXml(dir + "dataset-finch1.xml", dir + "Version2-0.xsd"));
        assertEquals(true, XmlValidator.validateXml(dir + "dataset-spruce1.xml", dir + "Version2-0.xsd"));
    }

}
