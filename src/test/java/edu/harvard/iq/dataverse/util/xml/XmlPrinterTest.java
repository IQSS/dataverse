package edu.harvard.iq.dataverse.util.xml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlPrinterTest {

    @Test
    public void testPrettyPrintXmlShort() {
        String lineSep = System.lineSeparator();
        String xml = "<foo><bar>baz</bar></foo>";
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSep
                + "<foo>" + lineSep
                + "  <bar>baz</bar>" + lineSep
                + "</foo>" + lineSep;
        String result = XmlPrinter.prettyPrintXml(xml);
        assertEquals(expResult, result);
    }

    @Test
    public void testPrettyPrintXmlNonXML() {
        assertEquals("THIS IS NOT XML", XmlPrinter.prettyPrintXml("THIS IS NOT XML"));
    }

}
