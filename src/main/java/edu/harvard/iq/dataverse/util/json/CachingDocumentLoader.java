package edu.harvard.iq.dataverse.util.json;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.HttpLoader;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A DocumentLoader that caches common JSON-LD contexts from local resources.
 */
public class CachingDocumentLoader implements DocumentLoader {

    private static final Logger logger = Logger.getLogger(CachingDocumentLoader.class.getCanonicalName());
    
    private final DocumentLoader defaultLoader;
    private final Map<URI, String> localContexts = new HashMap<>();

    public CachingDocumentLoader(DocumentLoader defaultLoader) {
        this.defaultLoader = defaultLoader != null ? defaultLoader : HttpLoader.defaultInstance();
        initLocalContexts();
    }

    private void initLocalContexts() {
        localContexts.put(URI.create("https://www.w3.org/ns/activitystreams"), "activitystreams.jsonld");
        localContexts.put(URI.create("https://coar-notify.net"), "coar-notify.jsonld");
        localContexts.put(URI.create("https://schema.org"), "schemaorg.jsonld");
        localContexts.put(URI.create("https://schema.org/"), "schemaorg.jsonld");
        localContexts.put(URI.create("https://schema.org/docs/jsonldcontext.jsonld"), "schemaorg.jsonld");
    }

    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        if (localContexts.containsKey(url)) {
            String resourceName = localContexts.get(url);
            try (InputStream is = getClass().getResourceAsStream("context/" + resourceName)) {
                if (is != null) {
                    logger.fine("Loading local context for " + url);
                    return JsonDocument.of(is);
                }
            } catch (Exception e) {
                logger.warning("Failed to load local context " + resourceName + ": " + e.getMessage());
            }
        }
        
        try {
            return defaultLoader.loadDocument(url, options);
        } catch (JsonLdError e) {
            // If it's a connection error and we are in a test environment, we might want to fail gracefully
            // but for now, we just rethrow.
            logger.warning("Failed to load remote document " + url + ": " + e.getMessage());
            throw e;
        }
    }
}
