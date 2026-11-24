package edu.harvard.iq.dataverse.util.xml;

import org.junit.jupiter.api.Test;
import org.w3c.dom.ls.LSInput;
import static org.junit.jupiter.api.Assertions.*;

public class LocalXmlResourceResolverTest {

    @Test
    public void testResolveResource() {
        LocalXmlResourceResolver resolver = new LocalXmlResourceResolver();
        
        // Test DataCite Kernel 4 main schema
        LSInput input4 = resolver.resolveResource(null, null, null, "https://schema.datacite.org/meta/kernel-4/metadata.xsd", null);
        assertNotNull(input4, "Should resolve DataCite Kernel 4");
        assertNotNull(input4.getByteStream(), "Should have byte stream for Kernel 4");
        
        // Test DataCite Kernel 4.7
        LSInput input47 = resolver.resolveResource(null, null, null, "http://schema.datacite.org/meta/kernel-4.7/metadata.xsd", null);
        assertNotNull(input47, "Should resolve DataCite Kernel 4.7");
        
        
        // Test relative include resolution
        LSInput inputInclude = resolver.resolveResource(null, null, null, "include/datacite-titleType-v4.xsd", "https://schema.datacite.org/meta/kernel-4/metadata.xsd");
        assertNotNull(inputInclude, "Should resolve relative include for Kernel 4");
        
        // Test unknown URL
        LSInput inputUnknown = resolver.resolveResource(null, null, null, "https://example.com/schema.xsd", null);
        assertNull(inputUnknown, "Should not resolve unknown URL");
    }
}
