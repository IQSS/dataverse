package edu.harvard.iq.dataverse.util.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XmlPrinterTest {

    @Test
    public void testPrettyPrintXmlShort() {
        final String xml = "<foo><bar>baz</bar></foo>";
        final String expResult =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<foo>\n" + "  <bar>baz</bar>\n" + "</foo>\n";
        final String result = XmlPrinter.prettyPrintXml(xml);
        assertEquals(expResult, result);
    }

    @Test
    public void testPrettyPrintXmlNonXML() {
        assertEquals("THIS IS NOT XML", XmlPrinter.prettyPrintXml("THIS IS NOT XML"));
    }

}
