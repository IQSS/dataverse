package edu.harvard.iq.dataverse.importer.metadata;

import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Eager
@ApplicationScoped
public class ImporterRegistry {
    private static final Logger logger = Logger.getLogger(ImporterRegistry.class.getSimpleName());

    private AtomicInteger counter = new AtomicInteger();

    private Map<String, MetadataImporter> registry = new HashMap<>();
    private Map<MetadataImporter, String> invertedRegistry = new HashMap<>();

    // -------------------- GETTERS --------------------

    public Map<String, MetadataImporter> getImporters() {
        return Collections.unmodifiableMap(registry);
    }

    // -------------------- LOGIC --------------------

    public void register(MetadataImporter importer) {
        if (importer == null) {
            logger.warning("Tried to register a null importer. Importer was not registered.");
            return;
        }
        String id = String.valueOf(counter.getAndIncrement());
        synchronized (this) {
            registry.put(id, importer);
            invertedRegistry.put(importer, id);
        }
        logger.info("Registered importer: " + importer.getClass().getSimpleName()
                + "with id: " + id);
    }

    public String getIdForImporter(MetadataImporter importer) {
        return invertedRegistry.get(importer);
    }

    public MetadataImporter getImporterForId(String id) {
        return registry.get(id);
    }
}
