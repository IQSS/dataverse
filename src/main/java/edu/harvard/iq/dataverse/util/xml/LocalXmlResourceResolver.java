package edu.harvard.iq.dataverse.util.xml;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * A resolver that maps remote XML schemas to local resources.
 */
public class LocalXmlResourceResolver implements LSResourceResolver {

    private static final Logger logger = Logger.getLogger(LocalXmlResourceResolver.class.getCanonicalName());

    private static final Map<String, String> URL_TO_RESOURCE = new HashMap<>();

    static {
        // DataCite Kernel 4
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        URL_TO_RESOURCE.put("http://schema.datacite.org/meta/kernel-4/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4.7/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        URL_TO_RESOURCE.put("http://schema.datacite.org/meta/kernel-4.7/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4.1/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        URL_TO_RESOURCE.put("http://schema.datacite.org/meta/kernel-4.1/metadata.xsd", "datacite/kernel-4/metadata.xsd");
        
        // DataCite Kernel 4 Includes
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/xml.xsd", "datacite/kernel-4/include/xml.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-titleType-v4.xsd", "datacite/kernel-4/include/datacite-titleType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-contributorType-v4.xsd", "datacite/kernel-4/include/datacite-contributorType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-dateType-v4.xsd", "datacite/kernel-4/include/datacite-dateType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-resourceType-v4.xsd", "datacite/kernel-4/include/datacite-resourceType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-relationType-v4.xsd", "datacite/kernel-4/include/datacite-relationType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-relatedIdentifierType-v4.xsd", "datacite/kernel-4/include/datacite-relatedIdentifierType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-funderIdentifierType-v4.xsd", "datacite/kernel-4/include/datacite-funderIdentifierType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-descriptionType-v4.xsd", "datacite/kernel-4/include/datacite-descriptionType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-nameType-v4.xsd", "datacite/kernel-4/include/datacite-nameType-v4.xsd");
        URL_TO_RESOURCE.put("https://schema.datacite.org/meta/kernel-4/include/datacite-numberType-v4.xsd", "datacite/kernel-4/include/datacite-numberType-v4.xsd");

    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        logger.fine("Resolving resource: type=" + type + ", namespaceURI=" + namespaceURI + ", publicId=" + publicId + ", systemId=" + systemId + ", baseURI=" + baseURI);
        
        String resourcePath = URL_TO_RESOURCE.get(systemId);
        if (resourcePath == null && systemId != null) {
            // Try to resolve relative path if baseURI is one of ours
            if (baseURI != null && baseURI.contains("datacite.org/meta/kernel-4")) {
                if (systemId.startsWith("include/")) {
                    resourcePath = "datacite/kernel-4/" + systemId;
                }
            }
        }

        if (resourcePath != null) {
            InputStream is = getClass().getResourceAsStream("schema/" + resourcePath);
            if (is != null) {
                logger.fine("Using local resource for " + systemId);
                return new LocalLSInput(is, publicId, systemId, baseURI);
            }
        }

        return null;
    }

    private static class LocalLSInput implements LSInput {
        private final InputStream inputStream;
        private final String publicId;
        private final String systemId;
        private final String baseURI;

        public LocalLSInput(InputStream inputStream, String publicId, String systemId, String baseURI) {
            this.inputStream = inputStream;
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI = baseURI;
        }

        @Override
        public java.io.Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(java.io.Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return inputStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
        }

        @Override
        public String getBaseURI() {
            return baseURI;
        }

        @Override
        public void setBaseURI(String baseURI) {
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }
}
