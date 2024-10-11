package edu.harvard.iq.dataverse.util.xml;

import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.util.xml.XmlPrinter.prettyPrintXml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class XmlPrinterTest {

    @Test
    public void testPrettyPrintXmlShort() {

        String[] results = prettyPrintXml("<foo><bar>baz</bar></foo>").split("\\R");

        assertThat(results[0]).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(results[1]).isEqualTo("<foo>");
        assertThat(results[2]).isEqualTo("  <bar>baz</bar>");
        assertThat(results[3]).isEqualTo("</foo>");
    }

    @Test
    public void testPrettyPrintXmlNonXML() {

        assertThat(prettyPrintXml("THIS IS NOT XML")).isEqualTo("THIS IS NOT XML");
    }

    @Test
    public void testPrettyPrintXmlEmptyString() {

        assertThat(prettyPrintXml("")).isEmpty();
    }

    @Test
    public void testPrettyPrintXmlNull() {

        assertThatNullPointerException().isThrownBy(() -> prettyPrintXml(null));
    }
}
